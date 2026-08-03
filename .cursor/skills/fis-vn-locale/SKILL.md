---
name: fis:vn-locale
description: "Vietnam locale rules — CCCD/CMND/MST validation, address (province→district→ward), VN phone (+84), VND currency, NAPAS/QR. Activate when handling Vietnamese identity, address, payment data."
category: backend
keywords: [vn-locale, fis, vietnam, knowhow]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# fis:vn-locale — FIS knowhow

Thin reference skill — captures FIS domain expertise as loadable references. Activate alongside `/fis-outcome`, `/fis-requirements`, `/fis-plan` when the topic matches.

## When to Use

- Designing or reverse-engineering features that touch this domain
- Choosing patterns / data formats for this domain
- Validating FIS-internal compliance (locale, regulatory)

## When NOT to Use

This is a **reference** skill. It does not write artifacts. Consumers (`/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test`, `/fis-craft`) embed loaded references into their own outputs.

## Topics

References live in `references/` and follow the canonical `{topic}-workflow.md` naming where the file describes how to apply that topic. For pure reference (no workflow), files use `{topic}.md`.

| File | Content |
|---|---|
| `references/address-province-district-ward.md` | Vietnamese administrative address structure |
| `references/phone-format.md` | Vietnamese phone-number normalization and validation |
| `references/cccd-cmnd-mst-validation.md` | CCCD, CMND, and tax-code validation |
| `references/napas-qr.md` | NAPAS QR payload and integration rules |
| `references/vnd-currency.md` | VND formatting, rounding, and amount handling |

## Project override

Drop project-specific addendum at `<project>/.fis/knowhow/vn-locale/{topic}.md`. Resolver loads project-local first, then this kit default.

## Output

Reference skill — no artifacts emitted. Consumers embed loaded content into their own artifacts.
