# Framework, locator, chống flaky và CI/CD

Mục lục: [Chọn tầng test](#chọn-tầng-test) · [Chọn công cụ](#chọn-công-cụ) · [Locator](#locator) · [Chống flaky](#chống-flaky) · [Cấu trúc dự án](#cấu-trúc-dự-án-test) · [CI/CD](#cicd) · [Môi trường](#môi-trường-test)

## Chọn tầng test

Ở ngân hàng, kim tự tháp test nghiêng mạnh về tầng API và tầng dữ liệu, vì đó là nơi tiền
thực sự di chuyển.

| Tầng | Tỉ trọng gợi ý | Kiểm cái gì |
|---|---|---|
| Unit | 40% | Tính phí, tính lãi, làm tròn, quy tắc ngày làm việc, validate |
| API / tích hợp | 40% | Luồng nghiệp vụ, phân quyền, idempotency, mã lỗi, bút toán |
| Dữ liệu / batch | 10% | Cân sổ, đối soát, báo cáo |
| E2E qua giao diện | 10% | Một vài luồng quan trọng nhất, đầu–cuối |

E2E qua UI đắt và dễ vỡ. Dùng nó để chứng minh các mảnh ghép nối được với nhau, không dùng
để phủ mọi biến thể nghiệp vụ — phủ biến thể ở tầng API.

**Quy tắc quyết định**: nếu một case có thể kiểm ở tầng thấp hơn mà vẫn giữ nguyên ý nghĩa,
hãy đưa nó xuống tầng thấp hơn.

## Chọn công cụ

| Nhu cầu | Công cụ phổ biến | Ghi chú cho môi trường ngân hàng |
|---|---|---|
| Web E2E | Playwright | Ưu tiên cho dự án mới: chờ tự động, xử lý iframe tốt (cần cho 3DS), bắt được lưu lượng mạng |
| Web E2E (hệ thống cũ) | Selenium | Nhiều core banking có giao diện cũ, chỉ chạy được trên trình duyệt cũ |
| API | RestAssured (Java), pytest + httpx (Python), Playwright APIRequest | Chọn theo ngôn ngữ đội đang dùng |
| Mobile | Appium (native), Maestro (nhanh hơn để viết) | Cần thiết bị thật cho sinh trắc học |
| Tải | k6, JMeter, Gatling | JMeter mạnh với giao thức phi HTTP như JDBC, JMS |
| Hợp đồng API | Pact, Schemathesis | Hữu ích khi nhiều đội cùng phát triển microservice |
| Giả lập đối tác | WireMock, MSW, Toxiproxy | Xem `nonfunctional.md` |
| Kiểm tra bí mật | gitleaks, trufflehog | Bắt buộc, chạy trên mọi PR |

Đừng đổi framework chỉ vì cái mới hợp mốt. Ràng buộc thật thường là: trình duyệt nào được
phép cài trên máy nội bộ, mạng có ra được internet để tải gói không, đội đã biết ngôn ngữ gì.

## Locator

Thứ tự ưu tiên khi chọn cách định vị phần tử:

1. `data-testid` (hoặc thuộc tính test riêng của dự án) — ổn định nhất, cần xin dev thêm vào.
2. Vai trò + tên có thể truy cập (`getByRole('button', { name: 'Xác nhận' })`).
3. Nhãn của ô nhập (`getByLabel('Số tài khoản người nhận')`).
4. Văn bản hiển thị — chấp nhận được nhưng vỡ khi đổi bản dịch.
5. CSS ngắn, ổn định.
6. XPath theo cấu trúc DOM — **tránh**; vỡ mỗi khi dev bọc thêm một thẻ `div`.

**Không bao giờ** dùng lớp CSS do công cụ sinh (`css-1x2y3z`) hay chỉ số vị trí
(`div:nth-child(4)`).

Với hệ thống ngân hàng cũ có giao diện sinh tự động và id thay đổi mỗi lần tải trang, chiến
lược thực tế là định vị theo neo ổn định gần đó: tìm nhãn cố định rồi lấy ô nhập kế bên.

```javascript
// Neo vao nhan thay vi vao id dong
const oSoTien = page.locator('tr', { hasText: 'Số tiền' }).locator('input');
```

## Chống flaky

Ở ngân hàng, nguyên nhân flaky xếp theo tần suất:

**1. Dữ liệu dùng chung** (nguyên nhân lớn nhất) — hai test tranh nhau một tài khoản. Cách
sửa: mỗi test tự tạo dữ liệu, xem `test-data.md`. Đây là việc phải sửa tận gốc, không có cách
vá nào hiệu quả.

**2. Chờ cứng** — `sleep(3)` vừa chậm vừa vẫn hỏng khi hệ thống tải cao. Thay bằng chờ có điều
kiện:

```javascript
// Sai
await page.waitForTimeout(3000);
await expect(page.getByText('Thành công')).toBeVisible();

// Dung: cho den khi so du that su doi
await expect.poll(async () => await api.balance(acc), {
  timeout: 15_000, intervals: [500, 1000, 2000],
}).toBe(expectedBalance);
```

**3. Bất đồng bộ giữa các hệ thống** — kênh trả về "thành công" trước khi core hoàn tất hạch
toán. Đừng kiểm tra số dư ngay lập tức; hãy chờ tới trạng thái cuối cùng với thời gian chờ
hợp lý, và nếu hệ thống có cơ chế thông báo hoàn tất thì chờ theo cơ chế đó.

**4. Môi trường** — core bị reset, đối tác sandbox bảo trì, hết hạn chứng chỉ. Phân loại các
lần fail: lỗi sản phẩm, lỗi test, hay lỗi môi trường. Nếu không phân loại, đội sẽ quen với
việc suite đỏ và bỏ qua cả lỗi thật.

**5. Thứ tự chạy** — test A để lại trạng thái làm test B fail. Chạy suite theo thứ tự ngẫu
nhiên định kỳ để phát hiện phụ thuộc ẩn.

**Quy tắc xử lý test flaky**: cách ly ngay (đánh dấu quarantine, vẫn chạy nhưng không chặn
merge), mở phiếu theo dõi, sửa trong vòng một sprint. Không để test flaky nằm trong suite
chặn merge, và cũng không xóa nó đi — xóa là mất vùng phủ mà không ai biết.

## Cấu trúc dự án test

```
tests/
├── config/                 # cau hinh theo moi truong, KHONG chua bi mat
├── fixtures/               # du lieu tham chieu tinh: bieu phi, ma loi, lich le
├── factories/              # sinh du lieu dong: account, customer, card
├── clients/                # lop goi API: core, kenh, doi tac
├── pages/                  # Page Object cho UI
├── helpers/
│   ├── assertions.py       # assert_balanced, assert_balance_changed, ...
│   └── db.py               # truy van kiem tra but toan
├── suites/
│   ├── smoke/              # 5-10 phut, chan merge
│   ├── regression/         # day du, chay dem
│   ├── compliance/         # anh xa toi dieu khoan phap quy
│   └── performance/
└── reports/
```

Điểm quan trọng: **tách lớp gọi API ra khỏi test**. Khi đặc tả API đổi, chỉ sửa một chỗ. Ở
ngân hàng, API nội bộ đổi thường xuyên hơn giao diện.

Bí mật (mật khẩu tài khoản test, khóa API sandbox) lấy từ biến môi trường hoặc kho bí mật,
không bao giờ nằm trong repo. Có một file `.env.example` liệt kê tên biến mà không có giá trị.

## CI/CD

Phân tầng theo thời gian chạy:

| Giai đoạn | Chạy gì | Thời lượng mục tiêu | Chặn merge |
|---|---|---|---|
| Pre-commit | Lint, quét bí mật | < 30 giây | Có |
| Trên mỗi PR | Unit + API smoke + quét bí mật | < 10 phút | Có |
| Sau khi merge | Bộ API đầy đủ + E2E luồng chính | < 40 phút | Có (chặn triển khai) |
| Hằng đêm | Hồi quy đầy đủ, đa trình duyệt, mobile | vài giờ | Không, nhưng phải có người xem |
| Hằng tuần | Tải, soak, quét bảo mật | | Không |
| Trước phát hành | Bộ tuân thủ + đối soát + diễn tập khôi phục | | Có |

**Nguyên tắc cho môi trường ngân hàng:**
- Pipeline không bao giờ được trỏ vào môi trường có dữ liệu thật. Thêm một bước kiểm tra
  cứng: nếu URL đích khớp mẫu của production thì dừng ngay.
- Kết quả chạy phải lưu trữ đủ lâu để phục vụ kiểm toán, không chỉ 30 ngày mặc định.
- Máy chạy CI thường không ra được internet → phải có kho gói nội bộ. Tính đến việc này từ
  đầu, vì nó quyết định được cả framework.

```yaml
# Chan chay nham vao production
- name: Kiem tra moi truong
  run: |
    if echo "$BASE_URL" | grep -Eq '(prod|production|\.vn/?$)'; then
      echo "TU CHOI: BASE_URL tro toi moi truong that: $BASE_URL"
      exit 1
    fi
```

## Môi trường test

Ngân hàng thường có nhiều tầng môi trường, và biết mình đang ở đâu là điều kiện tiên quyết:

| Môi trường | Dữ liệu | Đối tác | Dùng để |
|---|---|---|---|
| DEV | Giả, ít | Mock | Dev tự test |
| SIT | Giả, đầy đủ quan hệ | Mock + một ít sandbox | Test tích hợp, tự động hóa chính |
| UAT | Giả, giống thật về khối lượng | Sandbox thật | Nghiệp vụ nghiệm thu |
| Pre-prod | Giả, cấu hình như thật | Sandbox thật | Diễn tập phát hành, test hiệu năng |
| PROD | Thật | Thật | **Chỉ test khói có kiểm soát, với tài khoản riêng, số tiền nhỏ, có phê duyệt** |

Test trên môi trường thật (nếu chính sách cho phép) phải: dùng tài khoản test đã đăng ký và
loại khỏi báo cáo, số tiền tối thiểu, chỉ các luồng đã duyệt, có người trực, và tự dọn dẹp.
Ghi rõ trong tài liệu ai được phép chạy và chạy lúc nào.

**Hồ sơ môi trường**: duy trì một file mô tả mỗi môi trường — phiên bản core, đối tác nào là
thật, lịch reset dữ liệu, ai liên hệ khi hỏng. Phần lớn thời gian điều tra "bug" thực ra là
thời gian phát hiện ra môi trường đã đổi mà không ai báo.
