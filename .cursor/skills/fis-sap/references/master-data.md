# Master Data

## Core objects

| Object | Key length | Levels |
|---|---|---|
| Material | 18 chars (MATNR) | client / plant / storage location |
| Vendor | 10 chars (LIFNR) | client / company code / purchasing org |
| Customer | 10 chars (KUNNR) | client / company code / sales area |
| Business Partner | 10 chars (BU_PARTNER) | client / role-based (S/4HANA replaces customer/vendor) |
| GL account | 10 chars (SAKNR) | client / company code |
| Cost center | 10 chars (KOSTL) | controlling area |
| Profit center | 10 chars (PRCTR) | controlling area |
| Asset | NNNNNNNN-NNNN (ANLN1-ANLN2) | company code |

## Hierarchy / inheritance

- Client-level data (e.g. material description) shared across company codes.
- Company-code data (e.g. payment terms) maintained per company code.
- Sales-area data (sales org × distribution channel × division) for SD.

## Deletion semantics

- Master data is never deleted — flagged for deletion (LVORM = 'X').
- Archive run removes flagged records after retention.
- Reactivation = clear deletion flag (no recreate).

## Creation patterns at FIS

- LSMW (legacy) — script-based migration; deprecated.
- LTMC / Migration Cockpit (S/4HANA) — modern; XML / Excel templates.
- Custom Z program calling BAPI in batch — only if standard tools insufficient.

## Master data delivery sheets

FIS engagements deliver MD as separate XLSX per type:
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Cost_Center_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Profit_Center_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_COA_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Asset_Class_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Material_Group_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Vendor_v<X>.xlsx`
- `<COMPANY>_SAP_<YEAR>_<PHASE>_MD_Customer_v<X>.xlsx`

Each MD sheet shipped as part of SL (Solution) doc package — section 3 (Danh mục dùng chung) references the file name.

## FIS coding conventions

### Company Code

Max 4 chars. Group-aware coding pattern: `<GROUP-2-DIGIT><ENTITY-2-DIGIT>` (e.g., group `XX` + entity `YY` → `XXYY`).

### Plant (4 chars)

```
XXXY
```
- `XXX` = first 3 chars of Company Code (e.g., `671`)
- `Y` = digit (1..N) for production plants, OR `K` for non-valuation warehouse

### Storage Location / Sloc (4 chars)

```
XYYY
```
- `X` = goods type prefix:
  - `1` = VBC (vật bao bì / packaging)
  - `2` = NVL (nguyên vật liệu / raw material)
  - `3` = CCDC (công cụ dụng cụ / tools)
  - `4` = BTP (bán thành phẩm / WIP)
  - `5` = TP (thành phẩm / finished goods)
  - `9` = non-valuation warehouse
- `YYY` = 3-digit sequence starting `000`

### Warehouse Number (3 chars, EWM/WM)

Same convention as Plant for the geographical area, since each plant typically has 1 warehouse number.

### Storage Type (4 chars)

```
XXYY
```
- `XX` = first 2 chars of Sloc
- `YY` = 2-digit sequence per storage type variant

Plus standard interim storage types for receiving/shipping/QI/returns.

### Profit Center (max 10 chars)

Project-defined; typically functional grouping (R&D / Sales / Support / Production lines).

### Cost Center (max 10 chars)

Project-defined; typically by department.

### Material number

S/4HANA allows up to 40 chars (MATNR40). Default to 18 unless customer wants longer for legacy compatibility.

### Asset (master)

ANLN1 / ANLN2 — main asset number + sub-number. Sub-number used for capitalization in tranches (e.g., AuC partial settlements).

## Business Partner Groups (BP_GROUP)

S/4HANA replaces customer / vendor with unified Business Partner. FIS standard 5-group pattern:

| Code | VN name | Numbering | Use |
|---|---|---|---|
| Z001 | Trong nước | Auto | Domestic vendors / customers |
| Z002 | Nước ngoài | Auto | Foreign vendors / customers |
| Z003 | Vãng lai | Auto | One-time partners |
| Z004 | Nội bộ | = Company Code | Intercompany |
| Z005 | Nhân viên | = Customer-prefix + employee ID | Employee BP |

Note: VN customers often have 3-digit employee IDs — pad with leading zeros to 4 (e.g., `<PREFIX>0XYZ`) for SAP consistency across the BP namespace.

## Asset Class (FIS Vietnam standard)

Aligned with Thông tư 200, 99, 96:

| Asset Class | VN name | TT account |
|---|---|---|
| AS6271 | TSCĐ phục vụ sản xuất | 627 |
| AS6411 | TSCĐ phục vụ bán hàng | 641 |
| AS6421 | TSCĐ phục vụ quản lý | 642 |
| AS2110 | Nhà cửa, vật kiến trúc | 211 |
| AS2120 | Máy móc, thiết bị | 212 |
| AS2130 | Phương tiện vận tải | 213 |
| AS2140 | Thiết bị, dụng cụ quản lý | 214 |
| AS241 | XDCBDD (AuC) | 241 |
| AS153 | CCDC trong kho | 153 |

(Skip 215 / 217 for new projects unless customer specifically uses them.)

Each asset class has:
- Account determination (G/L for acquisition / depreciation / retirement)
- Depreciation key (DK) — straight-line by day / by month / no depreciation
- Useful life range
- Screen layout

## Master data anti-patterns

- Copy material master across plants without checking views config (sales view, purchasing view) → incomplete master.
- Vendor created in pre-prod with same number as prod → cutover collision.
- Inventing custom asset class outside Thông tư 200 → tax audit pain.
- Skipping `house bank` master for one-off accounts → AP run fails.
- Using free-text vendor name instead of BP master → cannot consolidate spend reporting.

## Cross-reference

- VN-specific master data values (VAT codes, exchange rates, payment terms): `references/vn-localization.md`
- Module-specific master: `references/modules-fi-co.md`, `references/modules-mm.md`, `references/modules-sd.md`
- BP / customer / vendor migration: `references/integration-bapi.md` (BAPI_BUPA_*)
