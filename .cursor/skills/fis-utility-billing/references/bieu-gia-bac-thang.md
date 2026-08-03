# bieu-gia-bac-thang

## Biểu giá điện sinh hoạt — Bậc thang

6 bậc per QĐ 24/2017/QĐ-TTg, cập nhật giá theo QĐ EVN.

| Bậc | Mức kWh | Diễn giải |
|---|---|---|
| 1 | 0-50 | Hộ nghèo / hộ chính sách |
| 2 | 51-100 | Hộ nhỏ |
| 3 | 101-200 | Hộ vừa |
| 4 | 201-300 | Hộ tiêu thụ trung bình |
| 5 | 301-400 | Hộ tiêu thụ cao |
| 6 | > 400 | Hộ tiêu thụ rất cao |

## Biểu giá sản xuất / kinh doanh / hành chính sự nghiệp

3 khung giờ:
- Cao điểm (peak) — sáng + chiều
- Bình thường (normal)
- Thấp điểm (off-peak) — đêm

Hệ số per nhóm khách hàng khác nhau.

## VAT + thuế môi trường

- VAT (V1) — 10% (hoặc 8% theo Nghị quyết miễn giảm tạm thời).
- Thuế bảo vệ môi trường — không áp dụng cho điện sinh hoạt.

## Anti-patterns

- Tính bậc thang trên kWh dồn 2 chu kỳ → khách hàng mất quyền lợi bậc thấp.
- Hardcode giá per kWh → mỗi lần điều chỉnh giá phải hot-fix code.
- Quên multiply hệ số per giờ trong biểu giá kinh doanh.
