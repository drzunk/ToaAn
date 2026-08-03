---
name: fis:ehrp
description: "FIS EHRP (Vietnam government Human Resource Platform) knowhow — ngạch / bậc / hệ số lương, phụ cấp, đánh giá công chức viên chức, BHXH/BHYT/BHTN, regulatory VN (Luật CBCC, Luật Viên chức, NĐ 204/2004, NĐ 138/2020, TT 09/2024). Activate when designing PRD/feature-spec for government HR features."
category: backend
keywords: [ehrp, hrm, hr, gov, government, civil-servant, ngach, bac, luong, payroll, bhxh, vietnam, fis]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# FIS EHRP knowhow

Domain reference for FIS EHRP (Electronic HR Platform — government HR systems for cơ quan nhà nước). Activate alongside `/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test` when designing or reverse-engineering features for civil-servant / public-employee management.

## When to Apply

Reference these guidelines when:
- Designing salary calculation, evaluation, promotion features for công chức / viên chức
- Mapping ngạch / bậc / hệ số lương to FIS data model
- Building BHXH / BHYT / BHTN integration
- Reporting to Bộ Nội vụ / Sở Nội vụ
- Validating regulatory compliance (Luật CBCC 2008, Luật Viên chức 2010, NĐ 204/2004/NĐ-CP)

## Topic Categories

| Category | Files | When |
|---|---|---|
| Ngạch & Bậc | `references/ngach-bac.md` | Civil servant rank system |
| Lương | `references/luong-payroll.md` | Salary calculation, hệ số lương, phụ cấp |
| Đánh giá | `references/danh-gia.md` | Annual evaluation, KPI, kỷ luật |
| BHXH | `references/bhxh-bhyt-bhtn.md` | Social insurance, health insurance, unemployment |
| Quy hoạch | `references/quy-hoach-bo-nhiem.md` | Promotion planning, appointment |
| Regulatory | `references/regulatory.md` | Luật CBCC, Luật Viên chức, NĐ 204, NĐ 138, TT 09 |
| Reports | `references/reports.md` | Bộ Nội vụ, Sở Nội vụ standard reports |
| Anti-patterns | `references/anti-patterns.md` | Known pitfalls in EHRP projects |

## Quick Reference

### Ngạch / Bậc / Hệ số lương

- Public employee ranks indexed by ngạch (e.g. chuyên viên cao cấp, chuyên viên chính, chuyên viên).
- Each ngạch has multiple bậc; each bậc has hệ số lương (per NĐ 204/2004, Phụ lục).
- Salary = mức lương cơ sở × hệ số × (1 + sum phụ cấp %).
- Mức lương cơ sở changes by Nghị định: 1.490.000 VND (2019), 1.800.000 (2023), 2.340.000 (1/7/2024).

### Phụ cấp common

- Phụ cấp chức vụ (position allowance) — % per chức vụ table.
- Phụ cấp khu vực (regional allowance) — per khu vực 0.1 / 0.2 / 0.3 / 0.4 / 0.5 / 0.7 / 1.0.
- Phụ cấp thâm niên vượt khung (over-grade tenure) — for staff stuck at top bậc.
- Phụ cấp công vụ (civil-service allowance) — 25% (Decree 17/2025/NĐ-CP applies).

### BHXH / BHYT / BHTN

- BHXH (Social Insurance) — 8% employee + 17.5% employer = 25.5% of salary base.
- BHYT (Health) — 1.5% + 3% = 4.5%.
- BHTN (Unemployment) — 1% + 1% = 2%.
- Salary base capped at 20× mức lương cơ sở.

## Project override

`<project>/.fis/knowhow/ehrp/<topic>.md`.

## Output

Reference skill — no artifacts emitted. Consumers (`/fis-outcome`, `/fis-plan`, `/fis-test`) embed loaded references into their generated artifacts.

## CHANGELOG

- 1.0.0 — Initial knowhow extraction from FIS-EHRP project archive.
