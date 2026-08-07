---
title: "ToaAn — Tổng quan dự án (Project Overview / PDR)"
template: project-overview-pdr
version: "1.0"
status: "Đang dùng"
date: "2026-08-07"
---

# ToaAn — Tổng quan dự án

Tài liệu này trả lời: dự án này giải quyết việc gì, cho ai, phạm vi tới đâu và khi nào coi là đạt.
Chi tiết kỹ thuật nằm ở [`system-architecture.md`](system-architecture.md);
bản đồ code ở [`codebase-summary.md`](codebase-summary.md).

## 1. Bối cảnh

Hệ thống được kiểm thử (SUT) là **Cổng Dịch vụ Tư pháp** (`https://demo-dichvutuphap.gsfpt.com/`) —
một SPA cho phép người dân nộp đơn khởi kiện / yêu cầu trực tuyến. Nghiệp vụ trọng tâm là
**Tạo đơn / Nộp đơn** dưới dạng wizard 6 bước:

| Bước | Nội dung | Page Object |
|---|---|---|
| 0 | Đăng nhập (có captcha) + Bảng điều khiển → Nộp đơn mới | `LoginPage`, `DashboardPage` |
| 1 | Chọn loại đơn, loại việc, tòa án nhận đơn | `TaoDonPage` |
| 2 | Thông tin nguyên đơn (cá nhân / tổ chức, đồng nguyên đơn, người đại diện) | `NguyenDonPage` |
| 3 | Thông tin bị đơn / bên bị kiện (nhiều bị đơn, người liên quan) | `BiDonPage` |
| 4 | Nội dung đơn (form cũ, upload file, hoặc eform động trong iframe) | `NoiDungDonPage` |
| 5 | Tải tài liệu và chứng cứ | `TaiLieuPage` |
| 6 | Xem lại và gửi đơn | `XemLaiGuiDonPage` |

Kiểm thử tay 6 bước × nhiều loại đơn × nhánh cá nhân/tổ chức là việc tốn công và dễ bỏ sót.
Dự án này tự động hóa luồng đó và biến việc "khai báo một kịch bản" thành một dòng dữ liệu
thay vì một method Java mới.

## 2. Người dùng của repo

| Nhóm | Việc họ làm | Cửa vào |
|---|---|---|
| **QA vận hành** | Khai báo case, chạy, đọc báo cáo, phân loại fail | Dashboard cục bộ (`scripts/chay.cmd` mục 9) hoặc Google Sheet |
| **Dev automation** | Sửa locator khi UI đổi, thêm bước/nhánh mới, mở rộng ma trận độ phủ | Code trong `src/main/java` |
| **CI** | Cổng chất lượng tự động trên mỗi push/PR | `.github/workflows/ci.yml` |

Nguyên tắc phân vai: QA không cần chạm Java. Case **cấu hình** đi vào Google Sheet hoặc
`src/test/resources/local-cases.json`; chỉ case **framework** (login, discovery, unit, ma trận)
mới sống trong Java test class. Chi tiết ở [`WORKFLOW.md`](WORKFLOW.md) mục 4.

## 3. Mục tiêu

1. Luồng tạo đơn điền đúng dữ liệu → đi tới bước cấu hình (0–6) → tùy chọn gửi đơn.
2. Ca âm: hệ thống **phải** chặn khi bỏ trống hoặc điền sai field bắt buộc. Không chặn = lỗ hổng
   validation, tính là FAIL.
3. Đăng nhập: 1 ca dương + 3 ca âm (sai mật khẩu, sai captcha, trống mật khẩu).
4. Độ phủ catalog qua ma trận smoke (~3) / mid (~39) / full (~106 pairwise).
5. Báo cáo HTML tự chứa, có ảnh chụp và bảng trường đã nhập ở từng bước, đủ để triage mà không
   cần chạy lại.

Đặc tả chi tiết theo AC: [`test-spec.md`](test-spec.md).

## 4. Phạm vi v1

Đã đạt và được chốt trong [`V1-CHECKLIST.md`](V1-CHECKLIST.md):

- Bộ tài liệu vận hành: workflow, triage, schema case, tech debt.
- Dashboard `CaseEditorServer` với tab sinh test case và API `generate-cases`.
- `TestCaseGenerator` + unit test trong profile `unit`.
- Ba nguồn case: `local-cases.json`, Google Sheet, ma trận độ phủ.
- Field bắt buộc bước 1–3 fail sớm qua `setTextRequired`; field tùy chọn vẫn soft-skip.
- Tín hiệu assert ổn định ở màn Xem lại (marker + loại đơn) thay vì chỉ `assertNotNull`.
- CI chạy `mvn -B -Punit test` trên mỗi push/PR.

### Ngoài phạm vi

Từ mục "Không làm trong v1" của [`TECH-DEBT.md`](TECH-DEBT.md):

- Migrate sang Playwright.
- Sinh case bằng LLM / Vision.
- Thay `BaoCaoHtml` bằng Allure hoặc Extent.
- Viết lại UI Case Editor.
- Rewrite `WebUI`.

Ngoài phạm vi kiểm thử nói chung (theo [`test-spec.md`](test-spec.md)): hiệu năng tải trang,
bảo mật mạng, tương thích trình duyệt ngoài Chrome.

## 5. Tiêu chí chấp nhận vận hành

Một thay đổi được coi là đạt khi cả bốn điều sau đúng:

1. `mvn -B -Punit test` xanh trên máy local và trên CI.
2. Nếu thay đổi chạm `pages/` hoặc `WebUI`, suite UI hẹp nhất liên quan (`-Plogin` hoặc `-Psmoke`)
   cũng xanh.
3. Báo cáo `test-output/index.html` mở được và mỗi case đỏ đều truy được bước dừng + ảnh chụp.
4. Không có secret trong diff. `src/test/resources/config.properties` nằm trong `.gitignore` và
   không được commit.

Quy trình thay đổi code/doc: [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md).

## 6. Hiện trạng

| Chỉ số | Giá trị |
|---|---|
| File Java | 72 (8 package `main`, 4 package `test`) |
| File lớn nhất | `WebUI.java` ~4100 dòng |
| Suite TestNG XML | 13 (`src/test/resources/suites/`) |
| Maven profile | 11 |
| Stack | Java 17, Selenium 4.46.0, TestNG 7.9.0, Maven Surefire 3.2.5 |
| Unit test (không cần Chrome) | ~46 |

## 7. Rủi ro chính

| # | Rủi ro | Mức | Cách sống chung |
|---|---|---|---|
| 1 | **Captcha login** — OCR Tess4J không phải lúc nào cũng đọc đúng | Cao | `LoginPage` retry; tách suite `login` khi nghi captcha |
| 2 | **Eform bước 4 trong iframe** — schema động, form có thể chưa xuất bản | Cao | Phân loại ENV_DATA/FLAKE theo [`TRIAGE.md`](TRIAGE.md); không thêm sleep mù |
| 3 | **UI demo đổi** — locator và danh mục lệch catalog | Trung bình | `MasterDataSyncTest` đồng bộ `master-data.properties`; tab tra locator trên Dashboard |
| 4 | **Chạy song song** phụ thuộc session Chrome ổn định | Trung bình | `ScenarioDispatch` đảm bảo mỗi kịch bản chỉ chạy một lần; `BrowserClosedException` → Skip |
| 5 | **Google Sheet sai quyền chia sẻ** — master dừng đúng nhưng dễ bị hiểu là hỏng tool | Thấp | Sheet phải để "Bất kỳ ai có đường liên kết — Người xem"; có cache CSV dự phòng |

Danh sách nợ kỹ thuật đầy đủ và mức ưu tiên: [`TECH-DEBT.md`](TECH-DEBT.md).
Thứ tự xử lý: [`project-roadmap.md`](project-roadmap.md).
