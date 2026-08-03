---
name: fis:bss-billing
description: "FIS BSS billing knowhow — postpaid / prepaid / convergent billing patterns, mediation, rating, charging, invoicing, dunning, revenue assurance. Activate when designing or reverse-engineering telco billing features.."
category: backend
keywords: [bss, billing, postpaid, prepaid, convergent, mediation, rating, charging, invoice, dunning, revenue-assurance, telco]
license: MIT
argument-hint: "[topic] (e.g. \"prepaid mediation\", \"convergent rating\")"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# fis:bss-billing — FIS BSS billing knowhow

Domain knowhow for FIS BSS (Business Support System) billing engagements. Activate alongside `/fis-outcome`, `/fis-plan` when designing billing-touching features.

## When to Use

- Telco billing feature design / reverse engineering
- Postpaid vs prepaid vs convergent decisions
- Mediation: CDR ingestion, deduplication, normalization
- Rating: tariff plans, bundles, free-units, fair-usage
- Invoicing: cycle, proration, dunning
- Revenue assurance / leakage analysis

## Topics

| File | Content |
|---|---|
| `references/postpaid-vs-prepaid.md` | When each is right, hybrid (convergent) patterns |
| `references/mediation.md` | CDR formats (ASN.1, CSV, XML), deduplication keys, late records |
| `references/rating-engine.md` | Tariff structure: peak/off-peak, calling-circle, bundles, FUP |
| `references/charging.md` | Real-time vs offline charging, Diameter Gy, OCS integration |
| `references/invoicing.md` | Cycle ladder, proration mid-cycle, taxes, e-invoice VN |
| `references/dunning.md` | Dunning levels, suspension thresholds, write-off rules |
| `references/revenue-assurance.md` | Leakage detection: CDR ↔ rating ↔ invoice reconciliation |
| `references/data-models.md` | Subscriber, account, product, plan, charge, invoice — typical schemas |

## Project override

`<project>/.fis/knowhow/bss-billing/{topic}.md` overrides this kit's defaults.

## Output

Reference skill — no artifacts emitted. Consumers (`/fis-outcome`, `/fis-plan`) embed into their PRD / SOD / DDD / feature-spec.

## CHANGELOG

- 1.0.0 — Initial knowhow extraction from FIS BSS project archive.
