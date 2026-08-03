# hop-dong-pe

## Hợp đồng PE (Provider–Enterprise)

Hợp đồng cung cấp điện giữa EVN và khách hàng.

## Lifecycle

1. Đề nghị cung cấp điện (đơn từ khách hàng).
2. Khảo sát kỹ thuật (capacity, location, public/private grid).
3. Lập hợp đồng PE.
4. Lắp đặt công tơ + đường dây.
5. Đóng điện (energize).
6. Sửa đổi (capacity adjust, address change, owner change).
7. Chấm dứt (disconnect, demolish).

## Loại hợp đồng

- Sinh hoạt (residential)
- Kinh doanh (commercial)
- Sản xuất (manufacturing)
- Hành chính sự nghiệp (government / public service)
- Nông nghiệp

## Đặc trưng FIS

- 1 hợp đồng PE per điểm đo.
- 1 khách hàng có thể có N hợp đồng PE.
- Hợp đồng có effectivity-dated.

## Anti-patterns

- Một công tơ map nhiều hợp đồng → tính tiền sai.
- Không track address change history → audit fail.
