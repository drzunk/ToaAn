# Kiểm thử message ISO 8583 và ISO 20022 / SWIFT

Mục lục: [ISO 8583](#iso-8583--message-thẻ) · [ISO 20022](#iso-20022) · [SWIFT MT sang MX](#chuyển-đổi-swift-mt-sang-mx) · [Cách test message](#cách-test-ở-tầng-message) · [Anti-pattern](#anti-pattern)

## ISO 8583 — message thẻ

Chuẩn message dùng cho giao dịch thẻ (ATM, POS) và là nền tảng của kết nối NAPAS.

### Cấu trúc

```
[MTI 4 ký tự][Bitmap 64 hoặc 128 bit][Các trường dữ liệu theo thứ tự bit bật]
```

**MTI (Message Type Indicator)** — 4 chữ số, đọc theo từng vị trí:

| MTI | Ý nghĩa |
|---|---|
| 0100 / 0110 | Yêu cầu cấp phép / phản hồi cấp phép (authorization) |
| 0200 / 0210 | Yêu cầu tài chính / phản hồi (financial, ghi nợ thật) |
| 0400 / 0410 | Yêu cầu hủy / phản hồi hủy (reversal) |
| 0420 / 0430 | Hủy do lỗi (reversal advice) |
| 0800 / 0810 | Message quản trị (echo test, đổi khóa) |

**Các trường hay phải assert:**

| Field | Nội dung | Điểm test |
|---|---|---|
| DE2 | PAN | Phải bị che trong mọi log; độ dài 13–19 |
| DE3 | Processing code | Phân biệt rút tiền/truy vấn/chuyển khoản |
| DE4 | Số tiền giao dịch | Không dấu chấm, có số lẻ theo đơn vị tiền tệ |
| DE7 | Ngày giờ truyền | MMDDhhmmss, kiểm tra biên năm mới |
| DE11 | STAN (số theo dõi) | Duy nhất trong ngày, lộ bug khi quay vòng 999999 |
| DE12/13 | Ngày giờ giao dịch tại địa phương | Múi giờ |
| DE37 | Retrieval reference number | Dùng để đối soát, phải khớp giữa request/response/reversal |
| DE39 | Mã phản hồi | `00` thành công; các mã khác phải map đúng sang thông báo người dùng |
| DE41/42 | Mã thiết bị đầu cuối / đơn vị chấp nhận thẻ | |
| DE54 | Số dư khả dụng | Sau giao dịch |

### Case trọng tâm

- **Reversal**: gửi 0200, nhận timeout, gửi 0400 với cùng STAN/RRN → phải hoàn đúng số tiền,
  đúng một lần. Gửi 0400 hai lần → lần hai không được hoàn thêm.
- **Partial approval**: khách rút 2.000.000 nhưng chỉ còn 1.500.000 → tùy cấu hình, hệ thống
  hoặc từ chối hoàn toàn hoặc duyệt một phần. Phải khớp với đặc tả.
- **Mã phản hồi**: test đủ các mã hay gặp — `51` không đủ số dư, `54` thẻ hết hạn, `55` sai
  PIN, `61` vượt hạn mức, `91` bên phát hành không phản hồi. Mỗi mã phải cho ra một thông báo
  người dùng khác nhau và một hành vi hoàn tiền khác nhau.
- **STAN quay vòng**: đặt STAN gần 999999 rồi chạy tiếp, xem hệ thống có quay về 000001 đúng
  và không nhầm với giao dịch cũ.
- **Timeout hai chiều**: bên phát hành trả lời chậm hơn ngưỡng của bên thu nhận → bên thu
  nhận coi là thất bại, bên phát hành đã ghi nợ. Đây chính là tình huống sinh ra reversal.

## ISO 20022

Chuẩn message XML dùng cho thanh toán hiện đại, đang thay thế dần SWIFT MT và là nền của
nhiều hệ thống thanh toán mới.

### Các message hay gặp

| Message | Vai trò |
|---|---|
| `pain.001` | Khách hàng gửi lệnh chuyển tiền tới ngân hàng |
| `pain.002` | Ngân hàng báo trạng thái lệnh về cho khách hàng |
| `pacs.008` | Chuyển tiền khách hàng giữa các ngân hàng |
| `pacs.009` | Chuyển tiền giữa các định chế tài chính |
| `pacs.002` | Báo trạng thái message thanh toán (chấp nhận/từ chối) |
| `pacs.004` | Hoàn trả (return) một khoản đã chuyển |
| `camt.053` | Sao kê cuối ngày |
| `camt.054` | Báo có/báo nợ |
| `camt.056` | Yêu cầu hủy lệnh chuyển tiền |

### Case trọng tâm

- **Validate theo XSD** trước khi kiểm nghiệp vụ. Message sai schema phải bị từ chối ở cổng
  vào với mã lỗi rõ ràng, không được đi sâu vào hệ thống rồi mới lỗi.
- **Định danh duy nhất**: `MsgId`, `EndToEndId`, `TxId`, `UETR`. Kiểm tra `EndToEndId` được
  giữ nguyên xuyên suốt chuỗi message — đây là thứ khách hàng dùng để tra soát. Rất nhiều bug
  là mất hoặc cắt ngắn trường này khi đi qua một hệ thống trung gian.
- **Ký tự tiếng Việt**: bộ ký tự cho phép trong nhiều trường là tập hạn chế; tên có dấu phải
  được chuyển tự đúng, không biến thành dấu hỏi.
- **Độ dài trường**: tên người thụ hưởng thường giới hạn 140 ký tự, nhưng một số hệ thống
  trung gian cắt còn 35. Test tên dài để xem cắt ở đâu và có mất thông tin quan trọng không.
- **Số tiền và tiền tệ**: số chữ số thập phân phải đúng theo mã tiền tệ (VND không có phần
  thập phân — gửi `1000.00` cho VND là sai).
- **Chuỗi hoàn trả**: `pacs.008` bị từ chối → `pacs.002` với mã lý do; hoàn tiền → `pacs.004`
  phải tham chiếu đúng giao dịch gốc.
- **Message trùng**: gửi lại cùng `MsgId` phải bị phát hiện là trùng, không xử lý hai lần.

Assert bằng XPath trên từng trường, không so sánh cả file XML dưới dạng chuỗi:

```python
ns = {"d": "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08"}
root = etree.fromstring(message)
assert root.findtext(".//d:IntrBkSttlmAmt", namespaces=ns) == "1000"
assert root.find(".//d:IntrBkSttlmAmt", namespaces=ns).get("Ccy") == "VND"
assert root.findtext(".//d:EndToEndId", namespaces=ns) == expected_e2e
```

So sánh toàn bộ XML sẽ đỏ mỗi khi có thêm trường tùy chọn hoặc đổi thứ tự — vô dụng.

## Chuyển đổi SWIFT MT sang MX

Nếu dự án đang trong giai đoạn chuyển đổi, đây là vùng nhiều bug nhất.

Ánh xạ hay dùng: `MT103` → `pacs.008`, `MT202` → `pacs.009`, `MT940/950` → `camt.053`,
`MT192/292` → `camt.056`.

**Case bắt buộc khi migration:**
- **Round-trip**: MT → MX → MT phải cho lại nội dung nghiệp vụ tương đương. Chỗ nào mất
  thông tin phải được ghi nhận là có chủ đích, không phải tình cờ.
- **Truncation**: MT có trường độ dài cố định, MX dài hơn. Chuyển MX → MT dễ mất dữ liệu; phải
  có quy tắc rõ ràng và test cho trường hợp vượt độ dài.
- **Trường có cấu trúc so với dòng tự do**: MT dùng nhiều dòng tự do (field 70, 72), MX có
  cấu trúc. Việc bóc tách phải test với dữ liệu thực tế lộn xộn, không chỉ dữ liệu mẫu đẹp.
- **Chạy song song**: trong giai đoạn cả hai định dạng cùng chạy, một giao dịch không được
  xử lý hai lần qua hai luồng.
- **Phí OUR/SHA/BEN**: cách tính và hiển thị phí phải giống nhau giữa hai định dạng.

## Cách test ở tầng message

1. **Có bộ message mẫu phiên bản hóa** trong repo test (đã làm sạch dữ liệu), mỗi file là một
   kịch bản có tên rõ ràng: `pacs008_vnd_khong_phi.xml`, `pacs008_ten_dai_140ky_tu.xml`.
2. **Dựng bộ sinh message** thay vì sửa tay từng file — sửa tay dẫn tới file lệch nhau và
   không ai dám đụng vào.
3. **Kiểm 3 tầng**: đúng schema → đúng nghiệp vụ → đúng tác động lên sổ sách. Một message
   hợp lệ về hình thức vẫn có thể tạo bút toán sai.
4. **Ghi lại message thật từ môi trường sandbox** của đối tác làm bộ test hồi quy, sau khi đã
   thay hết định danh.

## Anti-pattern

- **So sánh toàn văn message.** Đỏ vì lý do vô hại, mọi người bắt đầu bỏ qua kết quả test.
- **Chỉ test message hợp lệ.** Phần lớn sự cố sản xuất đến từ message méo mó của đối tác.
- **Ghi PAN đầy đủ vào log test để tiện debug.** Vi phạm PCI DSS ngay cả ở môi trường test
  nếu log được lưu trữ. Luôn che còn 6 số đầu 4 số cuối, hoặc che hết.
- **Giả định đối tác luôn trả đúng chuẩn.** Test với trường thiếu, thừa, sai thứ tự, sai
  encoding.
