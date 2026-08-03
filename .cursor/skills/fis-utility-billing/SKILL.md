---
name: fis:utility-billing
description: "FIS utility billing knowhow — EVN biểu giá điện bậc thang, đo đếm, hợp đồng PE, tính tiền điện sinh hoạt + sản xuất + kinh doanh, phụ tải, công tơ, chu kỳ ghi-tính-thu, thoái thu / nợ tiền điện. Activate when designing PRD/feature-spec for electricity billing or utility metering features."
category: backend
keywords: [evn, dien, electricity, utility, billing, bac-thang, cong-to, hop-dong-pe, vietnam, fis]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# FIS utility billing knowhow

Domain reference for FIS utility billing engagements (EVN, EPC, EPTC). Activate alongside `/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test` when designing electricity billing, metering, or PE (Provider-Enterprise) contract features.

## When to Apply

- Designing tính tiền điện features (sinh hoạt / sản xuất / kinh doanh / hành chính sự nghiệp)
- Mapping biểu giá bậc thang to data model
- Building chu kỳ ghi-tính-thu (read → compute → collect)
- PE contract management
- Reverse engineering legacy EVN systems

## Topic Categories

| Category | Files | When |
|---|---|---|
| Biểu giá | `references/bieu-gia-bac-thang.md` | Tariff structure, bậc thang, peak/off-peak |
| Đo đếm | `references/do-dem-cong-to.md` | Metering, công tơ types, MDM |
| Hợp đồng | `references/hop-dong-pe.md` | PE contract, sửa chữa, cắt-mở |
| Chu kỳ tính | `references/chu-ky-tinh.md` | Read → compute → bill → collect cycle |
| Phụ tải | `references/phu-tai-load-curve.md` | Load curve, peak demand |
| Nợ & thoái thu | `references/no-thoai-thu.md` | Past due, refund, write-off |
| Anti-patterns | `references/anti-patterns.md` | Known pitfalls |

## Quick Reference

### Biểu giá điện sinh hoạt (Quyết định 24/2017/QĐ-TTg, cập nhật 2024)

| Bậc | Mức (kWh) | Giá (VND/kWh) — minh họa, cập nhật theo QĐ EVN |
|---|---|---|
| 1 | 0-50 | ~1.806 |
| 2 | 51-100 | ~1.866 |
| 3 | 101-200 | ~2.167 |
| 4 | 201-300 | ~2.729 |
| 5 | 301-400 | ~3.050 |
| 6 | > 400 | ~3.151 |

Plus VAT 10% (V1) + thuế bảo vệ môi trường nếu có.

### Biểu giá kinh doanh / sản xuất

- 3 khung giờ: cao điểm / bình thường / thấp điểm.
- Hệ số khung giờ khác nhau per nhóm khách hàng.

### Công tơ

- Cơ khí (mechanical) — đọc thủ công.
- Điện tử 1 pha / 3 pha — đo điện năng + công suất.
- AMR (Automated Meter Reading) — đọc remote qua RF / GPRS.

## Project override

`<project>/.fis/knowhow/utility-billing/<topic>.md`.

## Output

Reference skill — no artifacts emitted. Consumers (`/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test`, `/fis-craft`) embed loaded references.

## CHANGELOG

- 1.0.0 — Initial knowhow extraction from FIS EVN project archive.
