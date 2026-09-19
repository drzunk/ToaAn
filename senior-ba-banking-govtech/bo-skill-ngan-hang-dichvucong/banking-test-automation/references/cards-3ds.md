# Thẻ, ATM/POS, 3DS và tranh chấp

Mục lục: [Ba giai đoạn](#ba-giai-đoạn-của-một-giao-dịch-thẻ) · [Vòng đời thẻ](#vòng-đời-thẻ) · [ATM và POS](#atm-và-pos) · [Thẻ test](#thẻ-test-trong-sandbox) · [3DS](#3ds--sca) · [Tranh chấp](#tranh-chấp-và-chargeback)

## Ba giai đoạn của một giao dịch thẻ

Hiểu sai chỗ này là nguồn gốc của rất nhiều test case sai kỳ vọng:

1. **Authorization (cấp phép)** — kiểm tra thẻ hợp lệ và đủ tiền, **giữ** một khoản (hold).
   Tiền chưa thực sự rời tài khoản. Số dư sổ sách chưa đổi, số dư khả dụng giảm.
2. **Capture / Clearing (quyết toán)** — đơn vị chấp nhận thẻ gửi yêu cầu thu tiền thật,
   thường vào cuối ngày. Lúc này mới có bút toán ghi nợ.
3. **Settlement (thanh toán bù trừ)** — tiền chuyển giữa các ngân hàng qua tổ chức thẻ.

**Case sinh ra từ khoảng cách giữa 3 giai đoạn:**
- Hold rồi không capture → hold phải tự hết hạn và nhả tiền sau số ngày quy định.
- Capture số tiền **nhỏ hơn** authorization (khách sạn giữ 5 triệu, trả phòng thu 3 triệu) →
  phải nhả phần chênh.
- Capture số tiền **lớn hơn** authorization → phải bị từ chối hoặc chỉ cho phép trong biên độ
  quy định (ví dụ tiền tip).
- Capture nhiều lần trên một authorization (giao hàng từng phần).
- Khách xem số dư giữa auth và capture → phải thấy số dư khả dụng đã giảm, sổ sách chưa đổi.

## Vòng đời thẻ

```
ĐĂNG KÝ → PHÁT HÀNH → KÍCH HOẠT → HOẠT ĐỘNG ⇄ TẠM KHÓA
                                      ↓
                              KHÓA VĨNH VIỄN / HẾT HẠN / BÁO MẤT
```

Case cho từng chuyển đổi:
- Giao dịch bằng thẻ **chưa kích hoạt** → từ chối, mã lỗi riêng.
- Giao dịch bằng thẻ **tạm khóa** → từ chối; mở khóa xong giao dịch lại phải thành công.
- Thẻ **báo mất** → từ chối vĩnh viễn, kể cả khi thẻ vật lý còn dùng được ngoại tuyến.
- Thẻ **hết hạn**: test vào đúng ngày cuối tháng hết hạn (thẻ thường còn hiệu lực đến hết
  ngày cuối của tháng ghi trên thẻ) — đây là lỗi lệch một ngày rất hay gặp.
- Thẻ **gia hạn**: giao dịch định kỳ đã lưu thẻ cũ có tự chuyển sang thẻ mới không.
- Hạn mức tín dụng: sát hạn mức, đúng hạn mức, vượt hạn mức, và sau khi thanh toán dư nợ thì
  hạn mức khả dụng phải khôi phục.

## ATM và POS

**ATM — các case đặc thù:**
- Rút tiền thành công nhưng máy kẹt tiền → phải có cơ chế đối soát và hoàn tiền.
- Khách không lấy tiền sau khi máy nhả → tiền thu hồi, giao dịch bị hủy, hoàn tiền.
- Rút số tiền không chia hết cho mệnh giá nhỏ nhất → từ chối rõ ràng.
- Mất điện/mất mạng giữa giao dịch → reversal tự động (xem `iso-messages.md`).
- Rút ở ATM ngân hàng khác → phí khác, hạn mức khác.
- Sai PIN nhiều lần → khóa thẻ đúng số lần quy định, và phải khóa ở tầng hệ thống chứ không
  chỉ ở màn hình ATM.
- Truy vấn số dư, đổi PIN, chuyển khoản tại ATM — mỗi nghiệp vụ một processing code riêng.

**POS:**
- Quẹt, chạm (contactless), nhập tay (key-in) — ba phương thức có mức rủi ro và yêu cầu xác
  thực khác nhau.
- Contactless dưới ngưỡng không cần PIN, trên ngưỡng phải nhập PIN.
- Giao dịch ngoại tuyến (offline) khi POS mất mạng, đẩy lên sau.
- Hủy giao dịch trong ngày (void) khác với hoàn tiền sau đó (refund) — hai luồng khác nhau,
  bút toán khác nhau.

## Thẻ test trong sandbox

Với PSP, dùng đúng dải thẻ test công bố. Ở môi trường thử nghiệm của Stripe, số
`4242424242424242` cho kết quả thành công, còn các số khác cho từng tình huống từ chối cụ
thể như bị từ chối chung, không đủ số dư, hay thẻ hết hạn — mỗi số ứng với một mã lý do xác
định, nên dùng chúng để dựng case thay vì cố tạo lỗi bằng cách khác.

Nguyên tắc: **mỗi outcome cần test phải gắn với một số thẻ cho ra outcome đó một cách tất
định.** Đừng dựa vào việc "thử nhiều lần sẽ có lần fail".

Với kết nối trực tiếp tới tổ chức thẻ hoặc NAPAS, đội tích hợp sẽ được cấp bộ thẻ test riêng
— hỏi đội đó, đừng tự sinh số thẻ để bắn vào sandbox của đối tác.

## 3DS / SCA

Xác thực chủ thẻ khi thanh toán trực tuyến. Với thanh toán có yếu tố châu Âu thì gần như
luôn có bước thử thách; với thị trường Việt Nam tùy cấu hình của tổ chức phát hành.

**Điểm kỹ thuật quan trọng khi automation:** màn hình thử thách 3DS nằm trong iframe lồng
nhau, thường là cross-origin. Locator thông thường sẽ không bao giờ chạm tới được — phải đi
qua từng lớp frame:

```javascript
// Playwright: đi vào iframe lồng nhau thay vì locator phẳng
const outer = page.frameLocator('iframe[name*="__privateStripeFrame"]');
const challenge = outer.frameLocator('iframe[name="stripe-challenge-frame"]');
await challenge.getByRole('button', { name: /complete|hoàn tất/i }).click();
```

Tên iframe thay đổi giữa các phiên bản SDK, nên dùng khớp theo mẫu thay vì tên cứng.

**Case 3DS:**
- Thử thách thành công → giao dịch hoàn tất.
- Thử thách thất bại → giao dịch bị từ chối, **không** trừ tiền.
- Khách bỏ dở giữa chừng (đóng tab) → giao dịch không được treo ở trạng thái mập mờ.
- Thử thách hết thời gian chờ.
- Luồng không cần thử thách (frictionless) → vẫn phải có dữ liệu xác thực trong bản ghi.
- Giao dịch miễn thử thách do dưới ngưỡng → kiểm tra đúng ngưỡng cấu hình.

## Tranh chấp và chargeback

Quy trình nhiều bên, kéo dài nhiều ngày, và hầu như không đội QA nào test đủ.

```
Khách khiếu nại → Tra soát → Chargeback (đòi lại tiền từ đơn vị bán)
                                  ├─→ Đơn vị bán chấp nhận → hoàn tiền khách
                                  └─→ Đơn vị bán phản bác (representment)
                                          └─→ Phân xử → kết luận cuối
```

**Case cần có:**
- Tạo khiếu nại cho giao dịch chưa quyết toán và đã quyết toán — hai luồng khác nhau.
- Ghi có tạm thời cho khách trong thời gian tra soát (nếu chính sách có), và thu hồi lại nếu
  khiếu nại không thành.
- Thời hạn từng bước: hết hạn phản hồi thì tự động chuyển trạng thái.
- Khiếu nại cho giao dịch đã được hoàn tiền trước đó → phải chặn, tránh hoàn hai lần.
- Phí xử lý tranh chấp hạch toán vào đâu.
- Đối soát: giao dịch đang tranh chấp phải được đánh dấu trong báo cáo cuối ngày.

**Nguyên tắc test**: mọi nhánh của tranh chấp đều kết thúc bằng một phép cân tiền. Tổng tiền
trong hệ thống trước và sau toàn bộ quy trình phải giải thích được bằng các bút toán đã sinh
ra, không thiếu không thừa một đồng.
