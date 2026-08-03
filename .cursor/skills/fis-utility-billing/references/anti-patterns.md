# anti-patterns

Real bugs / incidents from FIS EVN project archive.

## 1. Hardcoded biểu giá

**Symptom:** Mỗi lần EVN điều chỉnh giá phải sửa code.
**Root:** Bảng giá lưu trong code constants thay vì DB.
**Prevention:** Bảng `bieu_gia` effectivity-dated; lookup theo `cycle_date BETWEEN effective_from AND effective_to`.

## 2. Bậc thang dồn 2 chu kỳ

**Symptom:** Khách bị tính bậc 6 khi sản lượng 2 tháng cộng lại > 400 kWh dù mỗi tháng < 200.
**Root:** Không reset bậc per chu kỳ.
**Prevention:** Tính bậc thang strict per chu kỳ; cycle isolation.

## 3. Estimate auto-approve

**Symptom:** Khách phản đối, audit phát hiện không đọc chỉ số 6 tháng liền, system tự estimate.
**Root:** Estimate algorithm không có manual review gate.
**Prevention:** Estimate flag → manual review queue; max 2 chu kỳ liên tiếp được estimate.

## 4. Hóa đơn không match e-invoice cổng thuế

**Symptom:** Audit thuế phát hiện chênh lệch tổng doanh thu giữa internal report và e-invoice gateway.
**Root:** Hóa đơn phát hành nhưng e-invoice gateway lỗi không gửi → desync.
**Prevention:** Two-phase commit pattern: chỉ commit invoice DB khi gateway ACK 200.
