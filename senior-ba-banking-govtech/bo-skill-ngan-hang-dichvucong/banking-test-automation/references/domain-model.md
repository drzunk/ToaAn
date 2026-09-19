# Mô hình nghiệp vụ ngân hàng cho người test

Mục lục: [Các thực thể](#các-thực-thể-cốt-lõi) · [Bút toán kép](#bút-toán-kép) · [Số dư](#các-loại-số-dư) · [Vòng đời giao dịch](#vòng-đời-giao-dịch) · [Ngày giá trị](#ngày-hạch-toán-và-ngày-giá-trị) · [Phí lãi tỉ giá](#phí-lãi-và-tỉ-giá) · [Checklist](#checklist-kiểm-tra-nghiệp-vụ)

## Các thực thể cốt lõi

| Thực thể | Ý nghĩa | Điểm test hay sót |
|---|---|---|
| CIF (Customer Information File) | Hồ sơ khách hàng, 1 khách có thể nhiều tài khoản | Trùng CIF khi mở tài khoản thứ 2; sửa thông tin CIF có lan sang tài khoản không |
| Account | Tài khoản thanh toán / tiết kiệm / vay | Mỗi tài khoản có 1 loại tiền tệ; giao dịch khác currency phải bị chặn hoặc quy đổi tường minh |
| GL (General Ledger) | Sổ cái, tập hợp tài khoản kế toán nội bộ | Giao dịch khách hàng luôn có bút toán đối ứng vào GL (phí, treo, trung gian) |
| Transaction / Journal entry | Bản ghi giao dịch và bút toán sinh ra từ nó | 1 giao dịch nghiệp vụ = nhiều dòng bút toán |
| Limit | Hạn mức theo giao dịch / ngày / kênh / cấp duyệt | Hạn mức cộng dồn theo ngày, reset lúc nào |
| Product | Sản phẩm (loại tài khoản, loại thẻ, gói phí) | Cùng nghiệp vụ nhưng khác sản phẩm thì phí và hạn mức khác |

## Bút toán kép

Đây là nền tảng của toàn bộ việc kiểm tra. Mọi giao dịch phải thỏa mãn:

```
SUM(debit) == SUM(credit)   cho mỗi giao dịch, và cho mỗi ngày hạch toán
```

Ví dụ chuyển tiền 1.000.000đ, phí 11.000đ (đã gồm VAT), nội bộ cùng ngân hàng:

| Tài khoản | Nợ | Có |
|---|---|---|
| TK khách hàng A | 1.011.000 | |
| TK khách hàng B | | 1.000.000 |
| GL thu phí dịch vụ | | 10.000 |
| GL thuế VAT phải nộp | | 1.000 |

Test phải khẳng định cả 4 dòng, không chỉ số dư A và B. Rất nhiều bug nằm ở dòng phí và thuế:
phí thu sai sản phẩm, VAT tính trên số sai, hoặc phí thu nhưng không ghi GL (tiền "bốc hơi"
khỏi sổ).

**Cách viết assertion:**

```python
def assert_balanced(txn_id):
    rows = db.query("SELECT account, debit, credit FROM journal WHERE txn_id = %s", txn_id)
    assert rows, f"Giao dich {txn_id} khong sinh but toan nao"
    total_debit = sum(r.debit for r in rows)
    total_credit = sum(r.credit for r in rows)
    assert total_debit == total_credit, (
        f"Lech but toan {total_debit - total_credit} tai txn {txn_id}"
    )
```

Dùng số nguyên đơn vị nhỏ nhất (đồng với VND, cent với USD). **Không bao giờ dùng float cho
tiền** — `0.1 + 0.2 != 0.3` sẽ biến thành bug lệch sổ. Trong Java dùng `BigDecimal`, Python
dùng `Decimal` hoặc int, JS dùng `BigInt`/thư viện decimal.

## Các loại số dư

Nhầm lẫn giữa các loại số dư là lỗi kỳ vọng phổ biến nhất khi viết test:

- **Số dư sổ sách (ledger/book balance)**: tổng bút toán đã hạch toán.
- **Số dư khả dụng (available balance)**: sổ sách − số tiền đang bị giữ (hold) − số dư tối
  thiểu duy trì + hạn mức thấu chi.
- **Số tiền đang giữ (hold/authorization)**: ví dụ quẹt thẻ ở khách sạn, tiền bị giữ nhưng
  chưa hạch toán. Hold có thời hạn và có thể hết hạn tự nhả.
- **Số dư tối thiểu**: nhiều sản phẩm bắt buộc duy trì, không được rút hết.

Test case bắt buộc: tài khoản có 1.000.000 sổ sách nhưng đang giữ 300.000 → chuyển 800.000
phải **thất bại**. Nếu hệ thống cho qua, đó là lỗi cho phép âm quỹ.

## Vòng đời giao dịch

Vẽ và test theo máy trạng thái, không test theo màn hình. Vòng đời điển hình của một lệnh
chuyển tiền liên ngân hàng:

```
INITIATED → VALIDATED → AUTHORIZED → SENT_TO_PARTNER
                                          ├─→ CONFIRMED (đối tác báo thành công)
                                          ├─→ REJECTED  (đối tác từ chối, phải hoàn tiền)
                                          └─→ TIMEOUT   (chưa rõ) → TRACING → CONFIRMED | REVERSED
```

Với mỗi trạng thái, test 3 điều:
1. **Điều kiện vào**: cái gì hợp lệ mới được vào trạng thái này.
2. **Chuyển đổi không hợp lệ bị chặn**: ví dụ gọi API xác nhận cho giao dịch đã REVERSED
   phải trả lỗi, không được xử lý lại.
3. **Tác động lên tiền**: trạng thái nào trừ tiền, trạng thái nào hoàn tiền, trạng thái nào
   chỉ giữ.

Bug kinh điển: giao dịch TIMEOUT sau đó đối tác trả về CONFIRMED muộn, hệ thống đã REVERSED
rồi lại ghi có tiếp → khách nhận tiền hai lần. Luôn có case "phản hồi muộn sau khi đã hoàn".

## Ngày hạch toán và ngày giá trị

- **Booking date / ngày hạch toán**: ngày bút toán được ghi sổ.
- **Value date / ngày giá trị**: ngày bắt đầu tính lãi, có thể khác ngày hạch toán.
- **Cut-off time**: mốc giờ chia ngày làm việc. Giao dịch sau cut-off được tính sang ngày
  làm việc kế tiếp.

Case bắt buộc:
- Giao dịch lúc 23:59:59 và 00:00:01 — rơi đúng ngày nào.
- Giao dịch sau cut-off vào thứ Sáu → phải nhảy sang thứ Hai (hoặc ngày làm việc kế tiếp
  theo lịch nghỉ lễ đã cấu hình), không phải thứ Bảy.
- Ngày lễ tết Việt Nam: lịch nghỉ nhiều ngày liên tiếp làm lộ bug tính ngày làm việc.
- Múi giờ: hệ thống lưu UTC nhưng nghiệp vụ tính theo GMT+7. Giao dịch 07:30 sáng giờ VN là
  00:30 UTC của cùng ngày — nhiều hệ thống trả sai ngày ở đúng khoảng này.

Không chỉnh giờ máy chạy test. Dùng cơ chế giả lập thời gian phía server (business date
override, test clock) hoặc trigger batch thủ công. Xem `eod-reconciliation.md`.

## Phí, lãi và tỉ giá

**Phí**: xác định rõ thu trên tài khoản nào (người gửi hay người nhận), thu vào thời điểm
nào (khi khởi tạo hay khi thành công), và có hoàn lại khi giao dịch bị hủy không. Case quan
trọng: giao dịch thất bại nhưng phí đã thu — phải có bút toán hoàn phí.

**Làm tròn**: quy tắc làm tròn phải được hỏi rõ (làm tròn lên, xuống, hay half-up) và test ở
đúng chỗ phát sinh số lẻ. Với phí theo tỉ lệ phần trăm, test số tiền tạo ra phần thập phân:
0,03% của 333.333đ = 99,9999đ.

**Tỉ giá**: dùng tỉ giá nào (mua/bán/chuyển khoản), lấy tại thời điểm nào (khởi tạo hay xác
nhận), và điều gì xảy ra khi tỉ giá thay đổi giữa hai bước. Case: khách xem tỉ giá, chờ 5
phút rồi bấm xác nhận — hệ thống phải hoặc báo tỉ giá đã đổi, hoặc giữ giá đã khóa; im lặng
dùng giá mới là bug.

**Lãi**: test theo chu kỳ tính lãi, ngày nhập lãi, và trường hợp tất toán giữa kỳ. Số ngày
tính lãi theo quy ước nào (actual/365, 30/360) phải khớp với sản phẩm.

## Checklist kiểm tra nghiệp vụ

Dùng cho mọi case liên quan tiền:

- [ ] Số dư nguồn giảm đúng (gốc + phí + thuế)
- [ ] Số dư đích tăng đúng (gốc, hoặc gốc − phí nếu người nhận chịu phí)
- [ ] Tổng nợ = tổng có trong bút toán của giao dịch
- [ ] Bút toán phí và thuế tồn tại và vào đúng tài khoản GL
- [ ] Trạng thái giao dịch đúng với kết quả thực tế
- [ ] Sao kê/lịch sử giao dịch hiển thị đúng số tiền, đúng ngày, đúng mô tả
- [ ] Số dư khả dụng cập nhật ngay, không chỉ số dư sổ sách
- [ ] Thông báo (SMS/push/email) gửi đúng số tiền và đúng số dư còn lại
- [ ] Giao dịch xuất hiện trong báo cáo/file đối soát cuối ngày
- [ ] Audit log ghi đủ: ai, lúc nào, kênh nào, thiết bị/IP nào
