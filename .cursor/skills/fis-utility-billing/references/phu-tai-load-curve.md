# phu-tai-load-curve

## Load curve

Đường cong phụ tải theo thời gian (15-min, 30-min, 1-hour interval).

## Sử dụng

- Khách hàng lớn (TT 16/2014/TT-BCT) — bắt buộc đo phụ tải để tính giá theo khung giờ.
- Lưu trữ ≥ 13 tháng.
- Format: timestamp + active power (kW) + reactive power (kVAR).

## Tính tiền theo khung giờ



3 khung giờ:
- Cao điểm: 09h30-11h30, 17h00-20h00 (working day).
- Bình thường: 04h00-09h30, 11h30-17h00, 20h00-22h00.
- Thấp điểm: 22h00-04h00.

## Cosφ (power factor)

- Khách hàng có cosφ < 0.85 phải mua công suất phản kháng (Q charge).

## Anti-patterns

- Lưu load curve dạng wide table (288 cột cho 15-min) → query khó; nên long table.
- Quên reactive power → miss Q charge revenue.
