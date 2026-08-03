---
name: fis:mvno
description: "MVNO architecture knowhow — HLR/HSS, BSS/OSS layering, MVNE handoff, charging, SIM/IMSI lifecycle. Activate when designing or reverse-engineering MVNO platform features."
category: backend
keywords: [mvno, fis, vietnam, knowhow]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.0.0"
---

# fis:mvno — FIS knowhow

Thin reference skill — captures FIS domain expertise as loadable references. Activate alongside `/fis-outcome`, `/fis-plan` when the topic matches.

## When to Use

- Designing or reverse-engineering features that touch this domain
- Choosing patterns / data formats for this domain
- Validating FIS-internal compliance (locale, regulatory)

## When NOT to Use

This is a **reference** skill. It does not write artifacts. Consumers (`/fis-outcome`, `/fis-plan`, `/fis-test`, `/fis-craft`) embed loaded references into their own outputs.

## Topics

References live in `references/` and follow the canonical `{topic}-workflow.md` naming where the file describes how to apply that topic. For pure reference (no workflow), files use `{topic}.md`.

| File | Content |
|---|---|
| `references/architecture.md` | MVNO architecture and service boundaries |
| `references/bss-oss-layering.md` | BSS/OSS responsibilities and integration layers |
| `references/mvne-handoff.md` | MVNE handoff contracts and ownership |
| `references/sim-imsi-lifecycle.md` | SIM and IMSI lifecycle management |
| `references/hlr-hss.md` | HLR/HSS provisioning and synchronization |

## Project override

Drop project-specific addendum at `<project>/.fis/knowhow/mvno/{topic}.md`. Resolver loads project-local first, then this kit default.

## Output

Reference skill — no artifacts emitted. Consumers embed loaded content into their own artifacts.
