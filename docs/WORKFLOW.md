# Workflow vận hành ToaAn v1

> Bản đồ toàn bộ docs: [`README.md`](README.md) (trong `docs/`).

Tài liệu ngắn cho người chạy / sửa automation hàng ngày. Chi tiết lệnh menu: [`CHAY-TEST.md`](CHAY-TEST.md). Schema case: [`CASE-SCHEMA.md`](CASE-SCHEMA.md). Fail: [`TRIAGE.md`](TRIAGE.md). DoD: [`V1-CHECKLIST.md`](V1-CHECKLIST.md).

**Hai đường:** tài liệu này = **vận hành test** (Dashboard / Sheet / matrix). Khi **sửa framework** (feature, locator, flow, generator) dùng chuỗi FIS plan→scenario→craft→test→ship — xem [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md).

## 1. Vòng đời một nhu cầu kiểm thử

```text
Nhu cầu (bug / regression / case mới)
  → Chọn kênh chạy (bảng dưới)
  → Chuẩn bị data/case (Dashboard · Sheet · matrix)
  → Sửa đúng tầng (tests / flow / pages / ui / data / config / report / caseui)
  → Chạy suite hẹp nhất hữu ích
  → Triage fail (TRIAGE.md)
  → Đóng: case ổn định + report đọc được + (nếu cần) cập nhật catalog
```

**Cửa chính case cấu hình:** `.\scripts\chay.cmd` → **mục 9** → Dashboard `http://localhost:8787`  
**1. Chọn case** → chọn một màn, lọc ca âm/dương và thêm đề xuất →
**2. Danh sách chạy** → **Lưu tất cả xuống file** → **Chạy case đã lưu** →
**3. Báo cáo** → tải lại khi Maven chạy xong.
Nhập Google Sheet, tra locator và tài liệu nằm trong **Nâng cao**.

## 2. Bảng suite / lệnh

| Suite | Chrome? | Lệnh | Khi nào |
|---|---|---|---|
| **unit** | Không | `mvn -B -Punit test` | Mỗi PR / trước khi chạy UI; gồm matrix, sheet parser, `TestCaseGeneratorTest`, báo cáo |
| **login** | Có | `mvn -Plogin test` | Chỉ đăng nhập (+ ca âm login trong `LoginTest`) |
| **smoke** | Có | `mvn -Psmoke test` | Hàng ngày nhanh (~3 case) |
| **parallel-smoke** | Có | `mvn -Pparallel-smoke test` | Smoke 3 Chrome |
| **mid** | Có | `mvn -Pmid test` | Regression tuần (~39) |
| **full** | Có | `mvn -Pfull test` | Trước release / đổi catalog (~106 pairwise) |
| **master** | Có | `mvn -Pmaster test` hoặc Dashboard **Chạy** / menu Sheet | Case cấu hình (`local-cases.json` / Sheet / `run.cases`) |
| **discovery** | Có | `mvn -Pdiscovery test` | Quét ca âm field → CSV (không assert) |
| **MasterDataSync** | Có | `mvn test -Dtest=MasterDataSyncTest` | UI đổi danh mục → cập nhật `master-data.properties` |

Menu tương đương: mục 1 = Sheet+master, 3–4 = smoke, 5 = login, 6 = mid, 7 = full, 9 = Dashboard.

CI tương đương unit: `.github/workflows/ci.yml` (`mvn -B -Punit test`). Chạy tay khi không có Actions: cùng lệnh trên máy local.

## 3. Chọn kênh case

| Kênh | Dùng khi | Không dùng khi |
|---|---|---|
| **Dashboard · 1. Chọn case** (menu 9) | Cần đề xuất theo màn, ca âm whitelist, merge nhanh vào danh sách chạy | Cần ma trận độ phủ đầy đủ (dùng mid/full) |
| **Dashboard · 2. Danh sách chạy** / `local-cases.json` | Chỉnh case cấu hình, lưu file và chạy master có kiểm soát | Case “framework” (login âm, discovery, unit) |
| **Google Sheet** (menu 1) | BA/QA giữ danh sách ngoài repo, nhiều người cùng sửa | Mạng/sheet lỗi quyền — fallback file + cache |
| **Matrix smoke/mid/full** | Độ phủ catalog / pairwise, không cần viết từng dòng | Case nghiệp vụ một-off (đưa vào Sheet/JSON) |

## 4. Quy tắc chỗ đặt case

- Case **cấu hình** (loại đơn, untilStep, ca âm form) → Sheet hoặc `local-cases.json` / Dashboard — **không** nhét thêm method vào Java test class.
- Java test class chỉ giữ: **login** (`LoginTest`), **discovery**, **unit**, **matrix** (`TaoDonTest` smoke/mid/full), sync catalog.
- Sửa locator / bước wizard → `pages/` + (nếu cần) `WebUI`; không vá bằng sleep trong test.
- Đổi danh mục UI → `MasterDataSyncTest` rồi commit `master-data.properties` (không commit `config.properties`).

## 5. Definition of Done v1

Tick theo [`V1-CHECKLIST.md`](V1-CHECKLIST.md).
