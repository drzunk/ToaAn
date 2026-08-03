---
name: fis:sap
description: "FIS SAP ERP knowhow — modules (FI/CO/MM/SD/PP/HR/PM/QM), S/4HANA Cloud scope items (J58/J62/BFH/1GA/2VA), fit-to-standard methodology, FIS document types (BBKS/SL/BP/FS/TC/Cauhoikhaosat), integration (BAPI/IDoc/RFC/OData/Fiori), master data, authorization, transports, Vietnam localization (VAT, e-invoice, VAS, Thông tư 200, payment terms, asset class). Activate when designing PRD/feature-spec/test-spec for SAP-touching features or extracting TRD from existing ABAP code."
category: backend
keywords: [sap, abap, fi, co, mm, sd, pp, hr, hcm, bapi, idoc, rfc, odata, fiori, s4hana, s4hana-cloud, scope-item, sscui, fit-to-standard, vietnam, vas, e-invoice, thong-tu-200, ricefw, bbks, fs]
license: MIT
argument-hint: "[topic]"
metadata:
  author: fis-ai-kit
  version: "1.1.0"
---

# FIS SAP knowhow

Domain reference for FIS SAP engagements. Activate alongside `/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test` when designing or reverse-engineering SAP-touching features. Captures patterns FIS consultants encounter on Vietnamese SAP ERP / S/4HANA projects.

## When to Apply

Reference these guidelines when:
- Designing a feature that integrates with SAP (any module)
- Extracting TRD from existing SAP / ABAP code (brownfield)
- Choosing between BAPI / IDoc / RFC / OData / Fiori
- Mapping FIS data model to SAP master data (vendor, customer, material, GL account)
- Authorization design (S_TCODE, S_RFC, S_DEVELOP, custom auth objects)
- Vietnam localization: VAT V0/V1/V2, e-invoice (HOADON), VAS chart of accounts

## Topic Categories

| Category | Files | When |
|---|---|---|
| **FIS project artifacts** | `references/fis-document-types.md` | Naming + structure of BBKS / SL / BP / FS / TC / MD / Cauhoikhaosat |
| **S/4HANA Cloud scope items** | `references/s4hana-cloud-scope-items.md` | J58/J62/BFH/1GA/2VA, SSCUI codes, process navigator URLs |
| **Fit-to-Standard workshop** | `references/fit-to-standard-workshop.md` | Cauhoikhaosat structure, L2/L3 questions, BBKS workflow, common decision points |
| Modules | `references/modules-*.md` | Module-specific business behavior |
| Integration | `references/integration-*.md` | Inter-system data exchange |
| Master data | `references/master-data.md` | Vendor / customer / material / GL keys + FIS coding (Plant, Sloc, BP Group, Asset Class) |
| Authorization | `references/authorization.md` | Role design, S_* auth objects |
| Transports | `references/transports.md` | DEV → QAS → PRD movement |
| VN localization | `references/vn-localization.md` | VAT, e-invoice, VAS Thông tư 200, payment terms, reversal reasons, currency types |
| Anti-patterns | `references/anti-patterns.md` | Known pitfalls |
| **FS document authoring** | `references/fs-template-structure.md`, `references/fs-section-templates.md`, `references/fs-example-asset-depreciation.md`, `references/fs-example-asset-barcode.md` | When BA/SA writes FS doc for SAP custom development (RICEFW) — full FIS Word template + worked Cloud Public Edition example |
| Glossary | `references/sap-terminology-vn.md` | Vietnamese ↔ English SAP terms (incl. S/4HANA Cloud + RICEFW) |

## Quick Reference

### Modules

- `references/modules-fi-co.md` — Financial Accounting + Controlling (sổ cái, AR/AP, cost center, profit center)
- `references/modules-mm.md` — Materials Management (mua hàng, kho, vendor master, 3-way match)
- `references/modules-sd.md` — Sales & Distribution (bán hàng, billing, shipping, customer master)

### Integration

- `references/integration-bapi.md` — BAPI (sync; common for sync transactions)
- `references/integration-idoc.md` — IDoc (async; partner profile, ALE)
- `references/integration-rfc.md` — RFC (legacy sync; tRFC / qRFC variants)
- `references/integration-odata-fiori.md` — OData on S/4HANA + Fiori frontend binding

### Vietnam localization

- `references/vn-localization.md` — VAT codes (V1=10%, V2=5%, V0=0%), e-invoice integration with Tổng cục Thuế, VAS chart of accounts, Thông tư 200 reports

### Anti-patterns

- `references/anti-patterns.md` — "BAPI without COMMIT WORK", "IDoc with ad-hoc partner profile", "OData without server-side paging", "T-code modification breaks transports"

## Project override

Drop project-specific addendum at `<project>/.fis/knowhow/sap/<topic>.md`. The resolver loads project-local first, then this kit default.

## Output

This is a **reference** skill — output is contextual prose loaded into the session. No artifacts written. Consumers (`/fis-outcome`, `/fis-requirements`, `/fis-plan`, `/fis-test`, `/fis-craft`) embed loaded references into their generated artifacts.

## CHANGELOG

- 1.1.0 — Add `fis-document-types.md`, `s4hana-cloud-scope-items.md`, `fit-to-standard-workshop.md`, `fs-example-asset-barcode.md`. Enrich `vn-localization.md` (5 reversal reasons, payment terms, FX types, COA buckets, Asset Class TT200 mapping, custom report list, master data coding rules), `master-data.md` (Plant/Sloc/Storage Type/BP Group naming), `sap-terminology-vn.md` (S/4HANA Cloud terms, RICEFW). Pattern abstraction generalized — no customer-identifying examples.
- 1.0.0 — Initial knowhow extraction from FIS-SAP project archive.
