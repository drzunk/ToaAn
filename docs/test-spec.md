---
title: "ToaAn — Đặc tả kiểm thử (Test Spec)"
template: test-spec
version: "1.0"
status: "Nháp"
doc_code: "TOAAN-TP-01"
date: "2026-08-03"
---

# ToaAn — Đặc tả kiểm thử

## 1. Phạm vi

Bộ automation UI cho cổng nộp đơn trực tuyến Tòa án (`demo-dichvutuphap.gsfpt.com`), nghiệp vụ **Tạo đơn / Nộp đơn** (6 bước).

| Hạng mục | Giá trị |
|---|---|
| Stack | Java 17, Selenium 4.46, TestNG 7.9, Maven Surefire |
| Báo cáo | `BaoCaoHtml` → `test-output/index.html` |
| Nguồn case | Google Sheet / `local-cases.json` / ma trận smoke·mid·full |

**Ngoài phạm vi:** hiệu năng tải trang, bảo mật mạng, tương thích trình duyệt ngoài Chrome.

## 2. Mục tiêu kiểm thử

1. Luồng tạo đơn điền đúng → đến bước cấu hình (0–6) → tùy chọn Gửi đơn.
2. Validation (ca âm): hệ thống chặn khi điền sai / bỏ trống field bắt buộc.
3. Đăng nhập: thành công + 3 ca âm (sai MK, sai captcha, trống MK).
4. Ma trận độ phủ mid/full đúng catalog; parser sheet/JSON ổn định.
5. Báo cáo HTML đọc được, khớp mốc thư mục `runs/<mốc>/`.

## 3. Chiến lược

| Tầng | Suite / lệnh | Chrome? | Mục đích |
|---|---|---|---|
| Unit | `mvn -Punit test` | Không | Ma trận, sheet parser, case config, báo cáo |
| Login | `mvn -Plogin test` | Có | Đăng nhập độc lập |
| Smoke | `mvn -Psmoke test` (hoặc parallel) | Có | ~3 case nhanh |
| Mid | `mvn -Pmid test` | Có | ~39 kịch bản regression |
| Full | `mvn -Pfull test` | Có | ~106 kịch bản pairwise |
| Master | `mvn -Pmaster test` / menu Sheet·file | Có | Case cấu hình (sheet/JSON) |
| Discovery | `mvn -Pdiscovery test` | Có | Quét field — **không assert**, chỉ CSV |
| Sync catalog | `mvn test -Dtest=MasterDataSyncTest` | Có | Cập nhật `master-data.properties` |

Ưu tiên chạy hàng ngày: **unit → smoke**. Mid/full theo lịch regression. Master khi có danh sách case nghiệp vụ.

## 4. Ma trận AC coverage

### 4.1 Luồng chính (ca dương)

| AC | Mô tả | Suite / test | Độ sâu |
|---|---|---|---|
| AC-01 | Đăng nhập thành công → Dashboard | `LoginTest#testDangNhapThanhCong` | login |
| AC-02 | Bước 1–3 đủ 7 loại đơn | `TaoDonTest#testBuoc2Va3BayLoaiDon` | buoc23 |
| AC-03 | 6 bước tạo đơn + gửi đơn | `TaoDonTest#testFlowTaoDon` | smoke/mid/full |
| AC-04 | Xem lại → Chỉnh sửa nội dung | `TaoDonTest#testChinhSuaNoiDungTuXemLai` | smoke/full |
| AC-05 | Case cấu hình (sheet/JSON) đến bước N | `MasterExecutionTest` | master |
| AC-06 | Gửi đơn chỉ khi `submit` + `untilStep≥6` | `MasterExecutionTest` + `RunFlowConfig` | master |
| AC-07 | Nhánh CN/TC, 1–2 bị đơn, ĐD, NLQ, TLBS | Mid 39 / Full 106 | mid/full |
| AC-08 | Phá sản: 4 tư cách, 1 bị đơn, loại việc cố định | Mid/Full + unit | mid/full/unit |

### 4.2 Ca âm

| AC | Mô tả | Cách khai báo / test |
|---|---|---|
| AC-N01 | Sai mật khẩu — không vào Dashboard + có thông báo | `LoginTest#testDangNhapSaiMatKhau` |
| AC-N02 | Sai captcha | `LoginTest#testDangNhapSaiCaptcha` |
| AC-N03 | Trống mật khẩu | `LoginTest#testDangNhapBoTrongMatKhau` |
| AC-N04 | Field form sai → hệ thống chặn đúng thông báo | Cột `Trường lỗi` / `Giá trị lỗi` / `Thông báo mong đợi` trên sheet hoặc JSON |
| AC-N05 | Không chặn khi kỳ vọng chặn → FAIL (lỗ hổng validation) | `MasterExecutionTest` + `hasNegativeExpectation()` |

**13 trường lỗi form** (override): SĐT/Email/CCCD/Họ tên/MST nguyên đơn & bị đơn; Ngày sinh; Ngày cấp CCCD; Giá trị tranh chấp. Chọn sai ngữ cảnh → override bỏ qua → ca âm FAIL (đọc log).

### 4.3 Unit / hạ tầng dữ liệu

| AC | Test class | Kỳ vọng |
|---|---|---|
| AC-U01 | `FullCoverageMatrixTest` | `validateCoverage` rỗng; ≥ cặp + 3; đủ CN/TC, 2 BD, ĐD, NLQ |
| AC-U02 | `MidCoverageMatrixTest` | 35 thường + 4 tư cách PS = 39 |
| AC-U03 | `CaseSheetSourceTest` (18) | Parse cột, alias, ca âm, CSV đặc biệt |
| AC-U04 | `ConfiguredCasesTest` (5) | Bỏ dòng sai catalog; giữ cặp scenario↔profile; Phá sản ép BD=1 |
| AC-U05 | `BaoCaoTeeTest` / `BaoCaoHtmlTest` | Báo cáo tee + HTML |

## 5. Danh mục test case (tóm tắt)

### 5.1 UI / E2E

| ID | Tên | Loại | Chrome |
|---|---|---|---|
| TC-LGN-01…04 | Đăng nhập (+ 3 ca âm) | UI | Có |
| TC-SMOKE-* | Smoke ~3 case (PS + eform DS + random) | UI | Có |
| TC-MID-* | Mid ~39 | UI | Có |
| TC-FULL-* | Full ~106 | UI | Có |
| TC-MSTR-* | Case sheet/JSON (vd. `TC_Luong001` trong `local-cases.json`) | UI | Có |
| TC-B23-* | Bước 2–3 × 7 loại đơn | UI | Có |
| TC-EDIT-01 | Chỉnh sửa từ Xem lại | UI | Có |
| TC-DISC-* | Discovery ~28 probe | Quan sát | Có |

### 5.2 Unit (không Chrome)

| Class | Số test (ước lượng) |
|---|---|
| FullCoverageMatrixTest | 1 |
| MidCoverageMatrixTest | 1 |
| CaseSheetSourceTest | 18 |
| ConfiguredCasesTest | 5 |
| BaoCaoTeeTest | 10 |
| BaoCaoHtmlTest | 13 |
| **Tổng unit suite** | **~46** |

## 6. Dữ liệu & môi trường

| Mục | Chi tiết |
|---|---|
| Config | `config.properties` hoặc env `TOAAN_USERNAME` / `TOAAN_PASSWORD` / `TOAAN_BASE_URL` |
| Điều khiển | `run-flow.properties` (`suite`, `browsers`, `caseSource`, `untilStep`, `submit`) |
| Case cục bộ | `src/test/resources/local-cases.json` (hiện: 1 case Dân sự / BT ngoài HĐ / TC / Sơn La / until=6 / submit) |
| Upload mẫu | `testdata/sample.pdf`, `sample.png` + POI sinh xlsx/docx |
| Seed Faker | `20240724L` — tái lập được |

## 7. Tiêu chí đạt / không đạt

| Kết quả | Điều kiện |
|---|---|
| PASS (dương) | Flow đến `untilStep`; toast lỗi hệ thống = fail cứng |
| PASS (âm) | Bị `StepBlockedException`; thông báo chứa `thongBaoMongDoi` (rỗng = chấp nhận mọi thông báo) |
| FAIL (âm) | Không bị chặn (= lỗ hổng) hoặc thông báo lệch kỳ vọng |
| SKIP | Trình duyệt đóng giữa chừng (`BrowserClosedException` → Skip) |

**Cổng chất lượng đề xuất**

- Unit: 100% pass trước khi chạy UI.
- Smoke: 100% trước merge cấu hình chạy.
- Mid/Full: theo lịch; không bỏ qua fail.

## 8. Lỗ hổng phủ & rủi ro

| # | Gap / rủi ro | Mức | Gợi ý |
|---|---|---|---|
| 1 | ~~Unit `BaoCaoHtmlTest.gioHienThiLayTuMocThuMuc` FAIL~~ — đã sửa: `mucLuotChay` hiện lại đường dẫn `runs/<mốc>/` | Đã đóng | — |
| 2 | Discovery chưa quét địa chỉ lồng tỉnh/phường, CCCD/NS người ĐD tổ chức | Trung | Mở rộng sweep sau Giai đoạn 1 |
| 3 | Ca âm form phụ thuộc sheet/JSON — không có bộ ca âm form cố định trong repo | Trung | Thêm vài ca âm mẫu vào `local-cases.json` |
| 4 | UI song song phụ thuộc session/Chrome ổn định | Trung | Giữ `ScenarioDispatch`; theo dõi abort thread |
| 5 | Sheet sai chia sẻ / rỗng → master dừng (đúng) nhưng dễ nhầm “hỏng tool” | Thấp | Checklist README 6.4 |

## 9. Lịch chạy gợi ý

| Tần suất | Việc |
|---|---|
| Mỗi thay đổi code | `mvn -Punit test` |
| Hàng ngày | Smoke 1 Chrome |
| Tuần | Mid 3 Chrome |
| Trước release / đổi catalog | Full + `MasterDataSyncTest` nếu UI đổi danh mục |
| Khi mở rộng ca âm | Discovery → chọn field → ghi sheet |

## 10. Tham chiếu

- `README.md` §5 (bộ test), §6 (cấu hình / sheet / ca âm)
- `docs/CHAY-TEST.md` — hướng dẫn chạy menu
- Suite XML: `src/test/resources/suites/`
- Báo cáo QA phiên bản này: `plans/reports/260803-1353-tester-test-spec.md`
