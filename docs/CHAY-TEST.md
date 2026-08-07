# Cách chạy test

Tài liệu vận hành đầy đủ: [`WORKFLOW.md`](WORKFLOW.md) · triage fail: [`TRIAGE.md`](TRIAGE.md) · schema case: [`CASE-SCHEMA.md`](CASE-SCHEMA.md).

## Cách dễ nhất

```bat
.\scripts\chay.cmd
```

Dùng **↑ / ↓** để chọn, **Enter** để vào (hoặc bấm phím số 1–9). Chọn xong — **tự mở Chrome và chạy**.

| Số | Việc |
|----|------|
| **1** | **Chạy theo Google Sheet** — danh sách case lấy từ sheet (chỉ hỏi số Chrome) |
| 2 | Cấu hình case tại chỗ — loại đơn / loại việc / CN\|TC / dừng bước (1–3 Chrome) |
| 3 | Smoke nhanh — 1 Chrome |
| 4 | Smoke nhanh — 3 Chrome |
| 5 | Chỉ đăng nhập |
| 6 | Mid regression — 3 Chrome |
| 7 | Full coverage — 3 Chrome (lâu) |
| 8 | Xem cấu hình (không chạy) |
| **9** | **Mở Dashboard web** (`http://localhost:8787`) — **1. Chọn case → 2. Danh sách chạy → 3. Báo cáo** |

### Menu 1 — chạy theo Google Sheet

Mỗi dòng trên sheet là một case. Cột:

```
Chạy | Loại đơn | Loại việc | Chủ thể | Tư cách | Tòa án | Số bị đơn
     | Đồng NĐ | Đại diện | Liên quan | TL bổ sung | Đến bước | Gửi đơn | Ghi chú
```

- Cột **Chạy**: gõ `x` cho dòng muốn chạy — **để trống là bỏ qua dòng đó**.
- Ô trống ở các cột nhánh = để automation tự chọn.
- **Đến bước** `0`–`6` (`6` = điền đủ tới màn Xem lại). Chỉ khi `Đến bước = 6` thì cột
  **Gửi đơn** = `x` mới bấm Gửi đơn thật.
- Sheet phải chia sẻ **"Bất kỳ ai có đường liên kết — Người xem"**.

Link sheet nằm ở khoá `run.casesSheet` trong `src/test/resources/run-flow.properties`.
Số case trên sheet có thể nhiều hơn số Chrome — các case còn lại xếp hàng.

### Menu 2 — ví dụ

- Case 1: Dân sự → Hợp đồng → Cá nhân → dừng bước 3  
- Case 2: Hành chính → Quyết định → Tổ chức → điền đủ + gửi đơn  

→ ghi `run.cases=...`, mở đúng số Chrome đã chọn.

### Menu 9 — Dashboard web theo 3 bước

1. `.\scripts\chay.cmd` → chọn **9**.
2. **1. Chọn case** → chọn một màn ở cột trái → lọc **Ca âm** (mặc định), Ca dương hoặc Tất cả → tick case → **Thêm N case vào danh sách chạy**.
3. Màn **Đăng nhập** tách riêng: ca dương có thể thêm vào danh sách; 3 ca âm chạy bằng
   **Chạy kiểm tra đăng nhập**, không thêm vào `local-cases`.
4. **2. Danh sách chạy** → **tick cột «Chạy» cho ca âm** (đề xuất ca âm được thêm ở trạng thái tắt)
   → **Lưu tất cả xuống file** → **Chạy case đã lưu**.
   Nút Chạy bị khóa khi còn thay đổi chưa lưu hoặc chưa bật case nào — dòng trạng thái bên phải
   thanh hành động nói rõ đang thiếu bước nào.
5. Maven chạy nền; khi xong vào **3. Báo cáo** → **Tải lại báo cáo**.
6. **Nâng cao** chứa Nhập Google Sheet (ghi đè), tra locator và đường dẫn tài liệu.

Muốn chạy case của một màn: chọn các case ở màn đó → thêm → sang Danh sách chạy → lưu → chạy.

Đề xuất lấy từ `master-data.properties` + bản đồ field ca âm (`FieldCoverageCatalog`: whitelist
`TRUONG_LOI_HOP_LE` ∩ field `tryFieldOverride` ép được, gắn theo biến thể CN/TC/Phá sản và
textarea/eform — xem `docs/CASE-SCHEMA.md`).

**Muốn phủ field tốt hơn thì chạy `mvn -Pdiscovery test` trước khi Sinh:** discovery ghi CSV vào
`test-output/discovery-sweep/`, generator đọc CSV mới nhất để gắn `thongBaoMongDoi` thật (thay vì
để trống) và đánh dấu field từng bị hệ thống chặn. Dòng meta trên tab hiện `phủ field ca âm: x/y (z%)`
kèm danh sách field chưa có ca âm.

---

## Máy không tìm thấy Maven

Menu và nút Chạy tự dò `mvn` theo thứ tự: khoá `run.mavenCmd` → `MAVEN_HOME`/`M2_HOME` → PATH →
Maven đi kèm IntelliJ (tự lấy bản mới nhất, không cố định phiên bản). Máy cài Maven ở chỗ lạ thì
thêm một dòng vào `src/test/resources/run-flow.properties`:

```properties
run.mavenCmd=C:/Program Files/apache-maven-3.9.9/bin/mvn.cmd
```

`JAVA_HOME` cũng được điền tự động (JDK đang chạy, `~/.jdks`, JBR của IntelliJ). Xem thêm mục 4
trong [`TRIAGE.md`](TRIAGE.md).

---

## File cấu hình (nếu sửa tay)

`src/test/resources/run-flow.properties`

Rồi: `.\scripts\run-flow.cmd`

---

## Lần đầu

1. Copy `config.example.properties` → `config.properties`
2. Điền username / password / baseUrl
3. `.\scripts\chay.cmd`

Báo cáo: `test-output/index.html` (tự mở sau khi chạy xong)
