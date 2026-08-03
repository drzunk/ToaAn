---
name: fis:fintech-vn
description: "Vietnam fintech integration patterns — Vietcombank QR, NAPAS 247, MISA accounting bridge, Sepay webhooks, e-invoice (HOADON). Activate when integrating with Vietnamese banking/payment systems."
category: backend
keywords: [fintech-vn, fis, vietnam, knowhow]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# fis:fintech-vn — FIS knowhow

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
| `references/e-invoice-hoadon.md` | Vietnamese e-invoice integration |
| `references/napas-247.md` | NAPAS 247 transfer behavior |
| `references/sepay-style-webhooks.md` | Payment webhook handling patterns |
| `references/misa-bridge.md` | MISA accounting bridge integration |
| `references/vietcombank-qr.md` | Vietcombank QR payment integration |

## Project override

Drop project-specific addendum at `<project>/.fis/knowhow/fintech-vn/{topic}.md`. Resolver loads project-local first, then this kit default.

## Output

Reference skill — no artifacts emitted. Consumers embed loaded content into their own artifacts.
