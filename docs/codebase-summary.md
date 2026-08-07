---
title: "ToaAn — Bản đồ codebase"
template: codebase-summary
version: "1.0"
status: "Đang dùng"
date: "2026-08-07"
---

# ToaAn — Bản đồ codebase

Đây là **bản đồ nhanh** để định vị. Mô tả chi tiết từng file (chức năng, API, cạm bẫy) đã có sẵn ở
[README mục 5](../README.md#5-mô-tả-từng-file-theo-package), sơ đồ thư mục ở
[README mục 4](../README.md#4-sơ-đồ-thư-mục). Tài liệu này không chép lại nội dung đó.

Số dòng đo ngày 2026-08-07. Vài con số trong README mục 5 được viết ở thời điểm cũ hơn nên có thể
thấp hơn thực tế.

## 1. Tổng quan

| Hạng mục | Giá trị |
|---|---|
| File Java | 72 (57 `main` + 15 `test`) |
| Dòng Java | ~25.000 (~22.150 `main` + ~2.870 `test`) |
| Package `main` | 8 |
| Package `test` | 4 |
| Suite TestNG XML | 13 |
| Maven profile | 11 |

## 2. Package `src/main/java/vn/tuphap/automation/`

| Package | File | ~LOC | Trách nhiệm | Chi tiết |
|---|---:|---:|---|---|
| `pages` | 10 | 5.665 | Page Object cho từng màn của wizard 6 bước | README mục 4.5 |
| `ui` | 7 | 4.926 | Lớp bọc Selenium, wait, locator, OCR captcha | README mục 4.7 |
| `report` | 11 | 3.808 | Thu dữ liệu chạy, chụp màn hình, dựng HTML | README mục 4.6 |
| `data` | 10 | 3.336 | Mô hình kịch bản, sinh dữ liệu, catalog, ma trận độ phủ | README mục 4.3 |
| `config` | 4 | 1.557 | Đọc credential, cấu hình chạy, case từ Google Sheet | README mục 4.1 |
| `caseui` | 4 | 1.543 | Dashboard cục bộ, sinh đề xuất case, dò Maven | — |
| `core` | 8 | 807 | Vòng đời WebDriver, chạy song song, bố cục cửa sổ | README mục 4.2 |
| `flow` | 3 | 509 | Điều phối 6 bước, 2 exception nghiệp vụ | README mục 4.4 |

Package `caseui` ra đời sau nên chưa có mục riêng trong README mục 5. Bốn file của nó:

| File | Việc |
|---|---|
| `CaseEditorServer.java` | HTTP server cổng 8787, phục vụ Dashboard và 8 endpoint `/api/*` |
| `TestCaseGenerator.java` | Sinh đề xuất case theo màn, tiền tố `GEN_…` |
| `FieldCoverageCatalog.java` | Ánh xạ field ca âm ↔ bước/biến thể form, tính độ phủ |
| `MavenResolver.java` | Dò Maven và JDK khi máy không có `mvn` trên `PATH` |

## 3. File lớn nhất

Bảy file dưới đây chiếm khoảng **48%** tổng số dòng. Sửa chúng cần cẩn trọng hơn mức trung bình.

| File | LOC | Ghi chú |
|---|---:|---|
| `ui/WebUI.java` | 4.100 | Điểm nghẽn Selenium duy nhất — TD-01 |
| `report/BaoCaoHtml.java` | 2.054 | Dựng `index.html` tự chứa |
| `pages/NoiDungDonPage.java` | 1.220 | Bước 4 có 3 dạng UI (eform iframe / upload / textarea) |
| `data/DataGenerator.java` | 1.184 | Sinh dữ liệu theo seed cố định + override ca âm |
| `pages/NguyenDonPage.java` | 1.109 | Bước 2, nhánh cá nhân/tổ chức/đồng nguyên đơn |
| `pages/BiDonPage.java` | 1.083 | Bước 3, nhiều bị đơn + cơ quan hành chính |
| `pages/XemLaiGuiDonPage.java` | 1.002 | Bước 6, xem lại và gửi đơn |

Ngoài ra `config/RunFlowConfig.java` (761) là façade cấu hình duy nhất, và
`data/UiMasterDataReader.java` (537) là scraper danh mục.

## 4. Test → suite → profile

Suite XML nằm ở `src/test/resources/suites/`.

| Test class | Suite XML | Lệnh chạy | Chrome? |
|---|---|---|---|
| `tests/ConfiguredCasesTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `tests/MidCoverageMatrixTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `tests/FullCoverageMatrixTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `config/CaseSheetSourceTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `caseui/TestCaseGeneratorTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `caseui/FieldCoverageCatalogTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `caseui/MavenResolverTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `report/BaoCaoHtmlTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `report/BaoCaoTeeTest` | `testng-unit.xml` | `mvn -Punit test` | Không |
| `tests/LoginTest` | `testng-login.xml` | `mvn -Plogin test` | Có |
| `tests/TaoDonTest` | `testng-smoke.xml` | `mvn -Psmoke test` | Có |
| `tests/TaoDonTest` | `testng-mid.xml` | `mvn -Pmid test` | Có |
| `tests/TaoDonTest` | `testng.xml` | `mvn -Pfull test` | Có |
| `tests/TaoDonTest` | `testng-parallel-{smoke,mid,full,buoc23}.xml` | `mvn -Pparallel-smoke test` … | Có |
| `tests/TaoDonTest` | `testng-buoc23.xml` | không có profile — `scripts/run-flow.ps1` gọi qua `-DsuiteXmlFile` | Có |
| `tests/MasterExecutionTest` | `testng-master.xml` | `mvn -Pmaster test` | Có |
| `tests/FieldDiscoverySweepTest` | `testng-discovery.xml` | `mvn -Pdiscovery test` | Có |
| `report/BaoCaoTuongTacTest` | `testng-baocao-ui.xml` | không có profile — chọn suite trực tiếp | Có |
| `tests/MasterDataSyncTest` | không thuộc suite nào | `mvn test -Dtest=MasterDataSyncTest` | Có |

Chín class đầu tạo thành profile `unit` — đây là cổng bắt buộc trước mọi merge và cũng là thứ CI chạy.

## 5. Tài nguyên

| Đường dẫn | Vai trò |
|---|---|
| `src/main/resources/case-editor/index.html` | Toàn bộ giao diện Dashboard, SPA tự chứa, không build step |
| `src/main/resources/master-data.properties` | Catalog danh mục, sinh lại bằng `MasterDataSyncTest` |
| `src/main/resources/tessdata/eng.traineddata` | Dữ liệu Tesseract cho OCR captcha (~23 MB) |
| `src/test/resources/run-flow.properties` | Bảng điều khiển chính — xem [README mục 6.2](../README.md#62-srctestresourcesrun-flowproperties--bảng-điều-khiển-chính) |
| `src/test/resources/config.example.properties` | Mẫu credential; bản thật `config.properties` đã gitignore |
| `src/test/resources/local-cases.json` | Case cục bộ, mặc định rỗng |
| `src/test/resources/testdata/` | `sample.pdf`, `sample.png` dùng để upload |
| `src/test/resources/caseui/field-discovery-fixture.csv` | Fixture cho `FieldCoverageCatalogTest` |

## 6. Scripts

| File | Việc |
|---|---|
| `scripts/chay.cmd` → `chay.ps1` | Menu tương tác; ghi lựa chọn vào `run-flow.properties` |
| `scripts/run-flow.cmd` → `run-flow.ps1` | Đọc `run-flow.properties` → chọn profile → gọi Maven → ghi log |
| `scripts/lib-maven.ps1` | Dò Maven/JDK, bản PowerShell của `MavenResolver` |

## 7. Đi tiếp

- Quyết định kiến trúc và ranh giới tầng: [`system-architecture.md`](system-architecture.md)
- Quy ước khi sửa code: [`code-standards.md`](code-standards.md)
- Quy trình thay đổi: [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md)
- Nợ kỹ thuật: [`TECH-DEBT.md`](TECH-DEBT.md)
