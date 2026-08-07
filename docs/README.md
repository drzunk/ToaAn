# Docs ToaAn — bắt đầu ở đây

Cửa vào duy nhất. Mở file đúng việc; đừng đọc hết một lần.

## Tôi muốn…

| Việc | Mở file này |
|---|---|
| Chạy test / Dashboard hôm nay | [`CHAY-TEST.md`](CHAY-TEST.md) → [`WORKFLOW.md`](WORKFLOW.md) |
| Fail rồi phân loại | [`TRIAGE.md`](TRIAGE.md) |
| Thêm / sửa case JSON·Sheet | [`CASE-SCHEMA.md`](CASE-SCHEMA.md) |
| Sửa code / nhờ AI (FIS) | [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md) |
| Xem nợ kỹ thuật | [`TECH-DEBT.md`](TECH-DEBT.md) |
| Onboard — dự án là gì | [`project-overview-pdr.md`](project-overview-pdr.md) |
| Kiến trúc / map package | [`system-architecture.md`](system-architecture.md) · [`codebase-summary.md`](codebase-summary.md) |
| Quy ước khi viết code | [`code-standards.md`](code-standards.md) |
| Đặc tả AC / ma trận test | [`test-spec.md`](test-spec.md) |
| DoD v1 đã chốt gì | [`V1-CHECKLIST.md`](V1-CHECKLIST.md) |
| Làm gì tiếp theo (ưu tiên nợ) | [`project-roadmap.md`](project-roadmap.md) |

---

## Tầng 1 — Hàng ngày (bắt buộc thuộc)

| File | Một dòng |
|---|---|
| [`CHAY-TEST.md`](CHAY-TEST.md) | Menu `chay.cmd`, Dashboard 3 bước |
| [`WORKFLOW.md`](WORKFLOW.md) | Vòng đời vận hành, suite, kênh case |
| [`TRIAGE.md`](TRIAGE.md) | BUG / TEST_SAI / FLAKE / ENV_DATA |
| [`CASE-SCHEMA.md`](CASE-SCHEMA.md) | Field JSON·Sheet, ca âm, `GEN_…` |

Hai đường đừng lẫn: **vận hành** = tầng 1 · **sửa framework** = tầng 2.

---

## Tầng 2 — Khi sửa framework (FIS)

| File | Một dòng |
|---|---|
| [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md) | plan → scenario → craft → test → ship; map Maven; prompt mẫu |
| [`.cursor/rules/toa-an-fis.mdc`](../.cursor/rules/toa-an-fis.mdc) | Rule Cursor (không always-on) — trỏ agent sang FIS-WORKFLOW |

Gate: `mvn -B -Punit test`. Không `/fis-bootstrap` trên repo này.

---

## Tầng 3 — Tham chiếu (đọc khi cần)

| File | Khi nào mở |
|---|---|
| [`test-spec.md`](test-spec.md) | Viết/đối chiếu AC coverage |
| [`TECH-DEBT.md`](TECH-DEBT.md) | Biết nợ chấp nhận / chưa làm |
| [`V1-CHECKLIST.md`](V1-CHECKLIST.md) | Kiểm tra DoD v1 |
| [`code-standards.md`](code-standards.md) | Review / Required API / chỗ đặt case |
| [`codebase-summary.md`](codebase-summary.md) | Định vị package / LOC / suite |
| [`system-architecture.md`](system-architecture.md) | Quyết định kiến trúc, Dashboard, báo cáo |
| [`project-overview-pdr.md`](project-overview-pdr.md) | Người mới: bối cảnh + phạm vi |
| [`project-roadmap.md`](project-roadmap.md) | Thứ tự P1–P3 từ TECH-DEBT |

Các file tầng 3 **không** cần thuộc lòng để chạy test hàng ngày.

---

## Không mở hàng ngày

| Chỗ | Lý do |
|---|---|
| `.cursor/skills/**` | FIS AI Kit — skill cho agent; không phải hướng dẫn vận hành |
| `plans/**` | Plan từng việc / báo cáo phiên — lịch sử công việc |
| `README.md` (root) | Dài (~700 dòng): chỉ dùng mục lục + mục **Vận hành** / **FIS**; chi tiết kỹ thuật khi cần |

---

## Gợi ý 30 giây

1. Chạy case → `CHAY-TEST.md`
2. Đỏ → `TRIAGE.md`
3. Nhờ AI sửa code → `FIS-WORKFLOW.md`
4. Mọi thứ khác → quay lại bảng **Tôi muốn…** ở trên
