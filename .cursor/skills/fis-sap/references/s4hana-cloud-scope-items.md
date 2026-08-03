# SAP S/4HANA Cloud — Scope Items (Best Practices)

SAP S/4HANA Cloud Public Edition delivers pre-configured business processes as **scope items**. Each scope item has a 3-character ID, BPMN flow, transaction list, and step-by-step guide on `me.sap.com/processnavigator`.

## Why FIS engagements care

Vietnamese SAP cloud projects scope by selecting these items in the project's solution package, then build BP (Business Process) docs around them. Custom dev (FS / RICEFW) only happens where standard scope items don't fit.

**Rule:** Default to standard scope item; FS only when `Fit-to-Standard` workshop confirms gap.

## Common scope items (Public Cloud Edition)

### Finance

| ID | Name | Module | Notes |
|---|---|---|---|
| `J58` | Accounting and Financial Close | FI | Document Splitting, GL accounts categorization (Revenue / Expense / Bank / Cash / Balance Sheet) |
| `1GA` | Group Ledger – IFRS | FI | Parallel ledger for IFRS reporting |
| `2VA` | Group Ledger – Local GAAP (VN VAS) | FI | Vietnam-specific accounting principle |
| `J62` | Asset Accounting | AA | Capitalization, depreciation, retirement, valuation, period-end |
| `BFH` | Asset Under Construction (AuC) | AA | XDCBDD 241 in VN COA |
| `1GB` | Asset Accounting – Group Ledger IFRS | AA | Parallel depreciation for IFRS |
| `1GF` | AuC – Group Ledger IFRS | AA | Parallel AuC for IFRS |
| `2QY` | SAP Fiori Analytical Apps for Asset Accounting | AA | Pre-built Fiori dashboards |

### Procure / Supply

| ID | Name | Module | Notes |
|---|---|---|---|
| `J45` | Procurement of Direct Materials | MM | PO → GR → IR (3-way match) |
| `BMD` | Procurement of Services | MM | Service POs |
| `1Z2` | Stock Handling | MM-IM | Movement types, warehouse |

### Sell

| ID | Name | Module | Notes |
|---|---|---|---|
| `BD9` | Sell from Stock | SD | Sales order → delivery → billing |
| `BKZ` | Free of Charge Delivery | SD | Sample shipments |

### Make

| ID | Name | Module | Notes |
|---|---|---|---|
| `BJE` | Make-to-Stock Production | PP | MRP, work order, confirmation |
| `BJ8` | Make-to-Order Production | PP | KMAT, sales-order-driven |

## SSCUI (Self-Service Configuration UI)

S/4HANA Cloud doesn't expose IMG (SPRO). Configuration goes through Fiori-based SSCUI apps, each identified by a 6-digit code:

| SSCUI | Topic |
|---|---|
| `100002` | Define Company Code |
| `103556` | Assign Accounting Principles to Ledgers and Company Codes |
| `120100` | Document Splitting Characteristics |
| `100039` | Define Profit Centers |
| `100037` | Define Cost Centers |
| `101920` | Define Tax Codes for Sales/Purchases |
| `100147` | Define Asset Classes |
| `100143` | Specify Depreciation Areas |

Reference SSCUI ID in BP / SL docs; customer's IT or FPT consultant configures via Fiori.

## Process navigator URLs

```
https://me.sap.com/processnavigator/SolS/<solution-id>/<release>/SolP/<scope-id>?region=<DE|US|...>
```

Concrete example (Asset Accounting on release 2602):
- `https://me.sap.com/processnavigator/SolS/EARL_SolS-013/2602/SolP/J62?region=DE`

Region defaults to `DE` for navigator metadata; actual VN tenant uses VN region.

## Scope-item-driven BP authoring pattern

For each scoped item:
1. Open process navigator → download official BPMN PDF
2. Create section in BP doc:
   - **Mục đích** — translate from process navigator
   - **Phạm vi áp dụng** — list company codes / plants
   - **Hạch toán** — list accounting entries with VN account numbers (711, 627, 213, 214, ...)
   - **Sơ đồ quy trình nghiệp vụ** — embed PDF or refer to URL
   - **Mô tả quy trình** — step-by-step in VN, link to T-code / Fiori App per step
3. Cite scope-item link
4. Note delta from standard (if any) → triggers FS doc

## Cloud vs On-Premise differences

| Aspect | S/4HANA Cloud Public | S/4HANA Cloud Private | S/4HANA On-Premise |
|---|---|---|---|
| Customization | SSCUI only, no SPRO | Limited SPRO | Full SPRO |
| Custom code (ABAP) | In-app extensibility (Cloud SDK) | Side-by-side (BTP) only for restricted classes | Full classic ABAP |
| Upgrades | Quarterly automatic | Quarterly with sandbox | On-demand |
| RICEFW scope | Restricted (Reports OK via CDS, custom Forms via Adobe Forms, Interfaces via API/iFlow) | Wider | Unlimited |
| Document type for custom | FS (must justify why standard doesn't work) | FS | FS |

For Public Edition projects, FS docs MUST cite which standard scope item was insufficient and why (e.g., Asset BARCODE FS — no standard barcode label print; built via Fiori app + I_FIXEDASSET CDS View — see `references/fs-example-asset-barcode.md`).

## Release versioning

| Release | Calendar |
|---|---|
| 2602 | Q1 2026 |
| 2508 | Q3 2025 |
| 2502 | Q1 2025 |

Cite release in BP/FS docs when behavior depends on quarterly feature.

## Cross-reference

- FIS document types: `references/fis-document-types.md`
- FS template: `references/fs-template-structure.md`
- Asset BARCODE worked FS: `references/fs-example-asset-depreciation.md`
- Fit-to-standard workshop: `references/fit-to-standard-workshop.md`
