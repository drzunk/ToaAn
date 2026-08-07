---
title: "ToaAn — Quy ước code"
template: code-standards
version: "1.0"
status: "Đang dùng"
date: "2026-08-07"
---

# ToaAn — Quy ước code

**Nguồn chính là [README mục 10](../README.md#10-quy-ước-code)** — thứ tự ưu tiên locator, cấm gọi
Selenium trực tiếp từ Page Object, cấm hard-code timeout và nhãn tiếng Việt, mỗi thread một Chrome,
rẽ nhánh UI qua `DataDictionary`. Đọc mục đó trước.

Tài liệu này chỉ bổ sung những quy tắc đang sống trong practice review mà README chưa ghi, cùng các
cổng chất lượng phải qua.

## 1. Field bắt buộc phải fail sớm

`WebUI` có hai họ API cho việc điền text, và chọn sai họ là lỗi hay gặp nhất:

| API | Hành vi khi field ẩn / disabled | Dùng cho |
|---|---|---|
| `setTextWithCheck` | Bỏ qua âm thầm, ghi log "Bỏ qua" | Field **tùy chọn** |
| `setTextRequired` | Ném lỗi ngay, nêu tên field | Field **bắt buộc** trên UI |
| `setTextForMaskedInputRequired` | Như trên, cho input có mask | Field bắt buộc dạng ngày/số |

Lý do: soft-skip trên field bắt buộc làm test đi tiếp với form thiếu dữ liệu rồi fail ở một bước xa
hơn, hoặc tệ hơn là **pass giả**. Trong báo cáo, thấy log "Bỏ qua" trên một field bắt buộc thì nghi
TEST_SAI hoặc lỗi locator, không phải chuyện bình thường.

Giá trị rỗng vẫn được truyền nguyên vẹn xuống Required API — đó là điều kiện để ca âm "bỏ trống
field bắt buộc" kiểm tra được validation. Helper chuyển đổi dữ liệu (ví dụ `namSinhTuNgay` trong
`NguyenDonPage`, `ngaySinhTuNamSinh` trong `BiDonPage`) phải trả chuỗi rỗng khi đầu vào rỗng, tuyệt
đối không thay bằng giá trị mặc định.

Hiện trạng chuyển đổi và phần soft-skip còn cố ý giữ lại: TD-02 trong [`TECH-DEBT.md`](TECH-DEBT.md).

## 2. Chờ đợi

Không thêm `Thread.sleep`. Mọi timeout và thời gian "lắng" nằm trong
`src/main/java/vn/tuphap/automation/ui/WaitConfig.java`. Nếu một chỗ cần chờ lâu hơn, sửa hằng số ở
đó hoặc dùng `-Dtaodon.wait.scale`, đừng vá tại chỗ.

Test bị flake không được chữa bằng sleep. Xem mục 3 của [`TRIAGE.md`](TRIAGE.md) trước; nếu vẫn
flake thì ghi vào [`TECH-DEBT.md`](TECH-DEBT.md).

## 3. Chỗ đặt một kịch bản mới

Theo [`WORKFLOW.md`](WORKFLOW.md) mục 4:

| Loại kịch bản | Đặt ở đâu |
|---|---|
| Case cấu hình: loại đơn, `untilStep`, ca âm form | Google Sheet hoặc `src/test/resources/local-cases.json` |
| Login (kể cả ca âm), discovery, unit, ma trận độ phủ | Java test class |
| Sửa locator hoặc bước wizard | `pages/`, nếu cần thì `WebUI` |
| Danh mục UI thay đổi | Chạy `MasterDataSyncTest` rồi commit `master-data.properties` |

**Không thêm method vào Java test class cho một case nghiệp vụ một-off.** Đó là dấu hiệu case này
đáng lẽ phải nằm trong Sheet hoặc JSON.

Test class mới phải khai báo group và được thêm vào đúng suite XML trong
`src/test/resources/suites/`, nếu không nó sẽ không bao giờ chạy. Unit test phải chạy được mà không
cần Chrome và không gọi mạng.

## 4. Bảo mật

- **Không commit `src/test/resources/config.properties`.** File đã nằm trong `.gitignore`; mẫu để
  copy là `config.example.properties`.
- Credential truyền qua biến môi trường `TOAAN_USERNAME` / `TOAAN_PASSWORD` / `TOAAN_BASE_URL`.
  Biến môi trường thắng file với credential.
- Trên CI, ba giá trị trên là GitHub secrets. Job smoke tự dừng khi thiếu, không chạy với giá trị rỗng.
- Giá trị mật khẩu bị che trong log và báo cáo — đừng thêm đường ghi log nào vòng qua chỗ này.
- Google Sheet dùng CSV công khai, **không** có API key hay service account trong repo. Đừng thêm.

## 5. Cổng chất lượng

| Khi nào | Lệnh |
|---|---|
| Mọi thay đổi, bắt buộc | `mvn -B -Punit test` |
| Chạm `pages/` hoặc `WebUI` | thêm `mvn -Plogin test` hoặc `mvn -Psmoke test` |
| Chạm ma trận độ phủ / parser sheet / generator | `mvn -B -Punit test` đã phủ |
| Đổi danh mục UI | `mvn test -Dtest=MasterDataSyncTest` rồi kiểm tra diff `master-data.properties` |

Không sửa test cho xanh khi nguyên nhân là bug thật của sản phẩm — phân loại theo
[`TRIAGE.md`](TRIAGE.md) trước, giữ case FAIL và mở ticket.

## 6. Commit

Dùng conventional commit, không nhắc tới AI trong message. Giữ commit gọn theo một mục đích. Không
đưa mã phase, mã plan hay mã finding của audit vào comment code, tên migration, tên test hay commit
message — mô tả thẳng hành vi hoặc bất biến.

Quy trình đầy đủ từ lúc nhận việc tới lúc merge: [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md).
