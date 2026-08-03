# Transports

## Landscape

```
DEV (development) → QAS (quality / UAT) → PRD (production)
```

## Transport request types

- **Customizing TR** — IMG config (table content).
- **Workbench TR** — code, screens, reports.
- **Toc / Customer TR** — SAP system patches.

## FIS conventions

- TR description: `<project> - <module> - <feature> - <change>` (e.g. `FRT_BSS_2024 - FI - VAT update - V1 8% reduced`).
- One TR per feature; no "junk TR" with mixed changes.
- Cross-system dependency: workbench TR before customizing TR if config references new code.

## Release / import

- Release in DEV via SE10 → import to QAS automatically (STMS) → manual import to PRD post-UAT.
- Import order matters — STMS queue position.

## Anti-patterns

- TR with deletions of cross-client objects → can't reverse easily.
- Releasing TR before code review → broken QAS environment.
- Hand-editing prod via SCC1 cross-client copy → audit failure.
