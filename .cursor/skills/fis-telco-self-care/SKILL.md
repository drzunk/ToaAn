---
name: fis:telco-self-care
description: "Telco self-care portal/app patterns — account management, top-up, plan-change, subscriber state. Activate when designing customer-facing telco UX.."
category: backend
keywords: [telco-self-care, fis, vietnam, knowhow]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# fis:telco-self-care — FIS knowhow

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
| `references/subscriber-state.md` | Subscriber lifecycle and state transitions |
| `references/top-up.md` | Top-up flows, validation, and failure handling |
| `references/account-mgmt.md` | Account and profile management behavior |
| `references/plan-change.md` | Plan-change eligibility and orchestration |

## Project override

Drop project-specific addendum at `<project>/.fis/knowhow/telco-self-care/{topic}.md`. Resolver loads project-local first, then this kit default.

## Output

Reference skill — no artifacts emitted. Consumers embed loaded content into their own artifacts.
