# Triage fail — ToaAn v1

## 1. Fail → thu thập bằng chứng

1. Mở báo cáo: Dashboard bước **3. Báo cáo** (menu 9) hoặc `test-output/index.html`.
2. Mở case đỏ: bước dừng, message, ảnh chụp (bấm để phóng to) và bảng **trường đã nhập** trong
   từng bước. Kịch bản không đi qua 6 bước (suite login) có bảng **trường đã nhập (ngoài bước)**
   ngay dưới dải tiến độ. Giá trị ô mật khẩu luôn bị ẩn. File ảnh gốc: `test-output/runs/<mốc>/`.
3. Đọc `test-output/last-run.log` (lượt chạy từ Dashboard / `run-flow`).
4. Ghi lại: suite, `untilStep`, loại đơn/việc, ca dương hay âm (`truongLoi`).

Không đoán trước khi có ảnh + bước trong report.

## 2. Phân đúng **một** loại

| Loại | Dấu hiệu | Việc làm |
|---|---|---|
| **BUG** | UI/API sai thật: validation không chặn khi ca âm kỳ vọng chặn; toast lỗi hệ thống sau Gửi đơn; dữ liệu đúng mà không sang bước | Mở ticket sản phẩm; giữ case FAIL; không “sửa test cho xanh” |
| **TEST_SAI** | Locator/nhãn lệch catalog; `untilStep`/ca âm khai báo sai; ghi chú `GEN_…` trùng ý nhưng field whitelist sai | Sửa `pages` / case JSON·Sheet / generator whitelist; chạy lại hẹp |
| **FLAKE** | Chạy lại cùng case pass/fail không ổn định; race SPA, captcha, eform iframe | Xem mục 3; siết wait có sẵn (`WaitConfig`), không thêm sleep mù; nếu vẫn flake → ghi [`TECH-DEBT.md`](TECH-DEBT.md) |
| **ENV_DATA** | Sai URL/user; sheet mất quyền; catalog UAT đổi chưa sync; thiếu file upload mẫu | Sửa `config.properties`/env (không commit secret); `MasterDataSyncTest`; kiểm tra `testdata/` |

## 3. Flake hay gặp (SPA demo + generator/discovery)

| Hiện tượng | Gợi ý |
|---|---|
| Captcha login | OCR Tess4J; retry trong `LoginPage`; suite login riêng khi nghi captcha |
| Eform bước 4 (iframe) | Chờ ack/toast host; “chưa xuất bản” = ENV/data form; xem `NoiDungDonPage` |
| Toast / validation | Baseline toast trước Gửi đơn; đừng assert chuỗi tuyệt đối dễ đổi — ca âm dùng `thongBaoMongDoi` chứa chuỗi con |
| Step transition | Marker bước sau (`waitForStepTransition`); fail sớm field bắt buộc (`setTextRequired`) thay vì soft-skip âm thầm |
| Soft-skip / `boQua` | Report có “Bỏ qua” trên field bắt buộc → nghi TEST_SAI hoặc bug locator; optional vẫn được skip |
| Parallel abort | `BrowserClosedException` → Skip; xem `ScenarioDispatch` / Chrome crash |
| Discovery CSV | Không assert — CSV chỉ gợi ý ca âm; thông báo gắn vào generator có thể lệch UI mới |

## 4. Không chạy được Maven (ENV_DATA)

| Thông báo | Nguyên nhân | Cách sửa |
|---|---|---|
| `Không tìm thấy Maven. Đã tìm ở: …` (nút Chạy) hoặc `LỖI: Không tìm thấy mvn.` (menu) | Máy không có `mvn` trên PATH và cũng không có Maven đi kèm IntelliJ ở nơi đã dò | Cài Maven, hoặc thêm `run.mavenCmd=C:/…/bin/mvn.cmd` vào `src/test/resources/run-flow.properties` (dùng được cả `-Drun.mavenCmd=…` và env `TOAAN_RUN_MAVENCMD`) |
| `Maven thoát ngay với mã 1` kèm cuối log | Thường là `JAVA_HOME` sai/thiếu, hoặc lỗi biên dịch | Đọc đoạn log trả về; JAVA_HOME được tự điền từ JDK đang chạy Dashboard nên nếu vẫn lỗi thì xem dòng ERROR đầu tiên trong `test-output/last-run.log` |
| `CreateProcess error=2` | Bản cũ hardcode đường dẫn IntelliJ theo phiên bản — không còn xảy ra sau khi dò qua `MavenResolver` | Nếu tái xuất hiện: kiểm tra `run.mavenCmd` có trỏ vào `mvn.cmd` (không phải file `mvn` không đuôi) |

Cùng cách dò được dùng cho nút Chạy trên Dashboard (`MavenResolver`) và cho menu/`run-flow.ps1`
(`scripts/lib-maven.ps1`), nên khai báo một lần là cả hai kênh cùng chạy.

## 5. Sau khi phân loại

- Cập nhật case / code đúng tầng ([`WORKFLOW.md`](WORKFLOW.md)).
- Chạy lại: unit (nếu đụng matrix/parser/generator) → smoke hoặc đúng 1 case master.
- Đóng khi report xanh ổn định hoặc bug đã gắn ticket.
