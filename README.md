# ToaAn — Automation Test Cổng Dịch vụ Tư pháp

Bộ kiểm thử tự động (UI automation) cho cổng nộp đơn trực tuyến của ngành Tòa án
(`https://demo-dichvutuphap.gsfpt.com/`), tập trung vào nghiệp vụ **Tạo đơn / Nộp đơn trực tuyến**
gồm 6 bước.

| Hạng mục | Giá trị |
|---|---|
| Maven coordinates | `vn.tuphap.automation:ToaAn:1.0-SNAPSHOT` |
| Java | 17 (`maven.compiler.release=17`) |
| Selenium | 4.46.0 |
| TestNG | 7.9.0 |
| Báo cáo HTML | Tự dựng (`BaoCaoHtml` → `test-output/index.html`), dữ liệu lưu bằng Gson 2.10.1 |
| Sinh file mẫu upload | Apache POI 5.2.5 (`poi-ooxml`) — .xlsx/.docx để kiểm thử bước 5 |
| Sinh dữ liệu | Datafaker 2.1.0 (locale `vi`) |
| OCR captcha | Tess4J 5.11.0 |
| Khác | commons-io 2.15.1, log4j 2.23.0, slf4j-simple 1.7.36 |

---

## Mục lục

1. [Bắt đầu nhanh](#1-bắt-đầu-nhanh)
2. [Vận hành v1](#2-vận-hành-v1)
3. [Kiến trúc & luồng thực thi](#3-kiến-trúc--luồng-thực-thi)
4. [Sơ đồ thư mục](#4-sơ-đồ-thư-mục)
5. [Mô tả từng file theo package](#5-mô-tả-từng-file-theo-package)
6. [Bộ test & suite XML](#6-bộ-test--suite-xml)
7. [File cấu hình](#7-file-cấu-hình)
8. [Báo cáo đầu ra](#8-báo-cáo-đầu-ra)
9. [Scripts](#9-scripts)
10. [Quy ước code](#10-quy-ước-code)
11. [Xử lý sự cố](#11-xử-lý-sự-cố)

---

## 1. Bắt đầu nhanh

### Lần đầu

1. Copy `src/test/resources/config.example.properties` → `src/test/resources/config.properties`
   (file này nằm trong `.gitignore`, **không commit**).
2. Điền `username` / `password` / `baseUrl`.
   Có thể thay bằng biến môi trường `TOAAN_USERNAME` / `TOAAN_PASSWORD` / `TOAAN_BASE_URL`
   (biến môi trường **ưu tiên hơn** file).
3. Chạy menu:

```bat
.\scripts\chay.cmd
```

Dùng **↑ / ↓** chọn, **Enter** xác nhận (hoặc bấm phím số). Chọn xong tự mở Chrome và chạy.

4. Xem báo cáo: `test-output/index.html` (tự mở sau khi chạy xong)

### Chạy trực tiếp bằng Maven

```bash
mvn -Punit  test          # unit test ma trận độ phủ + bộ đọc Google Sheet — không cần trình duyệt
mvn -Psmoke test          # smoke 1 Chrome
mvn -Pparallel-smoke test # smoke 3 Chrome song song
mvn -Plogin test          # chỉ kiểm tra đăng nhập
mvn -Pmaster test         # chạy theo run-flow.properties (untilStep / submit / cases)
mvn -Pfull  test          # full coverage (lâu)

mvn test -Dtest=MasterDataSyncTest   # quét lại master-data.properties từ UI thật
```

### Chạy danh sách case trên Google Sheet

Test case được khai báo trên Google Sheet (nguồn mặc định) — xem
[mục 6.4](#64-lấy-test-case-từ-google-sheet-runcasessheet) để biết các cột.

1. Điền case vào sheet (mỗi dòng 1 case, cột `Chạy` = `x` cho dòng muốn chạy).
2. `.\scripts\chay.cmd` → chọn mục **1. Chạy theo Google Sheet** → chọn số Chrome.

Sheet phải chia sẻ **"Bất kỳ ai có đường liên kết — Người xem"**; link cấu hình ở khoá
`run.casesSheet` trong `run-flow.properties`.

### Chạy theo file cấu hình (không dùng menu)

Sửa `src/test/resources/run-flow.properties`, rồi:

```bat
.\scripts\run-flow.cmd
```

Xem thêm: [`docs/CHAY-TEST.md`](docs/CHAY-TEST.md).

---

## 2. Vận hành v1

Cửa hàng ngày cho **case cấu hình + sinh đề xuất + xem report**:

```bat
.\scripts\chay.cmd
```

Chọn **mục 9 — Dashboard** (`http://localhost:8787`) và đi theo một luồng:
**1. Chọn case** → **2. Danh sách chạy** (Lưu rồi Chạy) → **3. Báo cáo**.
Sheet, locator và tài liệu vận hành nằm trong **Nâng cao**.
Kênh song song: mục **1** (Google Sheet). Ma trận độ phủ: smoke / mid / full (menu 3–7).

| Doc | Nội dung |
|---|---|
| [`docs/WORKFLOW.md`](docs/WORKFLOW.md) | Vòng đời, bảng suite, chọn kênh Dashboard/Sheet/matrix |
| [`docs/TRIAGE.md`](docs/TRIAGE.md) | Phân loại fail BUG / TEST_SAI / FLAKE / ENV_DATA |
| [`docs/CASE-SCHEMA.md`](docs/CASE-SCHEMA.md) | Field JSON·Sheet, `untilStep`, ca âm, prefix `GEN_…` |
| [`docs/TECH-DEBT.md`](docs/TECH-DEBT.md) | Nợ chấp nhận v1 / làm sau |
| [`docs/V1-CHECKLIST.md`](docs/V1-CHECKLIST.md) | Definition of Done v1 |
| [`docs/test-spec.md`](docs/test-spec.md) | Đặc tả kiểm thử / AC |

Verify tối thiểu trước merge: `mvn -B -Punit test` (CI: `.github/workflows/ci.yml`).

---

## 3. Kiến trúc & luồng thực thi

### Chuỗi gọi từ lúc bấm chạy

```
scripts/chay.cmd  →  scripts/chay.ps1        Menu ↑↓; chọn nguồn case (Google Sheet / wizard)
                          │                  → GHI run-flow.properties
                          ▼
scripts/run-flow.cmd → scripts/run-flow.ps1  ĐỌC run-flow.properties → chọn Maven -P profile
                          │                  → dựng danh sách -D → gọi mvn test → ghi last-run.log
                          ▼
                    Maven Surefire           suiteXmlFile từ profile (pom.xml)
                          │
                          ▼
              ParallelSuiteAdjuster          IAlterSuiteListener: ép parallel/thread-count
                          │                  theo RunFlowConfig.browsers()
                          ▼
                     TestListener            onStart: đặt mốc lượt chạy + ScreenshotStore.initRun
                          │                  onTest*: ghi log, chụp màn hình, set trạng thái
                          ▼
     MasterExecutionTest / TaoDonTest        DataProvider (parallel) sinh TaoDonScenario
                          │                  (case từ Google Sheet qua CaseSheetSource,
                          │                   hoặc run.cases / bộ smoke-mid-full)
                          ▼
                      TaoDonFlow             Điều phối bước 1 → 6, dừng sớm theo untilStep
                          │
                          ▼
                    Page Objects             TaoDonPage, NguyenDonPage, BiDonPage, ...
                          │
                          ▼
                        WebUI                Lớp bọc Selenium duy nhất (click, dropdown,
                                             địa chỉ, toast, chuyển bước, screenshot)
```

### Phân lớp

| Lớp | Trách nhiệm | Package |
|---|---|---|
| Test | Chỉ mô tả kịch bản + assert, không gọi Selenium | `tests` |
| Flow | Điều phối thứ tự bước, quyết định dừng ở đâu | `flow` |
| Page Object | Biết layout & locator của **một** màn hình | `pages` |
| WebUI | Thao tác Selenium cấp thấp, xử lý mọi quirk của UI | `ui` |
| Data | Sinh & mô hình hoá dữ liệu kịch bản | `data` |
| Config | Đọc cấu hình chạy và credential | `config` |
| Core | Vòng đời WebDriver, chạy song song, bố cục cửa sổ | `core` |
| Report | Thu thập dữ liệu và dựng `index.html` | `report` |

### Bản đồ 6 bước của wizard "Tạo đơn"

Tên bước chuẩn lấy từ `TaoDonReportBuilder.tenBuocDayDu(int)`.

| Bước | Tên bước | Page Object | Method trong `TaoDonFlow` |
|---|---|---|---|
| 0 | Đăng nhập + Bảng điều khiển (Nộp đơn mới) | `LoginPage`, `DashboardPage` | `moFormNopDonMoi()` |
| 1 | Chọn loại đơn, loại việc và tòa án nhận đơn | `TaoDonPage` | `dienBuoc1(s)` |
| 2 | Điền thông tin nguyên đơn | `NguyenDonPage` | `dienBuoc2(s)` |
| 3 | Điền thông tin bị đơn / bên bị kiện | `BiDonPage` | `dienBuoc3(s)` |
| 4 | Điền nội dung đơn | `NoiDungDonPage` (+ `EformDropdownHelper`) | `dienBuoc4(s)` |
| 5 | Tải tài liệu và chứng cứ | `TaiLieuPage` | `dienBuoc5(s)` |
| 6 | Xem lại thông tin và gửi đơn | `XemLaiGuiDonPage` → `GuiDonKetQua` | (kết thúc `chayTheoMasterConfig(s)`) |

Mỗi bước theo cùng một khuôn mẫu:

```
markStepStart → TestActionLog.buoc(n) → điền form → logValidationMessages
              → clickTiepTheo() → waitForStepTransition(marker của bước n+1)
              → logStepDone(n/6) → trangThaiBuoc("Đạt")
```

**Hai điểm dễ nhầm:**

- `TaoDonFlow.chayTheoMasterConfig(s)` **dừng sớm** theo `run.untilStep` — ví dụ `untilStep=3`
  thì sau bước 3 nó `return null` và in "Dừng sau bước 3 (Bị đơn) theo run-flow.properties."
- Nút **Gửi đơn** **không** do flow bấm. `MasterExecutionTest.testMasterExecution` mới kiểm tra
  `RunFlowConfig.submit()` rồi mới gọi `review.thuGuiDonVaChoKetQua()`.

---

## 4. Sơ đồ thư mục

```
ToaAn/
├── README.md                       File này
├── pom.xml                         Maven: dependency, surefire, 12 profile chạy
├── .gitignore                      Bỏ qua target/, test-output/, config.properties, .idea/
│
├── docs/
│   └── CHAY-TEST.md                Hướng dẫn ngắn cho tester: menu chay.cmd + bảng phím
│
├── scripts/                        Bộ chạy trên Windows (không sửa cấu hình hệ thống)
│   ├── chay.cmd                    Wrapper: chcp 65001 + bypass ExecutionPolicy → chay.ps1
│   ├── chay.ps1                    Menu ↑↓ + wizard cấu hình case, GHI run-flow.properties
│   ├── run-flow.cmd                Wrapper → run-flow.ps1
│   └── run-flow.ps1                ĐỌC run-flow.properties → chọn profile → gọi mvn test
│
├── src/main/java/vn/tuphap/automation/
│   ├── config/                     Đọc cấu hình (credential + luồng chạy + Google Sheet case)
│   ├── core/                       Vòng đời WebDriver, chạy song song, bố cục cửa sổ Chrome
│   ├── data/                       Mô hình dữ liệu, catalog master data, ma trận độ phủ
│   ├── flow/                       Điều phối luồng 6 bước + 2 exception nghiệp vụ
│   ├── pages/                      Page Object cho từng màn hình
│   ├── report/                     Thu dữ liệu, dựng index.html, listener TestNG
│   └── ui/                         WebUI (bọc Selenium), timeout, đồng nghĩa nhãn, file mẫu
│
├── src/main/resources/
│   ├── master-data.properties      Catalog dữ liệu chuẩn (auto-generated từ UI)
│   └── tessdata/eng.traineddata    Dữ liệu Tesseract cho OCR captcha (~23 MB)
│
├── src/test/java/vn/tuphap/automation/tests/
│                                   6 test class (xem mục 5)
│
├── src/test/resources/
│   ├── run-flow.properties         ★ Bảng điều khiển chính (suite, số Chrome, link Google Sheet)
│   ├── config.example.properties   Mẫu credential — copy thành config.properties
│   ├── suites/                     11 file TestNG suite XML
│   └── testdata/                   sample.pdf, sample.png dùng để upload
│
├── test-output/                    (gitignored) Báo cáo & log sinh ra sau mỗi lần chạy
├── target/                         (gitignored) Build output của Maven
└── .idea/                          (gitignored) Cấu hình IntelliJ; chỉ vài file nhỏ được commit
```

---

## 5. Mô tả từng file theo package

### 4.1 `config/` — Đọc cấu hình

| File | Chức năng |
|---|---|
| `ConfigReader.java` | Nạp `config.properties` từ classpath một lần trong static block. `getValue(key)` tra theo thứ tự **biến môi trường → system property → file**, ném `RuntimeException` kèm gợi ý nếu không thấy; `getValue(key, default)` nuốt lỗi đó. Tên biến môi trường sinh bởi `toEnvKey()`: tách camelCase bằng `_`, thêm tiền tố `TOAAN_` (`baseUrl` → `TOAAN_BASE_URL`). |
| `RunFlowConfig.java` | Façade duy nhất cho `run-flow.properties` (nạp UTF-8 từ classpath). Mọi lần tra đi qua `raw(key)` theo thứ tự **`-Dkey` → alias `-Dtaodon.*` → env `TOAAN_<KEY>` → file → default**. Cung cấp `suite()`, `browsers()` (kẹp 1–8), `parallel()`, `untilStep()`, `submit()`, `cases()`, `slots()`, `window*()`, `openReport()`, `requireSubmit()`, và bộ ba nguồn case `useSheet()` / `casesSheetUrl()` / `caseSourceLabel()`. `cases()` ưu tiên **Google Sheet** (qua `CaseSheetSource`) rồi mới tới `run.cases`. Định nghĩa 2 record `SlotProfile(untilStep, submit)` và `CaseProfile(loaiDon, loaiViec, chuThe, tuCachNopDon, toaAn, soLuongBiDon, coDongNguyenDon, coNguoiDaiDien, coNguoiLienQuan, coTaiLieuBoSung, ghiChu, truongLoi, giaTriLoi, thongBaoMongDoi, untilStep, submit)` — có thêm constructor gọn 5 trường cho `run.cases` — cùng parser `parseCases`/`parseSlots`. 3 trường cuối (`truongLoi`/`giaTriLoi`/`thongBaoMongDoi`) khai báo **ca âm** (xem `hasNegativeExpectation()` và mục 6.4) — `truongLoi` rỗng = case bình thường như cũ. `bindCaseProfile()`/`clearBoundCase()` gắn độ sâu của một case vào thread đang chạy qua `ThreadLocal`. `applyKnownSystemAliases()` đổ ngược cấu hình sang các system property `taodon.*` (chỉ khi chưa có, để `-D` luôn thắng). |
| `CaseSheetSource.java` | Nạp danh sách test case từ **Google Sheet** (`run.casesSheet`). Đổi link chia sẻ / link có `gid` / ID thuần thành endpoint export CSV công khai (`toCsvExportUrl`) nên **không cần API key hay OAuth** — chỉ cần sheet chia sẻ "Bất kỳ ai có đường liên kết — Người xem". Tải bằng `java.net.http.HttpClient` (timeout 25 s), nhận diện trang HTML trả về = chưa mở quyền. Parser CSV tự viết theo RFC 4180 (ô có dấu phẩy / nháy kép / xuống dòng). `mapHeader` nhận cột theo tên đã chuẩn hoá (bỏ dấu, bỏ hoa thường, bỏ ký tự lạ) nên **thứ tự cột tuỳ ý, cột lạ bị bỏ qua**; quy ước ô trống = "automation tự chọn" (`triState` trả `null`). Chống mất mạng: tải OK → ghi `test-output/cases-sheet-cache.csv`; lỗi → dùng cache kèm cảnh báo thời điểm tải; không cache → trả rỗng để `RunFlowConfig` quay về `run.cases`. Kết quả cache trong bộ nhớ theo (url, gid) vì `cases()` bị gọi rất nhiều lần mỗi lần chạy. |

### 4.2 `core/` — Vòng đời trình duyệt & chạy song song

| File | Chức năng |
|---|---|
| `BaseTest.java` | Lớp cha TestNG, gắn `@Listeners(TestListener.class)`. `createDriver()` dựng `ChromeDriver` (tắt notification, `--remote-allow-origins=*`, `--start-maximized` chỉ khi không song song, tắt password manager, bật `goog:loggingPrefs`), gọi `BrowserLayout.apply` rồi đăng ký vào `DriverContext`. `@AfterSuite` gọi `DriverContext.quitAll()` và mở `test-output/index.html` nếu `run.openReport=true`. |
| `TaoDonBaseTest.java` | Lớp cha cho các test tạo đơn. Bật `reuseBrowserSession()` để **mỗi thread giữ 1 Chrome đã đăng nhập** suốt cả suite: `ensureThreadSessionAndDashboard()` tạo driver + đăng nhập đúng một lần (cờ `ThreadLocal`), `resetToDashboardAfterTest()` (`@AfterMethod`) đưa về Dashboard, `closeThreadBrowser()` (`@AfterClass`) đóng. `BrowserClosedException` được chuyển thành `SkipException` + đánh dấu abort thread. |
| `DriverContext.java` | Sổ đăng ký `WebDriver`/`WebUI` theo thread, cộng thêm `Set<WebDriver> ALL_DRIVERS` toàn cục. `quitCurrent()` chỉ đóng Chrome của thread hiện tại; `quitAll()` dành riêng cho `@AfterSuite`. Bộ ba `abortCurrentThread()` / `isCurrentThreadAborted()` / `clearAbortFlag()` hiện thực quy tắc **"đóng 1 Chrome chỉ dừng thread đó, các browser còn lại chạy tiếp"**. |
| `BrowserLayout.java` | Xếp các cửa sổ Chrome thành lưới để nhìn thấy hết khi chạy song song. `apply(driver)` maximize khi chạy đơn, còn lại chiếm một slot rảnh (`BitSet`, tối đa 16) và chia thành `min(total, 3)` cột dựa trên vùng làm việc AWT (`GAP=10`, `OUTER_MARGIN=12`). Kích thước lấy từ `run.window.width/height/scale`. Slot được gán nhãn TRÁI / GIỮA / PHẢI; `browserLabel()` trả "trình duyệt số N". |
| `BrowserSlot.java` | `ThreadLocal<Integer>` giữ chỉ số slot (0-based) của thread — dùng chung cho việc xếp cửa sổ và tra `run.slots`. Chỉ có `set/get/clear`. |
| `ParallelConfig.java` | Adapter mỏng: static block gọi `RunFlowConfig.applyKnownSystemAliases()`; `threadCount()` = `RunFlowConfig.browsers()`; `isParallel()` = `parallel() && threadCount() > 1`. |
| `ParallelSuiteAdjuster.java` | TestNG `IAlterSuiteListener` (đăng ký **một lần** qua surefire trong `pom.xml`, không khai lại trong XML). Ghi đè mọi `XmlSuite` lúc nạp: nếu song song thì `ParallelMode.METHODS` với `thread-count` và `data-provider-thread-count` = `RunFlowConfig.browsers()`; ngược lại `NONE`/1/1. Nhờ vậy `thread-count="3"` cứng trong XML luôn bị thay bằng giá trị từ `run.browsers`. |
| `ScenarioDispatch.java` | Chống chạy trùng case khi song song. `keyOf(scenario)` ghép khoá từ stt/loaiDon/loaiViec/tuCachNopDon/loaiChuThe/soLuongBiDon/coDongNguyenDon/tomTat; `claim(scenario)` chỉ trả `true` ở lần đầu tiên (backing store là concurrent set); `reset()` được gọi đầu suite. |

### 4.3 `data/` — Dữ liệu kịch bản & catalog

| File | Chức năng |
|---|---|
| `TaoDonScenario.java` | Hợp đồng dữ liệu bất biến cho **một** kịch bản tạo đơn — 60 trường, thay cho mảng `Object[]` 50 cột trước đây: metadata (`stt`, `loaiDon`, `loaiViec`, `toaAn`, `tomTat`), nguyên đơn cá nhân/tổ chức/đại diện, bị đơn, người liên quan, cơ quan hành chính, nội dung bước 4 (`thoiDiemPhatSinh`, `giaTriTranhChap`, `tomTatQuaTrinh`, `yeuCauCuThe`, `canCuPhapLy`), `coTaiLieuBoSung`, `tuCachNopDon`, `soLuongBiDon` (1 hoặc 2) + `biDonThem`, `coDongNguyenDon` + `dongNguyenDon`. Dựng bằng `TaoDonScenario.builder()`. `toBuilder()` trả một `Builder` đã điền sẵn mọi field hiện có — dùng để tạo bản sao đã sửa đúng 1 field (ca âm, xem `DataGenerator.tryFieldOverride`) mà không phải dựng lại từ đầu (class immutable, không có setter). |
| `BiDonData.java` | Value object builder-style cho **một** bị đơn (dùng cho bị đơn #1 và #2): 19 trường phủ cả cá nhân (`hoTen`, `cccd`, `namSinh`…), tổ chức (`tenToChuc`, `loaiHinh`, `mst`…), liên hệ và biến thể cơ quan hành chính (`tenCoQuanHC`, `chucDanhHC`, `nguoiThamQuyenHC`). Mọi setter tự chuyển `null` → `""`. |
| `DongNguyenDonData.java` | Value object cho **một** đồng nguyên đơn (form con xuất hiện sau khi bấm "Thêm"): 16 trường, mặc định `loai="Cá nhân"`, `gioiTinh="Nam"`. |
| `DataGenerator.java` | Nhà máy sinh dữ liệu dựa trên Datafaker (locale `vi`, seed cố định `FAKER_SEED = 20240724L` để tái lập được). Trả `Object[][]` cho DataProvider của TestNG. Các entry point: `generateFullCoverageData()`, `generateMidCoverageData()`, `generateSmokeData()` (3 dòng: ép 1 case Phá sản, 1 case eform "Dân sự / Bồi thường", 1 case ngẫu nhiên không trùng), `generateConfiguredCases(List<CaseProfile>)` (mỗi dòng sheet / mỗi token `run.cases` thành 1 kịch bản: khớp mờ loại đơn/việc/chủ thể/**tòa án** với catalog qua `resolveLoaiDon`/`resolveLoaiViec`/`resolveToaAn`, áp số bị đơn + đồng nguyên đơn + đại diện + người liên quan + tài liệu bổ sung theo cấu hình, ô trống thì giữ nguyên cách sinh theo seed; `resolveSoLuongBiDon`/`resolveCoDongNguyenDon` tự ép về giá trị UI cho phép và in lý do), `generateBuoc23AllLoaiDonData()`, `generateOneRandomScenario()`, `generateScenarioForReviewEdit()`. Mọi dòng đều được `MasterDataCatalog.assertInCatalog` kiểm tra. **`tryFieldOverride(TaoDonScenario, String truongLoi, String value)`** (`public static`, trả `FieldOverrideAttempt(applicable, skipReason, result)`) là lõi dùng chung cho ca âm: thử ép 1 trong 13 field (SĐT/Email/CCCD/Họ tên/Ngày sinh/Ngày cấp/MST × Nguyên đơn/Bị đơn, + Giá trị tranh chấp) sang `value` trên bản sao (`TaoDonScenario.toBuilder()`); field không khớp ngữ cảnh (vd. MST cho nguyên đơn Cá nhân, Email cho bị đơn Hành chính) trả `applicable=false` kèm lý do, không throw. `applyNegativeFieldOverride` (dùng bởi `generateConfiguredCases` khi `CaseProfile.hasNegativeExpectation()`) là hàm mỏng gọi `tryFieldOverride` rồi in log; `FieldDiscoverySweepTest` (mục 5.1) gọi thẳng `tryFieldOverride` để quét nhiều field không qua sheet. |
| `FullCoverageMatrix.java` | Dựng ma trận **pairwise mức B**: với mỗi cặp `(loaiDon, loaiViec)` sinh `PROFILES_PER_PAIR = 3` profile nhánh xoay vòng (riêng Phá sản sinh 1 dòng cho mỗi tư cách), áp ràng buộc UI (Phá sản ép bị đơn tổ chức + 1 bị đơn; thuận tình ly hôn ép 1 bị đơn; nguyên đơn tổ chức thì tắt người đại diện), rồi `fillGaps` thêm dòng để mọi giá trị của mọi trục đều xuất hiện. API: record `BranchSpec(...)`, `build()`, `expectedRowCount()`, `validateCoverage()`, `summarize()`. |
| `MidCoverageMatrix.java` | Rút gọn từ `FullCoverageMatrix.build()` (không tính lại tích Descartes): 1 kịch bản cho mỗi cặp không phải Phá sản, đệm thêm cho đủ `TARGET_REGULAR = 35`, rồi cộng 1 dòng cho mỗi tư cách Phá sản → **35 + 4 = 39 dòng**. |
| `MasterDataCatalog.java` | Nạp & cache `master-data.properties` từ classpath, tách giá trị bằng `\|`. `isProductionOption` lọc bỏ dữ liệu rác (chứa "test in don", "fpt test", "tam, xoa", "xoa sau"); `toaAn` được chuẩn hoá qua `ToaAnCatalog`. Getter cho từng khoá (`getLoaiDon`, `getToaAn`, `getGioiTinh`, …) ném lỗi nếu thiếu khoá; riêng `getTuCachNopDonPhaSan()` có fallback cứng. Xử lý cặp loại đơn–loại việc qua `getAllLoaiDonViecPairs()` / `getLoaiViecByLoaiDon()`. `saveToWorkspace()` ghi ngược file (dùng bởi `MasterDataSyncTest`). |
| `DataDictionary.java` | Façade uỷ quyền mọi tra cứu danh sách sang `MasterDataCatalog`, đồng thời chứa các **predicate rẽ nhánh UI** — thứ quyết định form hiển thị khác nhau: `isToChuc`, `isHanhChinh`, `isPhaSan`, `hasLoaiViecDropdown`, `isHonNhanGiaDinh`, `isThuanTinhLyHon`, `allowsThemBiDon`, `isStandardBiDonUi`, `hasGiaTriTranhChap`, `allowsDongNguyenDon`, `isGiaTriTranhChapRequired`. Hằng `PHA_SAN_LOAI_VIEC_MAC_DINH = "Yêu cầu mở thủ tục phá sản"` (Phá sản không có dropdown loại việc). |
| `ToaAnCatalog.java` | Whitelist cố định các tòa cấp tỉnh/thành dùng cho automation: `PREFERRED` (8 tên, Sơn La … Bắc Ninh), `MAX_AUTOMATION_COURTS = 10`. `filterForAutomation(scraped)` giữ lại các tên ưu tiên có trong danh sách quét được; `isProvinceOrCityLevel(name)` loại "khu vực" / "huyện" / "tòa án nhân dân tp.". |
| `UiMasterDataReader.java` | Scraper Selenium đọc thẳng card / dropdown / nhãn trên UI thật để tái sinh catalog. Các method `scrapeTaoDonStep1()`, `scrapeNguyenDonStep()`, `scrapeDongNguyenDonExpanded()`, `scrapeTuCachNopDonPhaSan()`, `scrapeBiDonStep()`, `scrapeThemBiDonExpanded()`, `scrapeAll()` sinh đúng bộ khoá sẽ được ghi vào `master-data.properties`. |

### 4.4 `flow/` — Điều phối luồng

| File | Chức năng |
|---|---|
| `TaoDonFlow.java` | Bộ điều phối wizard. Lớp `final` chỉ giữ `WebDriver` + `WebUI`, phơi một method cho mỗi bước (`dienBuoc1` … `dienBuoc5`) cùng các hành trình ghép: `denManXemLai(s)` (1→5 rồi dừng ở màn xem lại), `denHetBuoc3(s)`, `chayTheoMasterConfig(s)` (dừng sớm theo `untilStep`), `tuXemLaiQuaBuoc5DenXemLai(s)`, `tiepTucNopDonTuBuoc1SauChinhSua(s, yeuCauMoi)`. Bước 2 rẽ nhánh theo `DataDictionary.isToChuc`/`isPhaSan` và chỉ chạy bước xác minh lại tốn kém khi checkbox lưu định danh được tick (vì VNeID có thể prefill đè lên form). Method riêng `chuyenDenBuoc4GiuDuLieuSauChinhSua()` bấm "Tiếp theo" tối đa 5 lần để đi từ bước 1 về bước 4 **mà không điền lại** bước 2/3 (điền lại sẽ bỏ tick các checkbox đã chọn). |
| `StepBlockedException.java` | `RuntimeException` nghĩa là **chính hệ thống chặn việc chuyển bước** (validation phía server). Mang theo `stepNumber()`, `stepName()`, `systemMessage()`, `screenshotBase64()`; message dạng `❌ Bước N — <tên bước> — hệ thống báo lỗi: <message>`. Ném từ `WebUI` sau khi đã ghi log + chụp màn hình, nên `TestListener` nhận diện và **không** ghi thêm bản ghi fail thứ hai vào báo cáo. |
| `BrowserClosedException.java` | `RuntimeException` báo trình duyệt/tab đã bị đóng — kịch bản phải dừng ngay thay vì thử điều hướng lại. Message mặc định "Trình duyệt đã đóng — dừng kịch bản ngay." Được `TaoDonBaseTest` và `TestListener` xử lý như abort không thể retry, chỉ ảnh hưởng thread hiện tại. |

### 4.5 `pages/` — Page Object

| File | Chức năng |
|---|---|
| `LoginPage.java` | Màn đăng nhập. `openPage()`, `chonDangNhapBangTaiKhoan()`, `thucHienDangNhap(user, pass, manualCaptcha)` (đọc captcha qua `webUI.docCaptcha` khi không truyền tay), và `loginUntilDashboard(user, pass, maxAttempts, timeout)` — có retry: mở lại trang → thoát sớm nếu session cũ đã vào Dashboard → làm mới captcha từ lần thử thứ 2 → poll Dashboard mỗi 400 ms. |
| `DashboardPage.java` | Bọc mỏng nút "Nộp đơn mới" trên bảng điều khiển. `isDashboardVisible()`, `waitForDashboard(timeout)`, `clickNopDonMoi()`, và `ensureReady(loginPage, reLoginRunnable, timeout)` — tự đăng nhập lại nếu form login hiện ra (khôi phục session hết hạn). |
| `TaoDonPage.java` | **Bước 1**: chọn card loại đơn, dropdown loại việc, tòa án nhận đơn (dropdown có tìm kiếm), textarea tóm tắt. `boQuaNhapNeuCo()` bấm "Bắt đầu mới" để bỏ bản nháp cũ. Báo lỗi rõ ràng khi card loại đơn không tồn tại hoặc bị khoá (`opacity-70` = "Chỉ dành cho cơ quan"); bỏ qua dropdown loại việc với các loại đơn không có (`DataDictionary.hasLoaiViecDropdown`, ví dụ Phá sản); có retry F5 một lần khi frontend crash lúc chờ API catalog. |
| `NguyenDonPage.java` | **Bước 2**: thông tin nguyên đơn cho cả Cá nhân và Tổ chức. Xử lý địa chỉ thường trú / liên lạc, checkbox "địa chỉ liên lạc giống thường trú", checkbox đồng ý lưu thông tin định danh (`chonDongYLuuThongTinDinhDanh()` trả về việc có tick hay không — vì tick sẽ kích hoạt VNeID prefill ghi đè form), người đại diện, tư cách người nộp đơn (chỉ Phá sản), và **đồng nguyên đơn** (card con). Các method `dienDayDu*TruocTiepTheo` là lượt xác minh sau prefill, chỉ điền lại trường sai/trống. Locator dùng scope (`MAIN_SECTION`, `DONG_NGUYEN_DON_BLOCK`) để nhãn bước 2 không đụng nhãn bước 3. |
| `BiDonPage.java` | **Bước 3**: bị đơn / người bị kiện / cơ quan bị kiện, hỗ trợ 1..N bị đơn. `dienBuoc3(s)` chạy cả bước theo `s.soLuongBiDon()`; `ensureBiDonSlot(index, loaiDon)` / `clickXoaBiDon(index)` quản lý số card. Có nhánh riêng cho cơ quan hành chính (`dienThongTinNguoiBiKienHanhChinh`) và người liên quan. Việc đếm card chỉ tin vào **badge số chính xác** hoặc card "Tên cơ quan" ngoài cùng (`countConfirmedSlots`) vì card lồng nhau gây dương tính giả. |
| `NoiDungDonPage.java` | **Bước 4**: nội dung đơn — UI render theo **3 dạng khác nhau**, mô hình hoá bằng `enum Step4Mode`: (1) eform nhúng trong iframe `/f/…`, (2) upload file, (3) form textarea cũ. Với chế độ iframe: khớp theo gợi ý nhãn trước, sau đó quét điền mọi control bắt buộc/còn trống, ép input React commit giá trị trước khi host truy vấn iframe qua `postMessage`, bấm "Gửi ngay" trong iframe rồi chờ toast xác nhận của host, chuẩn hoá ngày về `yyyy-MM-dd`. Dừng ngay nếu eform báo "chưa xuất bản". `describeIframeFillGaps()` / `isIframeFullyFilled()` để flow biết còn thiếu trường nào. |
| `TaiLieuPage.java` | **Bước 5**: đính kèm tài liệu, chứng cứ. `uploadTaiLieuBatBuoc()` nhận diện dòng bắt buộc qua dấu `*` đỏ (`text-danger`); tiêu đề được thu thập trước rồi upload **theo tiêu đề chứ không theo chỉ số**, vì DOM re-render sau mỗi lần upload. Mặc định dùng file PDF sinh sẵn, chuyển sang PNG nếu thuộc tính `accept` chỉ nhận ảnh. |
| `XemLaiGuiDonPage.java` | **Bước 6**: xem lại và gửi. Gồm `clickChinhSua(...)` cùng các wrapper theo tên mục (`clickChinhSuaLoaiDon/NguyenDon/BiDon/Don/TaiLieu`), `xacNhanThongTin()`, `clickGuiDon()`, `thuGuiDonVaChoKetQua()` → `GuiDonKetQua`. Trước khi bấm Gửi đơn nó **chụp snapshot toast/notification hiện có làm baseline**, để chỉ thông báo **mới** mới được tính là kết quả. Cố tình tránh `getText()` trên card chứa iframe PDF (làm treo Chrome), dùng `existsNow` thay cho chờ có timeout ở chỗ đó. Lưu ý theo UI 2026: "Chỉnh sửa" trên card "Xem trước đơn" quay về **bước 1**, không phải bước 4. |
| `EformDropdownHelper.java` | Lớp `final` package-private (chỉ `NoiDungDonPage` dùng) điền các dropdown tuỳ biến dạng `button.inp` "— Chọn —" bên trong eform bằng JavaScript. Một entry point `static int fillPending(WebDriver, WebUI)` trả về số dropdown đã điền; lặp tối đa 10 vòng: đếm dropdown chưa chọn → snapshot các option đang thấy → mở dropdown → gõ "a" vào ô tìm kiếm nếu có → chọn option **mới** so với snapshot (thử lại ở default content khi menu render qua portal) → xác nhận text nút đã đổi; có fallback chọn bằng bàn phím và bỏ qua nếu giá trị vẫn không dính. |
| `GuiDonKetQua.java` | Value object bất biến cho kết quả bấm Gửi đơn: `enum TrangThai { SUCCESS, ERROR, TIMEOUT }` + thông báo hệ thống + ảnh chụp base64. Accessor `trangThai()`, `message()`, `screenshotBase64()`, `isSuccess()`, `isError()`, `isTimeout()`. |

### 4.6 `report/` — Báo cáo

| File | Chức năng |
|---|---|
| `TestListener.java` | TestNG `ITestListener` — bộ nối tất cả lại. `onStart`: đặt mốc lượt chạy, `ScreenshotStore.initRun()`, áp alias `RunFlowConfig`, in tóm tắt cấu hình, reset `ScenarioDispatch`, init Excel, set `taodon.suite`/`taodon.parallel`/`taodon.threads`. `onTestStart`: mở `TestActionLog`, dựng tiêu đề/mô tả từ scenario (hoặc tên tiếng Việt dễ hiểu cho test không data-driven). `onTestSuccess/Failure/Skipped`: `BaoCao.logStepSummary()` + `BaoCaoData.ketThucCase` + `recordFinished`; khi fail còn abort riêng thread nếu trình duyệt đã đóng, bỏ qua log trùng với `StepBlockedException`, chụp ảnh nếu lỗi chưa có ảnh, và rút gọn thông báo lỗi cho tester. `onFinish`: `LichSuKiemThu.append` (dựng lại `index.html`) rồi lưu file Excel. |
| `BaoCao.java` | Mặt tiền ghi log — **mọi** lời gọi báo cáo của cả dự án đi qua đây, 120 điểm gọi. Không giữ trạng thái nào ngoài mốc bắt đầu case và mã case (`ThreadLocal`); dữ liệu đẩy hết sang `BaoCaoData`. API: `createTest`/`clearTestContext`, `beginStepNode`/`endStepNode`, `markStepStart`/`logStepDone`, `logStepSummary`, `logScreenshots` (nhãn "Đầu/Giữa/Cuối biểu mẫu"), `logInfo/logPass/logWarning/logFail/logSkip` (+ biến thể kèm ảnh), `logThrowable`, `hasCurrentTest`, `wasFailScreenshotAttached`. Đây là `ExtentReportManager` cũ sau khi gỡ ExtentReports — javadoc của lớp ghi rõ **vì sao** gỡ, đọc trước khi có ý định mang thư viện báo cáo nào về. |
| `BaoCaoData.java` | Bộ thu thuần dữ liệu, không biết gì về HTML. Ba tầng record: `CaseBaoCao` → `BuocBaoCao` → `SuKien`/`HanhDong`, cộng `TomTatBuoc` (đủ 6 bước, phân biệt *Chưa chạy tới* với *Không hoàn thành*). Bước dở dang tự đo thời gian từ lúc mở, nên nhánh lỗi không cần nhớ gọi hàm nào. |
| `BaoCaoHtml.java` | Dựng `test-output/index.html` — một file tự chứa, không tài nguyên ngoài. Mỗi lượt lưu `test-output/runs/<mốc>/bao-cao.json` và trang được **dựng lại toàn bộ** từ các file đó, nên đổi giao diện không làm hỏng lịch sử cũ (`dungLaiTrang()` dựng lại mà không cần chạy test). Có xu hướng tỉ lệ đạt, lọc theo ngày, gom nhóm lỗi, đối chiếu với lượt trước (*mới hỏng / đã sửa / vẫn hỏng*), sáng/tối. |
| `ScreenshotStore.java` | Ghi ảnh ra `test-output/runs/<mốc>/screenshots/<mã case>/<NN>-<nhãn>.png`, thu nhỏ về ≤1600 px. Công tắc chung `-Dtaodon.screenshot=false`. |
| `TrangThai.java` | Các chuỗi trạng thái tiếng Việt (`Đạt`, `Thất bại`, `Bỏ qua`, `Không hoàn thành`, `Chưa chạy tới`). `BaoCaoHtml` chọn màu bằng cách so đầu chuỗi, nên gõ tay là mất màu. |
| `SuiteKind.java` | Bộ đang chạy (SMOKE/MID/FULL/LOGIN) và mã case ổn định `TC_<TAG>_007`. Mã phải ổn định giữa các lượt thì báo cáo mới đối chiếu được "mới hỏng / đã sửa" và liên kết sâu mới dán được. |
| `TestActionLog.java` | Từ vựng mô tả **những gì automation thực sự làm**: `dien`, `dienMask`, `dienLechGiaTri`, `chon`, `timKiemDropdown`, `taiLen`, `click`, `boQua`, `ghiChu`, `validation`. Chảy thẳng vào `BaoCaoData.hanhDong` — hiện thành bảng "N trường đã nhập" trong đúng bước. `pause()`/`resume()` loại phần đăng nhập ở `@BeforeClass` ra khỏi kịch bản đầu tiên. `firstTimeMismatch` chặn cảnh báo lệch giá trị lặp lại trong cùng case. |
| `TaoDonReportBuilder.java` | Helper định dạng text tiếng Việt: `asScenario(Object[] parameters)` (trích `TaoDonScenario` từ tham số DataProvider), `buildTestTitle`, `buildTestDescription`, `getLoaiDonCategory`, `getLoaiViecCategory`, `logScenarioOverview`, `tenBuocDayDu(int)` (**tên chuẩn của 6 bước**), `formatDuration(long)` (làm tròn TRƯỚC khi tách phút/giây — tách trước thì 179.5 giây in ra "2 phút 60 giây"). |

### 4.7 `ui/` — Tầng thao tác Selenium

| File | Chức năng |
|---|---|
| `WebUI.java` | **3.481 dòng** — lớp bọc Selenium cấp thấp mà mọi Page Object đều xây trên đó. Khoảng 90 method public, chia thành các nhóm: **(1) click & nhập** (`clickElement`, `clickElementOnceJs`, `setText`, `setTextForMaskedInput`, `uploadFile`); **(2) checkbox & toggle tuỳ biến** (`ensureCheckboxState`, `waitUntilCheckboxChecked`, `ensureCustomToggleSelected`, `clickChoiceChipIfNeeded`); **(3) kiểm tra hiện diện** (`isElementVisible`, `existsNow`, `countNow`, `waitUntilVisible/Invisible`); **(4) dropdown tuỳ biến** — không phải `<select>` (`selectCustomDropdown`, `selectDropdownWithSearch`, `selectToaAnWithCheck`, `dismissOpenDropdowns`) có chuẩn hoá text chịu được biến thể gạch ngang/khoảng trắng; **(5) khối địa chỉ hành chính** — cụm lớn nhất, giải quyết chuỗi tỉnh → phường/xã (dropdown xã chỉ nạp sau khi chọn tỉnh) cho nhiều khối địa chỉ lặp lại; **(6) thu thập toast & validation** (`collectToastMessages`, `collectValidationMessages`, `collectSystemFeedbackMessages`, `filterFeedbackNoise`) — hỗ trợ locator của Ant Design, Toastify và Sonner; **(7) chuyển bước & leo thang lỗi** (`waitForStepTransition`, `failStepWithSystemFeedback` → ném `StepBlockedException`); **(8) xử lý trình duyệt chết / frontend crash** (`isBrowserClosed`, `failIfBrowserClosed`, `recoverFromFrontendCrash`); **(9) chụp màn hình** — chỉ base64, **không ghi ra đĩa** (`takeOverviewScreenshot`, `takeScreenshotPreserveToast`, `takeContextScreenshots` chụp đầu/giữa/cuối form), tắt bằng `-Dtaodon.screenshot=false`; **(10) OCR captcha** (`docCaptcha` — đọc text HTML trước, chỉ khi rỗng mới chạy Tesseract với `tessdata/eng.traineddata`). Log tự thêm tiền tố `[C1]`/`[C2]` theo slot khi chạy song song. |
| `WaitConfig.java` | Lớp chỉ chứa hằng số, thay cho các timeout rải rác. Đơn vị giây: `FIELD=8`, `STEP=15`, `FORM=10`, `DASHBOARD=12`, `DASHBOARD_LOGIN=25`, `DROPDOWN=10`, `SUBMIT=60`, `HON_NHAN=15`, `WARD_READY=4`, `CATALOG_READY=12`, … Đơn vị ms cho thời gian "lắng": `SETTLE_SHORT_MS=120`, `SETTLE_MS=280`, `SETTLE_ADDRESS_MS=280`, `ADDRESS_BLOCK_GAP_MS=350`, `SETTLE_LONG_MS=700`. Một method `submitTimeoutSec()` ghi đè được bằng `-Dtaodon.submit.timeoutSec`. |
| `UiSynonyms.java` | Gom các danh sách **nhãn đồng nghĩa** để Page Object không hard-code một chuỗi tiếng Việt duy nhất: `HO_TEN`, `TEN_TO_CHUC`, `TU_CACH_NOP_DON`, `THEM_BI_DON`, `THEM_DONG_NGUYEN_DON`, `SLOT_BADGE_PREFIXES`. Dựng XPath từ chúng qua `containsAnyDot()`, `labelHoTenHoacToChuc()`, `buttonThemBiDonVariants()`, `anyThemButton()`, cùng `xpathLiteral()` xử lý chuỗi có cả 2 loại dấu nháy (tự dùng `concat()`). Javadoc của lớp ghi thứ tự ưu tiên locator của cả dự án. |
| `LoaiDonLocator.java` | Locator + bộ chuẩn hoá cho các card chọn loại đơn ở bước 1. `card(String loaiDon)` trả `By.xpath` trên `div[contains(@class,'cursor-pointer')]`, có nhánh riêng cho "Kinh doanh, thương mại" và "Dân sự" (khớp cả card bắt đầu bằng `DS `). `canonicalName(String cardText)` đưa text mô tả dài về đúng 1 trong 7 tên trong catalog, trả `null` cho card phụ đề/mô tả (dài > 45 ký tự hoặc chứa `...`/`…`). |
| `TestFileHelper.java` | Cung cấp file mẫu để upload ở bước "Tài liệu và chứng cứ". `getSamplePdf()`, `getSamplePng()` lấy từ classpath `testdata/`; `getSampleXlsx()`, `getSampleDocx()` ưu tiên classpath, nếu không có thì sinh lười vào `${java.io.tmpdir}/toaan-testdata/` bằng Apache POI (`XSSFWorkbook`/`XWPFDocument`, cache double-checked locking). `pickRandomUploadFile()` chọn ngẫu nhiên PDF/XLSX/DOCX; `displayName(path)` ánh xạ sang nhãn tiếng Việt trong báo cáo. |

### 4.8 `src/main/resources/`

| File | Chức năng |
|---|---|
| `master-data.properties` | Catalog dữ liệu chuẩn của hệ thống (xem [mục 6.6](#66-srcmainresourcesmaster-dataproperties)). Header ghi rõ `# Auto-generated by MasterDataSyncTest`. |
| `tessdata/eng.traineddata` | Dữ liệu huấn luyện Tesseract tiếng Anh (~23 MB) cho OCR captcha. Chỉ `WebUI.docCaptcha()` dùng tới; `WebUI.resolveTessDataPath()` tìm qua classpath, fallback về `${user.dir}/src/main/resources/tessdata`. |

---

## 6. Bộ test & suite XML

### 5.1 Test class

| File | Group | Cần Chrome? | Chức năng |
|---|---|---|---|
| `MasterExecutionTest.java` | `master`, `smoke`, `mid`, `full` | Có | **Điểm chạy trung tâm** (suite `master`), kế thừa `TaoDonBaseTest`, không gọi Selenium trực tiếp. DataProvider `DuLieuMaster` (parallel, trả `Object[][2]` — cột 1 `TaoDonScenario`, cột 2 `RunFlowConfig.CaseProfile` hoặc `null`): trả rỗng nếu `untilStep<=0`; dùng danh sách case từ **Google Sheet / `run.cases`** nếu có; nếu sheet đang bật mà không lấy được case nào thì **dừng kèm hướng dẫn kiểm tra** thay vì âm thầm chạy bộ smoke; ngược lại lấy dữ liệu smoke/mid/full theo tên suite, cắt bớt theo `run.browsers`. `testMasterExecution(TaoDonScenario s, CaseProfile caseProfile)` gắn `caseProfile` trực tiếp (không còn suy ngược từ `s.stt()`), giành quyền chạy qua `ScenarioDispatch`, gọi `TaoDonFlow.chayTheoMasterConfig(s)`, và **chỉ bấm Gửi đơn khi `RunFlowConfig.submit()` = true**. **Ca âm** (`caseProfile.hasNegativeExpectation()`): nếu flow chạy hết mà không bị chặn → FAIL ("lỗ hổng validation"); nếu bị `StepBlockedException` → `ghiNhanChanDungKyVong` so `ex.systemMessage()` với `thongBaoMongDoi()` (rỗng = chấp nhận mọi thông báo), khớp thì PASS, không khớp thì FAIL nêu rõ kỳ vọng vs thực tế — case bình thường (`caseProfile == null` hoặc không có kỳ vọng âm) giữ nguyên hành vi cũ (mọi block đều FAIL). |
| `TaoDonTest.java` | `smoke`, `mid`, `full`, `buoc23` | Có | Test luồng cổ điển (không qua master). `testFlowTaoDon(s)` chạy đủ 6 bước và **luôn gửi đơn**, fail cứng nếu có toast lỗi hệ thống. `testChinhSuaNoiDungTuXemLai()` kiểm tra Xem lại → Chỉnh sửa → gửi lại form → xác nhận text đã sửa (không gửi đơn). `testBuoc2Va3BayLoaiDon(s)` chạy bước 1→3 cho cả 7 loại đơn. Hỗ trợ lọc `-Dtaodon.onlyStt=12,13` để chạy lại riêng vài STT. |
| `LoginTest.java` | `login` | Có | Kiểm tra đăng nhập độc lập (kế thừa `BaseTest` nên mỗi method 1 Chrome mới, không tái dùng session). Đăng nhập rồi assert `DashboardPage.isDashboardVisible()`. Không nằm trong smoke. |
| `MasterDataSyncTest.java` | *(không group)* | Có | Quét lại `master-data.properties` từ UI dev/UAT thật. **Không được khai báo trong bất kỳ suite XML nào** — chỉ chạy được bằng `mvn test -Dtest=MasterDataSyncTest`. Gộp giá trị tĩnh mặc định với kết quả `UiMasterDataReader` scrape bước 1, bước 2 (Dân sự + tư cách Phá sản), bước 3 (Dân sự + Hành chính), rồi ghi file. Fail cứng nếu `loaiDon` hoặc `toaAn` rỗng. |
| `FullCoverageMatrixTest.java` | `unit` | **Không** | Unit test thuần: `FullCoverageMatrix.validateCoverage` phải rỗng, số dòng ≥ số cặp + 3, mọi cặp `(loaiDon, loaiViec)` và mọi tư cách Phá sản đều được phủ, có đủ nhánh CN/TC, 2 bị đơn, có đại diện, cả 2 giá trị người liên quan. |
| `MidCoverageMatrixTest.java` | `unit` | **Không** | Unit test thuần: ma trận mid phải đúng `TARGET_REGULAR (35) + số tư cách Phá sản (4) = 39` dòng, `validateCoverage` rỗng, mọi cặp không-Phá-sản được phủ, mọi dòng Phá sản có `soLuongBiDon == 1`. |
| `config/CaseSheetSourceTest.java` | `unit` | **Không** | Unit test bộ đọc Google Sheet — **không gọi mạng**, nạp CSV dạng chuỗi. 18 test: 12 test cột thường + 6 test riêng cho ca âm (đọc đúng `Trường lỗi`/`Giá trị lỗi`/`Thông báo mong đợi`, trống = case bình thường, để trống giá trị = cố tình bỏ trống field, alias tên cột linh hoạt). Nằm trong package `config` để dùng method package-private. |
| `FieldDiscoverySweepTest.java` | `discovery` (riêng, không nằm trong smoke/mid/full/master) | Có | **Bộ quét dò (Giai đoạn 1)** — tự động thử 13 field × ~2 biến thể sai (~28 dòng), **không assert pass/fail**, chỉ quan sát hệ thống có chặn hay không + thông báo gì, ghi ra `test-output/discovery-sweep/field-discovery_<timestamp>.csv`. Mỗi dòng: dựng baseline hợp lệ (`Dân sự`/CN, `Dân sự`/TC, hoặc `Phá sản` — xem hằng số `PHA_SAN` để biết vì sao cần riêng cho `Mã số thuế (Bị đơn)`) qua `DataGenerator.generateConfiguredCases`, ép 1 field sai qua `DataGenerator.tryFieldOverride`, điền hợp lệ các bước trước bước đích rồi thử bước đích trong `try/catch(StepBlockedException)`. Chạy: `mvn -Pdiscovery test`. Dùng để tìm field nào **thật sự** cần trở thành ca âm chính thức (điền vào cột `Trường lỗi` trên sheet) — xem mục 6.4. |

### 5.2 Suite XML — `src/test/resources/suites/`

| File | Tên suite | Nội dung | Song song |
|---|---|---|---|
| `testng.xml` | `ToaAn_Full_Suite` | `TaoDonTest#testFlowTaoDon` | Không |
| `testng-smoke.xml` | `ToaAn_Smoke_Suite` | `TaoDonTest#testFlowTaoDon` | Không |
| `testng-mid.xml` | `ToaAn_Mid_Suite` | `TaoDonTest#testFlowTaoDon` | Không |
| `testng-login.xml` | `ToaAn_Login_Suite` | `LoginTest` | Không |
| `testng-unit.xml` | `ToaAn_Unit_Suite` | `FullCoverageMatrixTest` + `MidCoverageMatrixTest` + `CaseSheetSourceTest`, group `unit` | Không |
| `testng-buoc23.xml` | `ToaAn_Buoc23_Suite` | `TaoDonTest#testBuoc2Va3BayLoaiDon`, group `buoc23` | Không |
| `testng-master.xml` | `ToaAn_Master_Suite` | `MasterExecutionTest`, group `master` | `methods`, thread-count 3 |
| `testng-parallel-smoke.xml` | `ToaAn_Smoke_Parallel_Suite` | `TaoDonTest#testFlowTaoDon`, group `smoke` | `methods`, thread-count 3 |
| `testng-parallel-mid.xml` | `ToaAn_Mid_Parallel_Suite` | `TaoDonTest#testFlowTaoDon`, group `mid` | `methods`, thread-count 3 |
| `testng-parallel-full.xml` | `ToaAn_Full_Parallel_Suite` | `TaoDonTest#testFlowTaoDon`, group `full` | `methods`, thread-count 3 |
| `testng-parallel-buoc23.xml` | `ToaAn_Buoc23_Parallel_Suite` | `TaoDonTest#testBuoc2Va3BayLoaiDon`, group `buoc23` | `methods`, thread-count 3 |
| `testng-discovery.xml` | `ToaAn_Discovery_Suite` | `FieldDiscoverySweepTest`, group `discovery` | Không |

> Giá trị `thread-count="3"` trong XML **chỉ là mặc định**. `ParallelSuiteAdjuster` ghi đè bằng
> `run.browsers` lúc nạp suite.

### 5.3 `src/test/resources/testdata/`

| File | Chức năng |
|---|---|
| `sample.pdf` | File PDF mẫu, mặc định dùng để upload ở bước 5 và bước 4 (chế độ upload). |
| `sample.png` | File ảnh mẫu, dùng khi ô upload chỉ chấp nhận `image/*`. |

---

## 7. File cấu hình

### 6.1 Thứ tự ưu tiên

`RunFlowConfig.raw(key)` tra theo thứ tự **giảm dần**:

```
1. System property đúng tên     -Drun.browsers=2
2. Alias system property        -Dtaodon.threads=2
3. Biến môi trường              TOAAN_RUN_BROWSERS=2
4. run-flow.properties          run.browsers=2
5. Giá trị mặc định trong code
```

`ConfigReader.getValue(key)` (dành cho credential) dùng thứ tự khác: **env → system property → file**.

### 6.2 `src/test/resources/run-flow.properties` — bảng điều khiển chính

Đây là file duy nhất cần sửa để đổi cách chạy. Được đọc **hai lần độc lập**: bởi `RunFlowConfig`
(Java) và bởi `scripts/run-flow.ps1` (PowerShell, parse text thuần).

| Khoá | Giá trị hợp lệ | Mặc định | Ý nghĩa |
|---|---|---|---|
| `run.suite` | `smoke` \| `mid` \| `full` \| `master` \| `login` \| `buoc23` \| `unit` | `smoke` | Chọn bộ test. Giá trị khác → script in lỗi và `exit 1`. |
| `run.browsers` | số nguyên, kẹp **1–8** | `1` | Số Chrome chạy cùng lúc. Nếu có `run.cases` thì còn bị kẹp thêm ≤ số case. |
| `run.parallel` | `true`/`yes`/`1`/`on` = bật | `false` | Tự ép `false` khi `browsers <= 1`; tự ép `true` khi `run.cases` hoặc `run.slots` khác rỗng. |
| `run.untilStep` | `login` \| `0`–`6` | `6` | Độ sâu chung — dừng sau bước thứ mấy. `login`/`0` = chỉ đăng nhập. Ngoài khoảng → kẹp về 0–6; không parse được → `6` kèm cảnh báo. Bị **bỏ qua** khi case có độ sâu riêng. |
| `run.submit` | `true`/`yes`/`1`/`on` | `false` | Có bấm "Gửi đơn" hay không. `submit()` chỉ trả `true` khi cờ bật **và** `untilStep >= 6`. |
| `run.caseSource` | `sheet` \| `file` | `sheet` | Nguồn danh sách case. `sheet` = đọc `run.casesSheet`; `file` = đọc `run.cases`. Xem [6.4](#64-lấy-test-case-từ-google-sheet-runcasessheet). |
| `run.casesSheet` | URL Google Sheet (hoặc ID thuần) | *(link sheet `testcase`)* | Sheet chứa danh sách case. Khác rỗng + `caseSource=sheet` → suite tự chuyển thành `master`. |
| `run.casesSheetGid` | số (gid của tab) | *(rỗng)* | Tab cụ thể trong sheet. Rỗng = tab đầu tiên. Nếu link đã có `#gid=` thì khoá này **thắng**. |
| `run.cases` | grammar ở [6.3](#63-grammar-của-runcases) | *(rỗng)* | Danh sách case + độ sâu riêng cho từng case (wizard menu 2 sinh ra). Dùng khi `caseSource=file`, hoặc làm fallback khi sheet lỗi và chưa có cache. Khác rỗng → suite tự chuyển thành `master`. |
| `run.slots` | `<bước>[:submit]` nối bằng `\|`, ví dụ `3\|6:submit\|6` | *(rỗng)* | **Legacy, hiếm dùng.** Gán độ sâu theo từng Chrome (index từ `BrowserSlot`). Cờ submit bị ép `false` khi bước < 6. |
| `run.window.width` | px (≤ 200 coi như bỏ trống) | `520` | Chiều rộng ô cửa sổ khi chạy song song. |
| `run.window.height` | px (≤ 200 coi như bỏ trống) | `580` | Chiều cao ô cửa sổ. |
| `run.window.scale` | số thực; áp dụng khi 0 < scale < 1 | `0.55` | Hệ số thu nhỏ khi không đặt kích thước cố định (chiều cao bị kẹp 0.45–0.85). |
| `run.openReport` | `true`/`false` | `true` | Tự mở `test-output/index.html` sau khi chạy xong (`BaseTest.afterSuite`). |
| `run.requireSubmit` | `true`/`false` | `false` (code) / `true` (file hiện tại) | Độ chặt của assert liên quan tới việc gửi đơn; ánh xạ sang `-Dtaodon.requireSubmit`. |

### 6.3 Grammar của `run.cases`

> Chỉ dùng khi `run.caseSource=file`. Nguồn mặc định hiện nay là Google Sheet — xem [6.4](#64-lấy-test-case-từ-google-sheet-runcasessheet).
> Các trục dữ liệu mở rộng (tòa án, số bị đơn, đồng nguyên đơn, đại diện, người liên quan,
> tài liệu bổ sung) **chỉ có trên sheet**; `run.cases` để automation tự chọn như trước.

Comment trong file ghi:

```
Format: Loại đơn>Loại việc>CN|TC>tư cách|->until[:submit]
```

⚠️ **Dễ nhầm:** dấu `|` trong `CN|TC` và `tư cách|-` chỉ có nghĩa "**hoặc**" trong phần mô tả.
Dấu `|` **thật sự** là ký tự **ngăn cách giữa các case**.

Quy tắc parse (`RunFlowConfig.parseCases` → `parseOneCase`):

1. Tách cả chuỗi bằng `|` → mỗi mảnh là **một case** (mảnh rỗng bị bỏ qua).
2. Tách mỗi mảnh bằng `>` → **phải đủ 5 trường**, thiếu sẽ in
   `⚠ run.cases token thiếu trường (cần 5 phần)` và **bỏ case đó**.

| # | Trường | Giá trị | Ghi chú |
|---|---|---|---|
| 1 | Loại đơn | `Dân sự`, `Phá sản`, `Hôn nhân và gia đình`… | Khớp mờ (không phân biệt hoa thường, khớp chuỗi con) với `MasterDataCatalog.getLoaiDon()`. Để trống → lỗi. |
| 2 | Loại việc | `Hợp đồng dân sự`, … hoặc `-` | Khớp với danh sách loại việc của loại đơn đó. Trống hoặc `-` → lấy loại việc đầu tiên. Với Phá sản luôn bị thay bằng `Yêu cầu mở thủ tục phá sản`. |
| 3 | `CN` \| `TC` | Chủ thể nguyên đơn | `TC` / chứa "tổ chức" / "doanh nghiệp" → `Tổ chức / Doanh nghiệp`; `CN` / "cá nhân" → `Cá nhân`; trống → `Cá nhân`. |
| 4 | Tư cách nộp đơn | `Chủ nợ`, `Người lao động`, `DN / HTX tự nộp`, `Cổ đông – thành viên HTX`, hoặc `-` | **Chỉ dùng cho Phá sản.** `-` hoặc trống → để hệ thống tự chọn theo seed. |
| 5 | `until[:submit]` | `3`, `6`, `6:submit`, `login` | Độ sâu riêng của case. `login`/`0`/trống → 0; số ngoài khoảng bị kẹp 0–6; không parse được → 6. Phần sau `:` nhận `submit`/`true`/`yes`/`gui`/`1`/`on`. **Submit bị ép `false` nếu bước < 6.** |

**Ví dụ:**

```properties
run.cases=Dân sự>Hợp đồng dân sự>CN>->3|Phá sản>Yêu cầu mở thủ tục phá sản>TC>Chủ nợ>6:submit
```

- **Case 1** — đơn Dân sự / Hợp đồng dân sự, nguyên đơn Cá nhân, không tư cách, **dừng sau bước 3**.
- **Case 2** — đơn Phá sản, nguyên đơn Tổ chức, tư cách "Chủ nợ", chạy đủ 6 bước và **bấm Gửi đơn**.

**Hai lưu ý quan trọng:**

- Số Chrome (`run.browsers`) **không bắt buộc bằng** số case — TestNG xếp hàng chờ.
  Ví dụ `run.browsers=2` với 4 case là hợp lệ.
- `run.cases` **không được truyền qua `-D`** vì ký tự `>` làm vỡ shell.
  Java đọc thẳng từ `run-flow.properties` (hoặc biến môi trường `TOAAN_RUN_CASES`).
  `run-flow.ps1` cũng in dòng nhắc `(cases lấy từ run-flow.properties — không truyền -D)`.

### 6.4 Lấy test case từ Google Sheet (`run.casesSheet`)

Đây là **nguồn test case mặc định** hiện nay: thay vì gõ `run.cases` trong file properties,
tester điền case thành từng dòng trên một Google Sheet. Java tải sheet dạng CSV lúc chạy
(`CaseSheetSource`) nên **không cần API key, không cần đăng nhập** — chỉ cần chia sẻ sheet ở chế độ
**"Bất kỳ ai có đường liên kết — Người xem"**.

Sheet đang dùng: [`testcase`](https://docs.google.com/spreadsheets/d/1ClZ9FJXn9lrCqxqTsTDoP5wlA7ajFQ7o0yyfL1J2KJU/edit)

#### Bố cục sheet

Dòng đầu tiên là **tên cột** (các dòng tiêu đề trang trí phía trên được bỏ qua — bộ đọc tìm dòng nào
có cột "Loại đơn"). Tên cột **không phân biệt hoa thường / dấu / thứ tự**, cột lạ bị bỏ qua.

| Cột | Bắt buộc | Giá trị | Bỏ trống nghĩa là |
|---|---|---|---|
| `Chạy` | Không | `x` / `Có` / `1` = chạy | **Bỏ qua dòng đó.** Nếu sheet không có cột này thì mọi dòng đều chạy |
| `Loại đơn` | **Có** | `Dân sự`, `Phá sản`, `Hôn nhân và gia đình`… (khớp mờ với catalog) | Bỏ qua dòng (in cảnh báo) |
| `Loại việc` | Không | Loại việc của loại đơn đó (khớp mờ) | Lấy loại việc đầu tiên; Phá sản luôn là `Yêu cầu mở thủ tục phá sản` |
| `Chủ thể` | Không | `CN` / `TC` (hoặc `Cá nhân` / `Tổ chức`) | `Cá nhân` |
| `Tư cách` | Không | `Chủ nợ`, `Người lao động`, `DN / HTX tự nộp`, `Cổ đông – thành viên HTX` | Tự chọn theo seed. **Chỉ dùng cho Phá sản** |
| `Tòa án` | Không | Gõ ngắn cũng được: `Sơn La`, `Huế`, `Bắc Ninh` | Xoay vòng theo thứ tự dòng |
| `Số bị đơn` | Không | `1` hoặc `2` | `1`. Loại đơn/việc không cho thêm bị đơn (Phá sản, thuận tình ly hôn) bị **ép về 1** kèm log |
| `Đồng NĐ` | Không | `Có` / `Không` | `Không`. Loại đơn không hỗ trợ → luôn `Không` |
| `Đại diện` | Không | `Có` / `Không` | `Không`. Nguyên đơn là tổ chức → luôn `Không` |
| `Liên quan` | Không | `Có` / `Không` | `Không` |
| `TL bổ sung` | Không | `Có` / `Không` | `Không` |
| `Đến bước` | Không | `0`/`login` … `6`, hoặc gộp `6:submit` | `6` (đến màn Xem lại) |
| `Gửi đơn` | Không | `x` = bấm Gửi đơn thật | Không gửi. **Bị ép tắt nếu `Đến bước` < 6** |
| `Trường lỗi` | Không | 1 trong 10 giá trị — xem bảng dưới | **Case bình thường** (không phải ca âm) |
| `Giá trị lỗi` | Không | Giá trị sai muốn điền vào `Trường lỗi` | Cố tình **để trống** field đó (chỉ có ý nghĩa khi `Trường lỗi` đã chọn) |
| `Thông báo mong đợi` | Không | Chuỗi con phải có trong thông báo hệ thống | Chấp nhận **bất kỳ** thông báo chặn nào (chỉ cần bị chặn là đạt) |
| `Ghi chú` | Không | Text tự do — in ra log để dễ lần | — |

Ví dụ 2 dòng đầu:

| Chạy | Loại đơn | Loại việc | Chủ thể | Tư cách | Tòa án | Số bị đơn | Đồng NĐ | Đại diện | Liên quan | TL bổ sung | Đến bước | Gửi đơn | Ghi chú |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| x | Dân sự | Hợp đồng dân sự | TC | | Sơn La | 2 | Có | Không | Có | Có | 6 | | case đầy đủ |
| x | Phá sản | | TC | Chủ nợ | Bắc Ninh | 1 | | | | | 6 | x | gửi đơn thật |

#### Ca âm — kiểm tra hệ thống có chặn đúng validation không

Mặc định automation chỉ điền dữ liệu **hợp lệ** rồi bấm Tiếp theo (click-through) — không phát
hiện được lỗ hổng validation phía server (vd. SĐT sai định dạng vẫn được chấp nhận). 3 cột
`Trường lỗi` / `Giá trị lỗi` / `Thông báo mong đợi` khai báo một **ca âm**: cố tình điền sai 1 field,
rồi kiểm tra hệ thống có **chặn đúng** hay không.

- `Trường lỗi` trống → case chạy bình thường như trước, 2 cột kia bị bỏ qua.
- `Trường lỗi` có giá trị, `Giá trị lỗi` trống → cố tình **để trống** field đó (test trường bắt buộc).
- Kết quả: **PASS** khi hệ thống chặn và thông báo chứa `Thông báo mong đợi` (hoặc bất kỳ thông báo
  nào nếu cột này để trống); **FAIL** khi hệ thống **không chặn** (lỗ hổng validation) hoặc chặn
  **sai thông báo**.

13 giá trị hợp lệ cho `Trường lỗi` (`DataGenerator.tryFieldOverride`) — 3 field cuối (Ngày sinh/Ngày
cấp/Giá trị tranh chấp) mới thêm ở Giai đoạn 1 dò field, dropdown "Trường lỗi" trên sheet hiện chưa
liệt kê, gõ tay vẫn nhận được:

| Trường lỗi | Áp dụng khi |
|---|---|
| `Số điện thoại (Nguyên đơn)` | Luôn áp dụng |
| `Email (Nguyên đơn)` | Luôn áp dụng |
| `CCCD (Nguyên đơn)` | Nguyên đơn là **Cá nhân** |
| `Họ tên (Nguyên đơn)` | Nguyên đơn là **Cá nhân** |
| `Mã số thuế (Nguyên đơn)` | Nguyên đơn là **Tổ chức** |
| `Số điện thoại (Bị đơn)` | Luôn áp dụng |
| `Email (Bị đơn)` | Trừ loại đơn **Hành chính** (form cơ quan không có ô Email) |
| `CCCD (Bị đơn)` | Bị đơn Cá nhân, và loại đơn không phải Hành chính/Phá sản |
| `Họ tên (Bị đơn)` | Bị đơn Cá nhân, và loại đơn không phải Hành chính/Phá sản |
| `Mã số thuế (Bị đơn)` | Bị đơn Tổ chức, hoặc loại đơn Phá sản (bị đơn luôn là Tổ chức) |
| `Ngày sinh (Nguyên đơn)` | Nguyên đơn là **Cá nhân** (người đại diện Tổ chức cũng có ô này nhưng chỉ hiện có điều kiện trên UI — chưa hỗ trợ) |
| `Ngày cấp CCCD (Nguyên đơn)` | Nguyên đơn là **Cá nhân** (lý do như trên) |
| `Giá trị tranh chấp` | Loại đơn có ô này (`DataDictionary.hasGiaTriTranhChap` — Dân sự, Kinh doanh thương mại, …) |

Chọn sai ngữ cảnh (vd. "Mã số thuế" cho nguyên đơn Cá nhân) → override bị **bỏ qua** kèm cảnh báo
console, nhưng case vẫn được coi là ca âm nên sẽ **FAIL** với thông báo "kỳ vọng bị chặn nhưng
không xảy ra" — đọc log console để biết lý do thực sự (chọn sai `Trường lỗi`, không phải bug hệ
thống).

Ví dụ 1 dòng ca âm — để trống SĐT, kỳ vọng hệ thống báo bắt buộc:

| Chạy | Loại đơn | Loại việc | Chủ thể | Đến bước | Trường lỗi | Giá trị lỗi | Thông báo mong đợi |
|---|---|---|---|---|---|---|---|
| x | Dân sự | Hợp đồng dân sự | CN | 2 | Số điện thoại (Nguyên đơn) | *(để trống)* | Số điện thoại |

#### Bộ quét dò (Giai đoạn 1) — tìm field nào cần trở thành ca âm

Gõ tay từng dòng ca âm không scale khi hệ thống có hàng trăm ô nhập. `FieldDiscoverySweepTest`
(mục 5.1) tự động thử 13 field × ~2 biến thể sai (~28 lượt), **không assert pass/fail** — chỉ quan
sát hệ thống có chặn hay không và thông báo gì, ghi ra
`test-output/discovery-sweep/field-discovery_<timestamp>.csv`.

```bash
mvn -Pdiscovery test   # ~15-25 phút, cần Chrome + đăng nhập thật
```

Đọc file CSV kết quả, với mỗi dòng "Bị chặn: Không" — tự quyết đó là lỗ hổng validation thật hay
field cố ý không có luật — rồi mới điền `Trường lỗi`/`Thông báo mong đợi` chính thức vào sheet
(biến thành ca âm thật, có assert). Đây là bước **dò**, không phải bước **biết đúng sai** — xem
`FieldDiscoverySweepTest`'s javadoc để biết đầy đủ giới hạn (chưa quét field địa chỉ do dropdown
tỉnh/phường lồng nhau, chưa quét CCCD/ngày sinh của người đại diện Tổ chức).

#### Chọn nguồn case

| `run.caseSource` | Nguồn |
|---|---|
| `sheet` *(mặc định khi `run.casesSheet` khác rỗng)* | Google Sheet |
| `file` | `run.cases` trong `run-flow.properties` |

Menu `chay.cmd` tự đặt khoá này: mục **1** (Chạy theo Google Sheet) đặt `sheet`;
wizard và các preset smoke/mid/full/login đặt `file` để sheet không chiếm quyền.

#### Khi không tải được sheet

```
1. Tải sheet OK        → dùng sheet, ghi cache test-output/cases-sheet-cache.csv
2. Lỗi mạng, có cache  → ⚠ dùng cache (in kèm thời điểm tải)
3. Lỗi mạng, ko cache  → ⚠ quay về run.cases trong run-flow.properties
4. Không có case nào   → ❌ MasterExecutionTest dừng kèm hướng dẫn kiểm tra
```

Sheet **không bao giờ làm suite crash lúc khởi động** — mọi lỗi tải được chuyển thành cảnh báo,
chỉ khi thực sự không còn case nào để chạy thì DataProvider mới báo lỗi rõ ràng.

Số Chrome vẫn lấy từ `run.browsers` (kẹp ≤ số case trên sheet) — sheet có 20 case vẫn chạy được
với 3 Chrome, TestNG xếp hàng.

### 6.5 `src/test/resources/config.example.properties`

Mẫu credential — copy thành `config.properties` (**đã nằm trong `.gitignore`**).

| Khoá | Biến môi trường | `-D` | Nơi dùng |
|---|---|---|---|
| `username` | `TOAAN_USERNAME` | `-Dusername` | `TaoDonBaseTest.performLogin`, `LoginTest`, `MasterDataSyncTest` |
| `password` | `TOAAN_PASSWORD` | `-Dpassword` | như trên |
| `baseUrl` | `TOAAN_BASE_URL` | `-DbaseUrl` | `LoginPage.openPage`, `MasterDataSyncTest`, hiển thị trong báo cáo |

Giá trị mặc định của `baseUrl`: `https://demo-dichvutuphap.gsfpt.com/`

### 6.6 `src/main/resources/master-data.properties`

Catalog dữ liệu chuẩn — **file này do `MasterDataSyncTest` sinh ra**, không sửa tay trừ khi cần.
Giá trị nhiều lựa chọn ngăn bằng `|`. Người đọc duy nhất là `MasterDataCatalog.loadCatalog()`.

| Khoá | Nội dung | Nơi dùng |
|---|---|---|
| `loaiDon` | 7 loại đơn | `DataGenerator` (mọi ma trận), `DataDictionary`, menu `chay.ps1` |
| `loaiDonViecPairs` | 35 cặp; **`;` ngăn cặp, `>` ngăn loại đơn với loại việc** | `getAllLoaiDonViecPairs()`, `FullCoverageMatrix`, `MidCoverageMatrix`, menu |
| `toaAn` | 8 tòa cấp tỉnh/thành | `getToaAn()` — lọc qua `ToaAnCatalog.filterForAutomation` ngay khi nạp |
| `loaiChuTheNguyenDon` | `Cá nhân\|Tổ chức / Doanh nghiệp` | `DataGenerator.resolveChuTheNguyenDon` |
| `loaiChuTheBiDon` | `Cơ quan` | `getLoaiChuTheBiDon()` |
| `loaiHinhToChuc` | Công ty TNHH / cổ phần / DNTN / HTX / Khác | Cho cả nguyên đơn và bị đơn tổ chức |
| `gioiTinh` | `Nam\|Nữ\|Khác` | `getGioiTinh()` |
| `noiCapCccd` | 2 nơi cấp | `getNoiCapCccd()` |
| `coKhong` | `Có\|Không` | `getCoKhong()` |
| `quanHeDaiDien` | `Luật sư\|Người thân\|Khác` | `getQuanHeDaiDien()` |
| `loaiChuTheDongNguyenDon`, `loaiHinhToChucBiDon`, `labels.dongNguyenDon`, `labels.themBiDon` | Snapshot nhãn UI | **Chỉ được ghi bởi sync test**, không có getter nào đọc — mang tính chẩn đoán/đối chiếu |
| `tuCachNopDonPhaSan` | 4 tư cách | ⚠️ **Hiện chưa có trong file** → `MasterDataCatalog.getTuCachNopDonPhaSan()` dùng fallback cứng trong code: `Chủ nợ`, `Người lao động`, `DN / HTX tự nộp`, `Cổ đông – thành viên HTX` |

Ngoài ra `MasterDataCatalog` còn hỗ trợ dạng khoá `loaiViec.<Loại đơn>` như nguồn cặp dự phòng,
nhưng file hiện tại dùng dạng một dòng `loaiDonViecPairs`.

### 6.7 Ánh xạ `run.suite` → Maven profile → suite XML

Logic ở `scripts/run-flow.ps1`; các profile định nghĩa trong `pom.xml`.

| `run.suite` | Không song song | Song song (`parallel=true` và `browsers>1`) | Suite XML |
|---|---|---|---|
| `smoke` | `-Psmoke` | `-Pparallel-smoke` | `testng-smoke.xml` / `testng-parallel-smoke.xml` |
| `mid` | `-Pmid` | `-Pparallel-mid` | `testng-mid.xml` / `testng-parallel-mid.xml` |
| `full` | `-Pfull` | `-Pparallel-full` | `testng.xml` / `testng-parallel-full.xml` |
| `buoc23` | *(không profile)* `-DsuiteXmlFile=…/testng-buoc23.xml` | `-Pparallel-buoc23` | `testng-buoc23.xml` / `testng-parallel-buoc23.xml` |
| `login` | `-Plogin` | `-Plogin` | `testng-login.xml` |
| `unit` | `-Punit` | `-Punit` | `testng-unit.xml` |
| `master` | `-Pmaster` | `-Pmaster` | `testng-master.xml` |
| *(khác)* | `LỖI: run.suite='…' không hợp lệ.` → `exit 1` | | |

`run.casesSheet` khác rỗng (và `run.caseSource` ≠ `file`), hoặc `run.cases` khác rỗng,
sẽ **ghi đè `$suite = 'master'`** trước khi ánh xạ. Script **không** tự tải sheet — nó chỉ chọn
profile và giữ `run.browsers`; Java tải sheet lúc chạy rồi tự kẹp số Chrome ≤ số case.

### 6.8 `pom.xml`

- **Properties mặc định**: `suiteXmlFile` = `testng.xml`, `taodon.suite=full`, `taodon.threads=1`,
  `taodon.parallel=false`, `taodon.untilStep=6`, `taodon.submit=false`.
- **maven-surefire-plugin 3.2.5**: nạp `${suiteXmlFile}`, đăng ký listener
  `vn.tuphap.automation.core.ParallelSuiteAdjuster` (**chỉ ở đây, không khai lại trong XML**),
  và chuyển tiếp `systemPropertyVariables`: `taodon.suite`, `taodon.threads`, `taodon.parallel`,
  `taodon.onlyStt`, `taodon.untilStep`, `taodon.submit`.
- **maven-compiler-plugin 3.13.0**: `release=17`, `encoding=UTF-8`.
- **12 profile**: `smoke`, `mid`, `full`, `login`, `unit`, `master`, `discovery`,
  `parallel-smoke`, `parallel-mid`, `parallel-full`, `parallel-buoc23` — mỗi profile đặt
  `suiteXmlFile` + `taodon.suite` (bản parallel đặt thêm `taodon.threads=3`, `taodon.parallel=true`).

### 6.9 Các `-D` và biến môi trường hữu ích

| Cờ | Tác dụng |
|---|---|
| `-Dtaodon.onlyStt=12,13` | Chỉ chạy các STT chỉ định trong `TaoDonTest` (env: `TOAAN_ONLY_STT`). Tách bằng `,` `;` hoặc khoảng trắng; không khớp gì → ném lỗi. |
| `-Dtaodon.screenshot=false` | Tắt toàn bộ chụp màn hình (chỉ giá trị đúng chữ `false` mới tắt). |
| `-Dtaodon.submit.timeoutSec=90` | Ghi đè timeout chờ kết quả gửi đơn (tối thiểu 3, mặc định `WaitConfig.SUBMIT=60`). |
| `-Dtaodon.suite=mid` | Ép loại suite khi tên suite XML không nói lên điều đó (ảnh hưởng cả tiền tố mã case `TC_<TAG>_NNN`). |
| `-Dtest=MasterDataSyncTest` | Chạy riêng test quét master data. |
| `-Drun.casesSheet=<url>` | Chạy theo một sheet khác mà không sửa file (URL truyền `-D` an toàn, không có ký tự vỡ shell). |
| `-Drun.caseSource=file` | Bỏ qua sheet, dùng `run.cases` trong `run-flow.properties`. |
| `TOAAN_USERNAME`, `TOAAN_PASSWORD`, `TOAAN_BASE_URL` | Credential — **ưu tiên hơn** `config.properties`. Hợp cho CI. |
| `TOAAN_RUN_CASESSHEET`, `TOAAN_RUN_CASESOURCE`, `TOAAN_RUN_CASES`, `TOAAN_RUN_BROWSERS`, … | Ghi đè khoá `run.*` tương ứng. Quy tắc `toEnvKey`: thay `.` bằng `_` rồi in hoa — nên `run.casesSheet` → `TOAAN_RUN_CASESSHEET` (không có gạch dưới giữa `CASES` và `SHEET`). |

---

## 8. Báo cáo đầu ra

Toàn bộ nằm trong `test-output/` — thư mục này **đã có trong `.gitignore`**.

| Đường dẫn | Nội dung |
|---|---|
| `test-output/index.html` | **Báo cáo chính.** Một file tự chứa, tiếng Việt, sáng/tối. Ba tầng gập: lượt chạy → kịch bản → bước, kèm ảnh chụp bấm phóng to được, dữ liệu từng trường đã nhập, dải tiến độ 6 bước, stack trace. Đầu trang có KPI, xu hướng tỉ lệ đạt qua các lượt, bảng gom nhóm lỗi, và dòng so với lượt trước. Lọc theo ngày / bộ / trạng thái. Tự mở sau khi chạy nếu `run.openReport=true`. |
| `test-output/runs/<mốc>/bao-cao.json` | Dữ liệu thô của một lượt chạy. `index.html` được **dựng lại toàn bộ** từ tất cả các file này, nên xoá `index.html` không mất gì — chạy `BaoCaoHtml.dungLaiTrang()` là có lại. Xoá thư mục `runs/<mốc>/` mới là xoá thật lượt chạy đó. |
| `test-output/runs/<mốc>/screenshots/<mã case>/` | Ảnh PNG theo case, đánh số theo thứ tự chụp. Báo cáo chỉ trỏ `<img src>` tới đây, không nhúng. |
| `test-output/runs/run_<bộ>_<mốc>.log` | Log Maven đầy đủ của từng lượt, giữ lại không ghi đè. |
| `test-output/testlogs_*/`<br>`test-output/LichSuKiemThu.xlsx` | **Không còn sinh ra.** Phần xuất Excel đã gỡ — mọi thứ nó từng có (dữ liệu từng trường, độ phủ, kết quả mong đợi/thực tế) nay nằm trong `index.html`. File cũ vẫn để nguyên làm hồ sơ; xoá được nếu không cần. |
| `test-output/last-run.log` | Log Maven đầy đủ của lần chạy gần nhất, do `run-flow.ps1` ghi (**ghi đè mỗi lần**). Có header tóm tắt cấu hình (kèm link sheet nếu đang chạy theo sheet) và footer `exit code`. Console cố ý không in log chi tiết. |
| `test-output/cases-sheet-cache.csv` | Bản CSV thô của lần tải Google Sheet gần nhất **có ít nhất 1 case**, do `CaseSheetSource` ghi (sheet trống / sai tên cột không ghi đè cache tốt). Dùng làm dự phòng khi mất mạng / sheet đổi quyền. Xoá file này là an toàn — lần chạy sau tải lại từ sheet. |

**Ảnh chụp được ghi ra đĩa** qua `ScreenshotStore` (PNG, thu nhỏ về ≤1600 px) và báo cáo chỉ trỏ
`<img src>` tới file. Nhúng base64 thẳng vào HTML — cách làm cũ — khiến file báo cáo phình lên
16.8 MB cho một lượt 39 case, mở rất chậm, và không thể dẫn link tới ảnh vì trên đĩa không
có file nào. Tắt hẳn việc chụp bằng `-Dtaodon.screenshot=false` nếu muốn chạy nhanh hơn.

---

## 9. Scripts

Thư mục `scripts/` gồm 2 cặp `.cmd` + `.ps1`. File `.cmd` chỉ là wrapper: đặt `chcp 65001`
(UTF-8 cho tiếng Việt trong console) và gọi PowerShell với `-ExecutionPolicy Bypass`
— **không đổi cấu hình Windows của máy**.

| File | Chức năng |
|---|---|
| `chay.cmd` | Wrapper cho `chay.ps1`. Đây là **điểm vào khuyến nghị**. |
| `chay.ps1` | Menu tương tác 593 dòng. Tự phát hiện terminal không hỗ trợ ↑↓ (ví dụ chạy trong IntelliJ) và **tự mở cửa sổ CMD mới**. 7 preset: (1) chạy theo Google Sheet, (2) wizard cấu hình case, (3) smoke 1 Chrome, (4) smoke 3 Chrome, (5) chỉ đăng nhập, (6) mid 3 Chrome, (7) full 3 Chrome, (8) xem cấu hình không chạy. Mục 1 (`Apply-SheetSource`) chỉ hỏi số Chrome rồi đặt `run.caseSource=sheet` + `run.suite=master` — danh sách case nằm trên sheet. Wizard mục 2 đọc `master-data.properties` để hiện đúng danh sách loại đơn / loại việc / tư cách, rồi **ghi ngược `run.cases`, `run.browsers` và `run.caseSource=file` vào `run-flow.properties`** trước khi gọi `run-flow.ps1`. Mọi preset khác cũng đặt `run.caseSource=file` để sheet không chiếm quyền. |
| `run-flow.cmd` | Wrapper cho `run-flow.ps1`. Dùng khi đã sửa tay `run-flow.properties`. |
| `run-flow.ps1` | Bộ chạy 300 dòng. Đọc `run-flow.properties` (hàm `Get-PropValue` bỏ comment sau `#`, lấy dòng khớp **cuối cùng**), ép UTF-8 cho JVM qua `JAVA_TOOL_OPTIONS`/`MAVEN_OPTS`, chọn Maven profile (sheet bật → `master`), dựng ~22 cờ `-D` (thêm `-Drun.caseSource` / `-Drun.casesSheet` khi chạy theo sheet), ghi header vào `test-output/last-run.log`, gọi `mvn test`, rồi in tóm tắt `Tests run / fail / error / skip`. Hỗ trợ `-DryRun` (chỉ in cấu hình, không chạy Maven). Nếu không tìm thấy `mvn` trong PATH sẽ dò Maven bundled của IntelliJ. |

Hướng dẫn ngắn dành cho tester: [`docs/CHAY-TEST.md`](docs/CHAY-TEST.md).

---

## 10. Quy ước code

**Thứ tự ưu tiên locator** (theo javadoc `UiSynonyms`):

```
1. data-testid / aria-label            ← ổn định nhất, ưu tiên tuyệt đối
2. business key + UiSynonyms           ← khoá nghiệp vụ kèm danh sách nhãn đồng nghĩa
3. XPath contains() theo text          ← chỉ khi không còn cách nào khác
```

Các nguyên tắc khác:

- **Page Object không gọi Selenium trực tiếp** — mọi thao tác đi qua `WebUI`
  (ngoại lệ duy nhất hiện tại: một lệnh `body.sendKeys(ESCAPE)` trong `TaoDonFlow`).
- **Không hard-code timeout** — tất cả nằm trong `WaitConfig`.
- **Không hard-code nhãn tiếng Việt đơn lẻ** — dùng danh sách đồng nghĩa trong `UiSynonyms`,
  vì UI hay đổi cách viết nhãn.
- **Ảnh chụp ghi ra đĩa** qua `ScreenshotStore`, báo cáo chỉ trỏ `<img src>`. Nhúng base64 từng làm file HTML phình lên 16.8 MB một lượt.
- **Mỗi thread một Chrome độc lập** — dùng `DriverContext`, không dùng biến static.
  Đóng một Chrome chỉ dừng thread đó.
- **Rẽ nhánh UI đi qua `DataDictionary`** — đừng viết `if (loaiDon.equals("Phá sản"))` rải rác
  trong page object, hãy thêm predicate vào `DataDictionary`.

---

## 11. Xử lý sự cố

| Triệu chứng | Nguyên nhân & cách xử lý |
|---|---|
| `RuntimeException` báo thiếu key khi khởi động | Chưa tạo `src/test/resources/config.properties`. Copy từ `config.example.properties` hoặc set `TOAAN_USERNAME`/`TOAAN_PASSWORD`. |
| Script in `LỖI: run.suite='...' không hợp lệ.` rồi `exit 1` | Giá trị `run.suite` sai chính tả. Chỉ nhận: `smoke`, `mid`, `full`, `master`, `login`, `buoc23`, `unit`. |
| `LỖI: Không tìm thấy mvn.` | Maven không có trong PATH. Script sẽ tự dò Maven bundled của IntelliJ; nếu vẫn không thấy thì cài Maven hoặc thêm vào PATH. |
| Test fail với `StepBlockedException: ❌ Bước N — … — hệ thống báo lỗi: …` | Đây là **lỗi nghiệp vụ do hệ thống trả về**, không phải script hỏng. Đọc `systemMessage` trong báo cáo — thường là validation của server. Báo cáo đã có sẵn ảnh chụp tại thời điểm bị chặn. |
| Đóng nhầm 1 cửa sổ Chrome khi chạy song song | Thiết kế đã lường trước: `BrowserClosedException` → `DriverContext.abortCurrentThread` chỉ dừng thread đó, các Chrome còn lại chạy tiếp. Các test còn lại của thread đó sẽ bị SKIP. |
| Cửa sổ Chrome đè lên nhau / quá nhỏ | Chỉnh `run.window.width` / `run.window.height` / `run.window.scale`. Giá trị ≤ 200 px bị coi là "không đặt". |
| Chạy quá nhiều case cùng lúc gây chậm | `run.browsers` bị kẹp tối đa 8, và không bao giờ vượt quá số case trong `run.cases`. |
| Muốn chạy lại đúng vài case đã fail | `mvn -Pfull test -Dtaodon.onlyStt=12,13` |
| `run.cases` bị bỏ qua | Kiểm tra đủ **5 trường** ngăn bằng `>`. Thiếu trường sẽ in `⚠ run.cases token thiếu trường (cần 5 phần)` và bỏ case. Cũng nhớ `run.cases` không truyền được qua `-D`, và sheet đang bật sẽ ưu tiên hơn — đặt `run.caseSource=file` nếu muốn dùng `run.cases`. |
| Log in `Google trả về trang HTML (thường là trang đăng nhập)` | Sheet chưa chia sẻ công khai. Trên Google Sheet: **Chia sẻ → Bất kỳ ai có đường liên kết → Người xem**. |
| Log in `HTTP 404` khi tải sheet | Sai ID sheet trong `run.casesSheet` (hoặc sheet đã bị xoá). Dán lại link từ thanh địa chỉ. |
| Sheet có dữ liệu nhưng đọc được 0 case | (1) Dòng đầu bảng phải có cột **Loại đơn** — bộ đọc dò theo tên cột chứ không theo vị trí; (2) cột `Chạy` để trống = **tắt dòng đó**; (3) nếu sheet nhiều tab, đặt `run.casesSheetGid` bằng `gid` trên URL của tab đúng. |
| Chạy theo sheet nhưng thấy các case smoke mặc định | `run.caseSource` đang là `file` (menu preset ghi giá trị này). Chạy menu mục **1** hoặc sửa tay về `sheet`. |
| Mất mạng vẫn muốn chạy theo sheet | Lần chạy trước đã lưu `test-output/cases-sheet-cache.csv` — automation tự dùng cache và in cảnh báo kèm thời điểm tải. |
| Danh sách loại đơn / tòa án lệch với UI thật | Chạy `mvn test -Dtest=MasterDataSyncTest` để quét lại `master-data.properties` từ UI. |
| Chạy chậm, muốn kiểm tra logic ma trận thôi | `mvn -Punit test` — unit test ma trận + bộ đọc sheet, không mở trình duyệt, không gọi mạng. |
