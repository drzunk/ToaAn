# Triage fail — ToaAn v1

## 1. Fail → thu thập bằng chứng

1. Mở báo cáo: Dashboard tab **Dashboard** (menu 9) hoặc `test-output/index.html`.
2. Mở case đỏ: bước dừng, message, screenshot trong `test-output/runs/<mốc>/`.
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

## 4. Sau khi phân loại

- Cập nhật case / code đúng tầng ([`WORKFLOW.md`](WORKFLOW.md)).
- Chạy lại: unit (nếu đụng matrix/parser/generator) → smoke hoặc đúng 1 case master.
- Đóng khi report xanh ổn định hoặc bug đã gắn ticket.
