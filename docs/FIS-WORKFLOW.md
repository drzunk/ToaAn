# FIS workflow cho ToaAn

> Bản đồ toàn bộ docs: [`README.md`](README.md) (trong `docs/`). Tầng 1 vận hành: [`WORKFLOW.md`](WORKFLOW.md).

Tài liệu tích hợp duy nhất: gom skill FIS (plan → scenario → scout → craft → test → ship)
cho đúng dự án Selenium/TestNG này. **Không** `/fis-bootstrap` trên repo đã có.

> Hai đường song song — đừng lẫn:
>
> | Đường | Việc | Công cụ |
> |---|---|---|
> | **Vận hành test** | Chọn case → Lưu → Chạy → Báo cáo → Triage | Dashboard menu 9, [`WORKFLOW.md`](WORKFLOW.md), [`TRIAGE.md`](TRIAGE.md) |
> | **Phát triển / sửa framework** | Feature hoặc fix code automation | FIS: plan → scenario → scout → craft → test → ship |

Vận hành hàng ngày: [`WORKFLOW.md`](WORKFLOW.md) · lệnh menu: [`CHAY-TEST.md`](CHAY-TEST.md).
Bộ docs FIS đầy đủ (overview, architecture…) là **optional** — chỉ khi cần, chạy `/fis-docs init` sau; không bắt buộc cho chuỗi dưới đây.

---

## A. Hai đường song song

| Khi nào | Đi đường nào |
|---|---|
| Thêm/chạy case, đọc báo cáo, phân BUG/FLAKE | **Vận hành** — Dashboard / Sheet / matrix |
| Sửa locator, flow, Dashboard API, generator, schema case | **Phát triển** — chuỗi FIS |
| Cả hai: lộ ca biên rồi đưa vào Dashboard | Scenario (FIS) → Chọn case / Sheet (vận hành) |

Người tester vẫn quyết nghiệp vụ và triage. FIS chỉ hỗ trợ khi **sửa code** hoặc **lộ ca biên**.

---

## B. Chuỗi FIS — full hay rút

```text
plan → scenario → scout → craft → test → ship
```

| Chế độ | Khi nào | Chuỗi |
|---|---|---|
| **Full** | Tính năng mới / nhiều trạng thái: validate màn mới, đổi `TaoDonFlow`, Dashboard hành vi mới, đổi schema `CaseRow`, nhiều `pages/` + `WebUI` | plan → **scenario** → scout (nếu chưa quen) → craft → test → ship |
| **Rút** | Đổi chữ, xóa dead code, sửa một locator, thêm hằng `WaitConfig`, sửa doc | `/fis:craft` + Tester diff-aware (`/fis:test`) — bỏ plan/scenario dài |
| **Cấm** | Cài lại kit trên repo này | **Không** `/fis-bootstrap` |

Scout chèn khi chưa quen vùng code; bỏ qua nếu vừa làm đúng các file đó trong phiên.

---

## C. `/fis:scenario` — lọc chiều cho ToaAn

Dùng **trước craft** hoặc **trước bổ sung ca âm**. Không cần đủ 12 chiều mọi lần.

### Ưu tiên

| Chiều | Vì sao quan trọng với ToaAn |
|---|---|
| **Business Logic** | 7 loại đơn, CN/TC, Phá sản, ràng buộc UI (`DataDictionary`) |
| **Input Extremes** | Field bắt buộc trống/sai → ca âm; soft-skip vs `setTextRequired` |
| **State Transitions** | Wizard 0–6, `untilStep`, abort giữa bước, chỉnh sửa từ Xem lại |
| **Timing** | Toast/validation race, chuyển bước SPA, parallel Chrome |
| **Error Cascades** | Captcha fail, eform chưa xuất bản, browser đóng, Sheet lỗi quyền |

### Dùng khi đụng hạ tầng

| Chiều | Khi nào |
|---|---|
| **Integration** | Google Sheet CSV, Maven spawn từ Dashboard, `MavenResolver` |
| **Environment** | Captcha OCR, máy không có `mvn` trên PATH, catalog UI lệch |

### Bỏ / ít dùng mặc định

- **Authorization** đa role — trừ khi sau này có nhiều vai trò trên cổng.
- Không ép đủ 12 chiều mỗi lần gọi.

### Đầu ra

Bảng **Critical / High** → chọn một trong:

1. Dashboard **1. Chọn case** / Sheet (`CASE-SCHEMA.md`)
2. [`TECH-DEBT.md`](TECH-DEBT.md) nếu chưa làm được ngay
3. Mục rủi ro trong plan (`plans/…`)

### Lệnh gọi sẵn (copy)

```text
/fis:scenario "Ca âm bước 2–3 nguyên đơn / bị đơn — field bắt buộc trống hoặc sai format"
/fis:scenario src/main/java/vn/tuphap/automation/pages/NoiDungDonPage.java
/fis:scenario "Dashboard Chọn → Thêm → Lưu → Chạy khi local-cases rỗng hoặc chưa lưu"
/fis:scenario "Master chạy song song 3 Chrome + ScenarioDispatch claim trùng"
```

---

## D. Tester / `/fis:test` — diff-aware map Maven

**Không** copy nguyên quy ước Jest. Map theo Selenium/TestNG của repo.

### Map file → test

| Đổi file | Test / lệnh gợi ý |
|---|---|
| `src/main/java/.../Foo.java` | `src/test/java/.../FooTest.java` (cùng package path) nếu có |
| `caseui/TestCaseGenerator.java` | `TestCaseGeneratorTest` → `mvn -B -Dtest=TestCaseGeneratorTest test` |
| `caseui/FieldCoverageCatalog.java` | `FieldCoverageCatalogTest` |
| `caseui/MavenResolver.java` | `MavenResolverTest` |
| `config/CaseSheetSource.java` | `CaseSheetSourceTest` |
| `config/CaseFileSource.java` / case JSON | `ConfiguredCasesTest` |
| `data/*Matrix*.java` | `MidCoverageMatrixTest` / `FullCoverageMatrixTest` |
| `report/BaoCao*.java` | `BaoCaoHtmlTest`, `BaoCaoTeeTest` |
| `pom.xml`, `suites/*.xml`, `WebUI`, `DataGenerator`, `RunFlowConfig` | **`mvn -B -Punit test`** (escalation) |
| Diff hẹp, suy ra được class | `mvn -B -Dtest=A,B test` |

### Không làm mặc định

- **Không** mặc định `-Psmoke` / `-Pmid` / `-Pfull` (Chrome). UAT vẫn là **người + Dashboard**.
- Coverage 80% toàn `WebUI`: **không** đặt làm ngưỡng. Chỉ kỳ vọng unit (matrix, generator, catalog, report, sheet parser…).

### Báo cáo mong đợi (sau `/fis:test`)

```text
Changed:   <file đổi>
Mapped:    <test class / lệnh Maven>
Result:    pass | fail
Unmapped:  <file đổi không có test — ghi rõ>
```

CI tương đương unit: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) (`mvn -B -Punit test`).

---

## E. Vai trò tách bạch

| Ai | Việc | Không làm thay |
|---|---|---|
| **Người (tester)** | Nghiệp vụ, Dashboard, TRIAGE, quyết BUG / TEST_SAI / FLAKE / ENV_DATA | Không nhờ AI “sửa test cho xanh” khi là bug sản phẩm |
| **`/fis:scenario`** | Lộ ca biên chưa nghĩ (Critical/High) | Không tự ghi vào Sheet / chạy Chrome |
| **`TestCaseGenerator`** | Sinh đề xuất case đã biết theo catalog + whitelist field | Không thay scenario exploration |
| **Tester / `/fis:test`** | Chạy test tự động sau diff (ưu tiên unit) | Không thay UAT Dashboard |
| **`/fis:craft`** (hoặc `/fis:fix`) | Sửa code đúng tầng | Không thêm case nghiệp vụ vào Java test class |

Chỗ đặt case: [`WORKFLOW.md`](WORKFLOW.md) mục 4 · schema: [`CASE-SCHEMA.md`](CASE-SCHEMA.md).

---

## F. Prompt mẫu (copy-paste)

### 1. Feature mới — full FIS

```text
Theo docs/FIS-WORKFLOW.md (đường Phát triển, full):
1) /fis:plan — pha + file đụng (pages/flow/caseui…)
2) /fis:scenario — ưu tiên Business Logic, Input Extremes, State Transitions, Timing, Error Cascades
3) Critical/High → ghi rủi ro plan + gợi ý case Dashboard/Sheet
4) /fis:scout nếu chưa quen → /fis:craft
5) /fis:test diff-aware (Maven map trong FIS-WORKFLOW §D) → /fis:ship khi unit xanh
Không bootstrap. Không chạy smoke/mid/full trừ khi tôi yêu cầu.
```

### 2. Chỉ scenario trước bổ sung ca âm

```text
/fis:scenario "Bổ sung ca âm bước 2 Nguyên đơn — CCCD/SĐT/ngày cấp trống hoặc sai"
Chỉ bảng Critical/High. Map từng dòng sang field whitelist CASE-SCHEMA / FieldCoverageCatalog.
Không sửa Java. Đầu ra để tôi thêm vào Dashboard Chọn case hoặc Sheet.
```

### 3. Sau craft — Tester diff-aware

```text
/fis:test theo docs/FIS-WORKFLOW.md §D:
Phân tích git diff → map Foo.java → FooTest / -Dtest=… hoặc -Punit nếu đụng WebUI/pom/suites.
Báo cáo: changed → mapped → pass/fail → unmapped.
Không chạy Chrome suite mặc định.
```

### 4. Sửa nhỏ — craft + unit

```text
Rút theo FIS-WORKFLOW: /fis:craft sửa [mô tả 1 locator / dead code / typo].
Sau đó /fis:test diff-aware hoặc mvn -B -Punit test.
Không plan dài, không scenario đủ 12 chiều, không bootstrap.
```

### 5. Fail sau chạy Dashboard — triage trước, FIS sau

```text
Đọc docs/TRIAGE.md. Phân đúng một loại: BUG | TEST_SAI | FLAKE | ENV_DATA
kèm bằng chứng từ test-output/index.html.
Chỉ khi TEST_SAI / FLAKE cần sửa code mới dùng /fis:fix + /fis:test.
```

---

## G. Liên kết

| Doc / file | Vai trò |
|---|---|
| [`WORKFLOW.md`](WORKFLOW.md) | Vòng đời vận hành, suite, kênh case |
| [`CHAY-TEST.md`](CHAY-TEST.md) | Menu `chay.cmd`, Dashboard 3 bước |
| [`CASE-SCHEMA.md`](CASE-SCHEMA.md) | Field JSON·Sheet, `untilStep`, ca âm, `GEN_…` |
| [`TRIAGE.md`](TRIAGE.md) | BUG / TEST_SAI / FLAKE / ENV_DATA |
| [`TECH-DEBT.md`](TECH-DEBT.md) | Nợ P1–P3; chỗ gửi scenario chưa làm được |
| [`V1-CHECKLIST.md`](V1-CHECKLIST.md) | DoD v1 |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | CI = `mvn -B -Punit test` |

### Không secret

- Không commit `src/test/resources/config.properties` (đã gitignore).
- Credential: env `TOAAN_USERNAME` / `TOAAN_PASSWORD` / `TOAAN_BASE_URL` (CI: GitHub secrets).
