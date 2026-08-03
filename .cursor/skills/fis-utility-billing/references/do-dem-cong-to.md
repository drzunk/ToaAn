# do-dem-cong-to

## Loại công tơ

| Loại | Đặc điểm | Đọc |
|---|---|---|
| Cơ khí 1 pha | Quay đĩa, không ghi peak | Manual ghi định kỳ |
| Điện tử 1 pha | LCD, ghi peak | Manual hoặc AMR |
| Điện tử 3 pha | Đo P, Q, S, cosφ | AMR ưu tiên |
| AMR | Remote RF/GPRS/PLC | Tự động qua MDM |

## Meter Data Management (MDM)

- Tích hợp đọc từ AMR / handheld / manual.
- Validate: chỉ số giảm so với chu kỳ trước → flag suspect (công tơ hỏng / đảo công tơ / ăn cắp điện).
- Estimate khi mất chỉ số: average của 3 chu kỳ gần nhất hoặc cùng kỳ năm trước.

## Sự kiện

- Đảo công tơ (meter swap) — chỉ số mới = 0 + chỉ số cũ.
- Hỏng công tơ — pro-rate theo trung bình.
- Cắt - mở do nợ — pause billing cycle.

## Anti-patterns

- Estimate tự động không có manual review → nhầm lẫn lớn.
- Không track lifecycle công tơ (lắp / kiểm định / thay thế) → audit khó.
