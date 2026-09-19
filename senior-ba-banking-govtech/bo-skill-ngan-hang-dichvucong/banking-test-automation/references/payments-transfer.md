# Chuyển tiền và thanh toán

Mục lục: [Các kênh](#các-kênh-chuyển-tiền-ở-việt-nam) · [Idempotency](#idempotency) · [Timeout](#giao-dịch-treo-và-tra-soát) · [VietQR](#vietqr) · [Bộ case chuẩn](#bộ-case-chuẩn-cho-một-lệnh-chuyển-tiền) · [Thanh toán hóa đơn](#thanh-toán-hóa-đơn-và-nạp-tiền)

## Các kênh chuyển tiền ở Việt Nam

| Kênh | Đặc điểm | Điểm test riêng |
|---|---|---|
| Nội bộ cùng ngân hàng | Ghi nợ/ghi có trong cùng core, tức thời | Nhanh nhất nhưng vẫn phải test bút toán và hạn mức |
| NAPAS 247 (nhanh 24/7) | Liên ngân hàng tức thời qua NAPAS, có hạn mức theo quy định | Timeout, mã lỗi NAPAS, hạn mức/giao dịch, hoàn tiền tự động |
| CITAD (điện liên ngân hàng NHNN) | Theo lô, theo giờ làm việc, hạn mức lớn | Cut-off, ngày làm việc, trạng thái chờ xử lý qua đêm |
| Chuyển tiền quốc tế (SWIFT) | T+1 trở lên, qua ngân hàng trung gian, phí OUR/SHA/BEN | Xem `iso-messages.md` |
| Ví điện tử / trung gian thanh toán | Liên kết tài khoản, hạn mức riêng | Luồng liên kết, hủy liên kết, hạn mức ví |

Điều quan trọng khi thiết kế case: **mỗi kênh có tập mã lỗi riêng và hành vi hoàn tiền riêng.**
Đừng viết một bộ case dùng chung cho mọi kênh rồi giả định kết quả giống nhau.

## Idempotency

Đây là phần dễ bỏ sót nhất và gây thiệt hại tiền lớn nhất.

**Cơ chế đúng**: client sinh một khóa duy nhất (idempotency key / transaction reference) cho
mỗi ý định chuyển tiền, gửi kèm request. Server lưu khóa này cùng kết quả. Request lặp lại
với cùng khóa trả về **kết quả đã lưu**, không thực hiện lại.

**Các case bắt buộc:**

```
1. Gửi cùng một khóa 2 lần liên tiếp
   → lần 2 trả về cùng transaction_id, cùng trạng thái
   → chỉ có 1 bộ bút toán trong DB
   → số dư chỉ giảm 1 lần

2. Gửi cùng khóa 2 lần ĐỒNG THỜI (song song)
   → đúng 1 lần thành công, lần kia trả kết quả giống hệt hoặc mã "đang xử lý"
   → tuyệt đối không được có 2 bút toán
   (Đây là case lộ ra lỗi thiếu khóa ở DB — in-memory lock không cứu được khi chạy nhiều pod)

3. Gửi cùng khóa nhưng ĐỔI SỐ TIỀN
   → phải trả lỗi xung đột, không được im lặng dùng dữ liệu cũ hay ghi đè

4. Gửi lại khóa cũ sau khi giao dịch đã hoàn tất từ lâu (ngoài thời gian lưu khóa)
   → phải rõ ràng: hoặc vẫn trả kết quả cũ, hoặc từ chối. Không được tạo giao dịch mới.
```

Test song song bằng cách bắn request đồng thời thật, không phải hai lời gọi tuần tự:

```python
import concurrent.futures

def test_idempotency_concurrent(api, account):
    key = str(uuid.uuid4())
    payload = {"from": account.no, "to": DEST, "amount": 100_000, "idem_key": key}

    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        results = [f.result() for f in
                   [pool.submit(api.transfer, payload) for _ in range(5)]]

    txn_ids = {r["transaction_id"] for r in results if r.get("transaction_id")}
    assert len(txn_ids) == 1, f"Tao ra nhieu giao dich: {txn_ids}"
    assert count_journal_entries(key) == 1
    assert api.balance(account.no) == account.balance - 100_000 - FEE
```

**Lưu ý về idempotency in-memory**: một `Set` trong bộ nhớ ứng dụng không phải idempotency.
Khi hệ thống chạy nhiều instance hoặc restart, nó mất tác dụng ngay. Khóa phải nằm ở lớp lưu
trữ bền vững, tốt nhất là ràng buộc unique ở DB.

## Giao dịch treo và tra soát

Tình huống nguy hiểm nhất: đã ghi nợ tài khoản khách, gửi lệnh sang đối tác, rồi mất phản hồi.

**Các nhánh phải test:**

| Tình huống | Hành vi đúng |
|---|---|
| Timeout khi gửi sang đối tác | Giao dịch ở trạng thái chờ/tra soát, tiền vẫn bị giữ, **không** báo thất bại cho khách ngay |
| Đối tác trả kết quả thành công muộn | Hoàn tất giao dịch, không sinh thêm bút toán |
| Đối tác trả kết quả thất bại muộn | Hoàn tiền về tài khoản nguồn, hoàn cả phí, thông báo cho khách |
| Job tra soát chạy và không nhận được phản hồi | Sau ngưỡng quy định, tự động hoàn tiền và ghi nhận để đối soát thủ công |
| Đã hoàn tiền rồi đối tác mới báo thành công | **Không** ghi nợ lại tự động; phải đẩy vào hàng chờ xử lý thủ công và cảnh báo |
| Ngân hàng nhận nhận được lệnh 2 lần | Chỉ ghi có 1 lần (idempotency phía nhận) |

Cách tạo timeout trong test: dùng công cụ tiêm lỗi mạng (Toxiproxy) hoặc mock đối tác có chế
độ trễ/không phản hồi — xem `nonfunctional.md`. Đừng mô phỏng bằng cách tắt service, vì hành
vi "connection refused" khác hẳn "connection timeout" ở tầng xử lý lỗi.

## VietQR

Mã QR thanh toán theo chuẩn EMVCo, chứa thông tin ngân hàng thụ hưởng, số tài khoản, và tùy
chọn số tiền + nội dung.

**Case cần có:**
- QR tĩnh (không có số tiền) → người chuyển tự nhập số tiền.
- QR động (có sẵn số tiền) → số tiền phải khóa, không cho sửa; nếu app cho sửa là bug.
- QR có nội dung chuyển khoản cố định → nội dung phải giữ nguyên, kể cả ký tự đặc biệt và
  độ dài tối đa.
- QR sai checksum (CRC) → phải bị từ chối, không cố đoán.
- QR của ngân hàng không thuộc mạng lưới → báo lỗi rõ ràng.
- QR hết hạn (với QR động có thời hạn) → từ chối.
- Quét lại cùng một QR động đã thanh toán → tùy nghiệp vụ: chặn hoặc cho phép; phải khớp với
  đặc tả, và phải nhất quán giữa các ngân hàng phát sinh lệnh.
- Ảnh QR mờ/nghiêng/thiếu góc → hành vi của bộ quét, thuộc phần mobile.

Kiểm tra nội dung QR bằng cách giải mã chuỗi EMVCo và assert từng tag, thay vì chỉ chụp màn
hình. CRC là 4 ký tự hex cuối, tính trên toàn bộ chuỗi phía trước.

## Bộ case chuẩn cho một lệnh chuyển tiền

Dùng làm khung, điều chỉnh theo sản phẩm:

**Nhóm hợp lệ**
- Chuyển nội bộ, chuyển liên ngân hàng, mỗi loại với và không có phí
- Số tiền nhỏ nhất cho phép và lớn nhất cho phép
- Nội dung chuyển khoản có dấu tiếng Việt, ký tự đặc biệt, độ dài tối đa
- Chuyển tới tài khoản đã lưu trong danh bạ và tài khoản nhập mới
- Chuyển theo lịch/định kỳ (nếu có): đúng ngày, và ngày rơi vào cuối tuần/lễ

**Nhóm biên**
- Số tiền = số dư khả dụng (phải thất bại nếu còn phí)
- Số tiền = số dư khả dụng − phí (phải thành công, số dư về 0 hoặc về mức tối thiểu)
- Số tiền vượt hạn mức 1 đồng; đúng bằng hạn mức
- Tổng dồn trong ngày chạm ngưỡng xác thực mạnh
- Số tiền 0 và số âm (phải bị từ chối ở cả client và server — test riêng ở API)

**Nhóm trạng thái bất thường**
- Tài khoản nguồn: đóng, phong tỏa, ngủ đông, quá hạn xác thực lại
- Tài khoản đích: không tồn tại, sai tên thụ hưởng, đóng, thuộc ngân hàng ngừng kết nối
- Chuyển cho chính mình
- Khác loại tiền tệ

**Nhóm xác thực**
- Sai OTP, OTP hết hạn, dùng lại OTP cũ, vượt số lần nhập sai
- Ngưỡng bắt buộc sinh trắc học (xem `auth-security.md`)
- Phiên đăng nhập hết hạn giữa chừng

**Nhóm hệ thống**
- Timeout, đối tác trả lỗi, gửi lặp, mất kết nối giữa bước xác nhận
- Chạy lại batch sau sự cố

**Nhóm hậu kiểm**
- Bút toán cân, sao kê đúng, thông báo đúng, có mặt trong file đối soát

## Thanh toán hóa đơn và nạp tiền

Khác chuyển tiền ở chỗ có bước **truy vấn** trước khi **thanh toán**, và nhà cung cấp dịch
vụ là bên thứ ba.

- Truy vấn hóa đơn không tồn tại, đã thanh toán, đã quá hạn
- Số tiền cố định (không cho sửa) và số tiền tự nhập (có giới hạn)
- Thanh toán thành công nhưng nhà cung cấp không ghi nhận → phải có cơ chế tra soát
- Thanh toán 2 lần cùng một hóa đơn → chặn hoặc cho phép theo đặc tả, không được mơ hồ
- Nhà cung cấp trả mã lỗi ngoài danh sách đã biết → hệ thống phải xử lý an toàn, không
  treo tiền khách
