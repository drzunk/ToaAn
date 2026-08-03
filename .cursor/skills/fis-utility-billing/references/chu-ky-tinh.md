# chu-ky-tinh

## Chu kỳ ghi - tính - thu

Chu kỳ tháng (monthly), một số khách hàng lớn chu kỳ ngày hoặc tuần.



## Cycle date

- Mỗi điểm đo có `cycle_day` (ví dụ ngày 5, 10, 15, 20, 25 hàng tháng).
- Đọc chỉ số trong khoảng ±2 ngày quanh `cycle_day`.

## Edge cases

- Chu kỳ đầu (first reading): pro-rate.
- Chu kỳ cuối (disconnect): chỉ số đến ngày cắt.
- Đổi công tơ giữa kỳ: 2 phần tính riêng.

## Anti-patterns

- Tính tiền trước khi đọc chỉ số xong → dùng estimate sai.
- Hóa đơn phát hành không đồng bộ với e-invoice cổng thuế.
