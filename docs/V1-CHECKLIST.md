# Checklist DoD — ToaAn v1

Đánh dấu khi xác nhận trên branch hiện tại.

## Tài liệu & cửa vận hành

- [x] `docs/WORKFLOW.md` — vòng đời, suite, kênh Dashboard/Sheet/matrix
- [x] `docs/TRIAGE.md` — BUG / TEST_SAI / FLAKE / ENV_DATA
- [x] `docs/CASE-SCHEMA.md` — CaseRow / Sheet / generator `GEN_…`
- [x] `docs/TECH-DEBT.md` — nợ P1–P3, phạm vi v1
- [x] README có mục **Vận hành v1** + link docs; nêu menu **9** / Sinh test case
- [x] `docs/CHAY-TEST.md` link WORKFLOW + TRIAGE + CASE-SCHEMA
- [x] `docs/FIS-WORKFLOW.md` — tích hợp FIS (scenario + Tester Maven + plan→ship); link từ README / WORKFLOW / CHAY-TEST
- [x] `docs/test-spec.md` có đoạn chiến lược trỏ workflow/dashboard

## Tool đã ship (không viết lại)

- [x] Dashboard `CaseEditorServer` + tab Sinh test case / API generate-cases
- [x] `TestCaseGenerator` + `TestCaseGeneratorTest` trong `-Punit`
- [x] Case cấu hình qua `local-cases.json` / Sheet + master

## Chất lượng chạy

- [x] Field bắt buộc bước 1–3: `setTextRequired` (fail sớm, tên field rõ); optional giữ soft-skip
- [x] Khi đến Xem lại (`untilStep >= 6`): assert tín hiệu ổn định (marker + loại đơn)
- [x] `mvn -B -Punit test` pass
- [x] Không commit `config.properties` / secret

## CI

- [x] `.github/workflows/ci.yml` — push/PR chạy `mvn -B -Punit test`
- [x] (Tuỳ chọn) `workflow_dispatch` smoke — không bắt buộc credentials mặc định

## Cố ý chưa làm (v1)

- [x] Không Playwright / LLM / rewrite `WebUI` / đổi báo cáo Allure
- [x] Generator không nâng thành full pairwise (ghi TECH-DEBT)
