# Hiệu năng, chịu lỗi và giả lập đối tác

Mục lục: [Hiệu năng](#hiệu-năng) · [Chịu lỗi](#chịu-lỗi-và-chaos) · [Giả lập đối tác](#giả-lập-đối-tác) · [DR](#khôi-phục-thảm-họa) · [Mobile](#hiệu-năng-và-ổn-định-trên-mobile)

## Hiệu năng

### Xác định tải mục tiêu từ nghiệp vụ, không từ cảm tính

Hỏi đội vận hành ba con số:
- TPS trung bình và TPS đỉnh của 12 tháng gần nhất.
- Thời điểm đỉnh: ngày trả lương (mùng 5, 10, 15), cuối tháng, cận Tết, giờ vàng khuyến mãi.
- Mục tiêu độ trễ theo nghiệp vụ, thường khác nhau nhiều: truy vấn số dư khác chuyển tiền
  liên ngân hàng khác tra cứu sao kê 12 tháng.

Đặt ngân sách hiệu năng cụ thể rồi để CI cưỡng chế, ví dụ:

| Nghiệp vụ | p95 | p99 | Tỉ lệ lỗi |
|---|---|---|---|
| Đăng nhập | < 800 ms | < 1.5 s | < 0,1% |
| Truy vấn số dư | < 300 ms | < 600 ms | < 0,1% |
| Chuyển tiền nội bộ | < 1.5 s | < 3 s | < 0,05% |
| Chuyển tiền liên NH | < 5 s | < 10 s | < 0,5% |
| Sao kê 12 tháng | < 3 s | < 6 s | < 0,5% |

Ngân sách chỉ có ý nghĩa khi CI chặn merge lúc vượt. Ghi vào ngưỡng của công cụ tải:

```javascript
// k6
export const options = {
  scenarios: {
    gio_cao_diem: {
      executor: 'ramping-arrival-rate',
      startRate: 50, timeUnit: '1s',
      stages: [
        { target: 200, duration: '5m' },   // tang dan
        { target: 200, duration: '20m' },  // giu tai dinh
        { target: 500, duration: '2m' },   // dot bien (ngay tra luong)
        { target: 50,  duration: '5m' },   // giam
      ],
      preAllocatedVUs: 300,
    },
  },
  thresholds: {
    'http_req_duration{nghiep_vu:chuyen_tien}': ['p(95)<1500', 'p(99)<3000'],
    'http_req_failed': ['rate<0.0005'],
  },
};
```

### Các loại test tải cần phân biệt

- **Load**: tải kỳ vọng, chạy đủ lâu để lộ rò rỉ bộ nhớ và cạn kết nối DB.
- **Stress**: tăng tới khi hỏng, để biết điểm gãy ở đâu và hệ thống gãy thế nào (từ chối lịch
  sự hay sập hoàn toàn).
- **Spike**: tăng vọt trong vài phút, mô phỏng khuyến mãi hoặc sự kiện.
- **Soak**: tải vừa phải trong 8–24 giờ. Đây là loại duy nhất bắt được rò rỉ tài nguyên.
- **Volume**: dữ liệu lớn (tài khoản có 5 năm lịch sử giao dịch) chứ không phải nhiều người
  dùng. Truy vấn sao kê trên tài khoản 500.000 giao dịch là nơi timeout hay xảy ra.

### Điểm nghẽn đặc thù ngân hàng

- **Điểm nóng trên một bản ghi**: tài khoản trung gian/treo của ngân hàng bị ghi bởi mọi giao
  dịch → tranh chấp khóa ở DB. Test bằng cách bắn nhiều giao dịch cùng loại đồng thời.
- **Hàng chờ tới core**: core thường chỉ chịu được TPS thấp hơn kênh nhiều. Test xem hàng chờ
  có tràn không và hành vi khi tràn.
- **Kết nối tới đối tác**: số kết nối đồng thời tới NAPAS/tổ chức thẻ là hữu hạn.
- **Batch chồng lên giờ cao điểm**: chạy tải trong lúc batch đang chạy.

## Chịu lỗi và chaos

Ngân hàng không được phép mất tiền khi hạ tầng trục trặc. Các kịch bản cần chứng minh:

| Sự cố tiêm vào | Hành vi đúng |
|---|---|
| Đối tác trả lỗi 500 | Giao dịch thất bại sạch sẽ, tiền không bị trừ hoặc được hoàn ngay |
| Đối tác phản hồi chậm 30 giây | Timeout đúng ngưỡng, chuyển sang trạng thái tra soát |
| Mất kết nối giữa lúc gửi | Trạng thái treo, job tra soát xử lý sau |
| DB chính chuyển đổi dự phòng | Giao dịch đang dở hoặc hoàn tất hoặc hoàn lại, không nửa vời |
| Một pod ứng dụng chết giữa giao dịch | Giao dịch không mất, không nhân đôi |
| Hàng đợi message đầy | Từ chối lịch sự ở cổng vào thay vì mất message |
| Cache Redis chết | Hệ thống chậm nhưng vẫn đúng, không được trả số dư sai |
| Đồng hồ hai máy lệch nhau | Không sinh giao dịch có dấu thời gian trong tương lai |

Công cụ tiêm lỗi mạng (ví dụ Toxiproxy) đặt giữa ứng dụng và đối tác cho phép tạo trễ, mất
gói, ngắt kết nối một cách có kiểm soát — tốt hơn nhiều so với việc tắt service, vì hành vi
"timeout" và "connection refused" đi vào hai nhánh xử lý lỗi khác nhau.

**Nguyên tắc**: sau mỗi thí nghiệm chaos, chạy lại bộ kiểm tra cân sổ ở
`eod-reconciliation.md`. Hệ thống sống sót mà sổ sách lệch thì vẫn là thất bại.

## Giả lập đối tác

Hiếm khi có sandbox cho mọi đối tác, và sandbox thật thường không tạo được các lỗi hiếm. Ba
mức, chọn theo mục đích:

| Mức | Dùng khi | Công cụ |
|---|---|---|
| Mock trong tiến trình | Unit test, test logic xử lý lỗi | thư viện mock của ngôn ngữ |
| Máy chủ giả lập HTTP | Test tích hợp, cần mô phỏng mã lỗi và độ trễ | WireMock, MSW, Mountebank |
| Sandbox thật của đối tác | Test chấp nhận trước khi lên thật | do đối tác cấp |

Với giao thức không phải HTTP (ISO 8583 qua TCP), thường phải tự viết một trình giả lập. Đầu
tư ban đầu lớn nhưng đây là thứ cho phép test reversal, timeout và mã lỗi hiếm — những thứ
sandbox thật không tạo ra theo yêu cầu.

**Quy tắc giữ cho giả lập không nói dối:**
- Ghi lại message thật từ sandbox làm cơ sở cho phản hồi giả lập.
- Có một bộ test nhỏ chạy định kỳ trên sandbox thật để phát hiện khi đối tác đổi hành vi mà
  giả lập chưa cập nhật.
- Giả lập phải mô phỏng được cả **độ trễ**, không chỉ nội dung — một đối tác trả lời sau 8
  giây gây ra lỗi hoàn toàn khác với một đối tác trả lời tức thì.

## Khôi phục thảm họa

Ngân hàng có yêu cầu về RTO (thời gian khôi phục) và RPO (lượng dữ liệu chấp nhận mất). Vai
trò của QA trong diễn tập chuyển đổi dự phòng:

- Chuẩn bị bộ test khói (smoke) chạy ngay sau khi chuyển sang trung tâm dự phòng: đăng nhập,
  truy vấn số dư, một giao dịch nhỏ mỗi kênh, một phép cân sổ.
- Khẳng định **không mất giao dịch**: ghi lại danh sách giao dịch gửi ngay trước sự cố, đối
  chiếu sau khi khôi phục.
- Khẳng định **không nhân đôi**: giao dịch đang dở không được xử lý lại ở hệ thống dự phòng.
- Đo thời gian thực tế so với RTO cam kết.

## Hiệu năng và ổn định trên mobile

- Thời gian khởi động ứng dụng ở thiết bị cấu hình thấp (đây là phần lớn người dùng thật).
- Mạng 3G chập chờn: chuyển tiền khi mạng ngắt giữa chừng → không được gửi hai lần khi mạng
  trở lại.
- Chuyển mạng Wi-Fi sang 4G giữa giao dịch.
- Ứng dụng bị hệ điều hành thu hồi bộ nhớ khi chạy nền, quay lại phải xác thực lại chứ không
  khôi phục màn hình xác nhận giao dịch.
- Pin và nhiệt khi quét sinh trắc học liên tục.
- Chạy trên dàn thiết bị thật cho các luồng liên quan camera/sinh trắc học; giả lập không đủ.
