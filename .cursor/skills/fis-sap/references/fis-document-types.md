# FIS SAP Project Document Types

Document type taxonomy used in FIS SAP engagements.

## Naming convention

```
<COMPANY>_SAP_<YEAR>_<PHASE>_<TYPE>_<TOPIC>[_v<MAJOR>.<MINOR>].<ext>
```

Generic examples:
- `<COMPANY>_SAP_<YEAR>_PM_BBKS_FICO_v1.0.docx` — biên bản khảo sát FICO
- `<COMPANY>_SAP_<YEAR>_PM_SL_KTHT_v2.1.docx` — solution kế toán hệ thống tổ chức
- `<COMPANY>_SAP_<YEAR>_PM_BP_AA_v2.0.docx` — business process Asset Accounting
- `<COMPANY>_SAP_<YEAR>_PM_FS_<feature>_v0.1.docx` — functional spec for one RICEFW item
- `<COMPANY>_SAP_<YEAR>_PM_TC_Testscript_<module>_v1.0.xlsx` — test script per module
- `<COMPANY>_SAP_<YEAR>_PM_Cauhoikhaosat_<module>_v1.0.xlsx` — fit-to-standard questionnaire

| Token | Meaning |
|---|---|
| `<COMPANY>` | Customer short code (typically 3-letter) |
| `<YEAR>` | Fiscal year of contract |
| `<PHASE>` | `PM` (Prepare), `EX` (Explore), `RZ` (Realize), `DE` (Deploy), `RU` (Run) — SAP Activate |
| `<TYPE>` | Document type code (table below) |
| `<TOPIC>` | SAP module + functional scope, e.g., `FICO`, `AA`, `KTHT`, `<feature-slug>` |
| `_v<MAJOR>.<MINOR>` | Version (optional for templates) |

## Document type codes

| Code | Vietnamese | English | Format | Owner | When |
|---|---|---|---|---|---|
| `BBKS` | Biên bản khảo sát | Survey minutes | docx | BA / SA | Phase Prepare — workshop output |
| `SL` | Solution document | Solution architecture | docx | SA | Phase Explore — system org structure + master data design |
| `BP` | Business Process | To-be process flow | docx | SA + BA | Phase Explore — per scope item, process steps + accounting entries |
| `FS` | Functional Spec | Custom dev spec | docx | SA → DEV | Phase Realize — only for RICEFW (Reports / Interfaces / Conversions / Enhancements / Forms / Workflow) |
| `TC` (Testscript) | Kịch bản kiểm thử | Test script | xlsx | Tester | Phase Realize — execute against scope item |
| `MD` | Master Data | Master data sheet | xlsx | BA + SA | Phase Explore — Cost Center, Profit Center, COA, Asset Class lists |
| `Cauhoikhaosat` | Câu hỏi khảo sát | Configuration questionnaire | xlsx | SA Pre-sales → Customer | Phase Prepare — fit-to-standard prep, L2/L3 questions |
| `KTHT` | Kế toán + Hệ thống tổ chức | Org structure for accounting | suffix | SA | Combine FI + CO + AA + AR/AP master data design |

## SAP Activate phase mapping

```
Prepare       → BBKS, Cauhoikhaosat
Explore       → SL, BP, MD
Realize       → FS (RICEFW only), TC, Test data
Deploy        → Cutover plan, hypercare runbook
Run           → BAU support, change requests
```

Each phase has a gate review aligned to acceptance criteria and tests/CI.

## BBKS (Biên bản khảo sát) — survey minutes structure

Header table:
- Dự án (project name)
- Mã hiệu dự án (project code)
- Công việc (workshop topic)
- Thời gian (date/time)
- Địa điểm (location)
- Thành phần tham dự (attendees: customer + FPT)
- Tiêu đề (title)

Body:
- Mục tiêu (objectives)
- Nội dung khảo sát — table 4 cols: STT | Chủ đề | Câu hỏi | Trao đổi nghiệp vụ
- Kết luận (conclusions)
- Action items table — STT | Tài liệu cần cung cấp / vấn đề tồn đọng | Người thực hiện | Thời hạn

Typical FICO survey scope: ~40-50 questions covering Kế toán tổng hợp, Kế toán phải thu/phải trả, Kế toán quản trị, Kế toán tài sản, Cấu trúc tổ chức, Thiết lập chung, Báo cáo. Close with action item table (deliverables / blockers + owner + deadline).

## SL (Solution) — design document structure

1. Tổng quan (Mục tiêu + Thuật ngữ)
2. Cơ cấu tổ chức
   - Kế toán tài chính / kế toán quản trị (Company Code, Profit Center, Cost Center)
   - Kho (Plant, Storage Location, Sloc naming rules)
   - Kho chi tiết (Warehouse Number, Storage Type, Storage Section, Storage Bin)
   - Mua hàng
   - Bán hàng (Sales Org, Distribution Channel, Division, Sales Area)
3. Danh mục dùng chung
   - GL: bộ sổ, COA, Currencies, Exchange Rate, Document Type
   - AR/AP: Business Partner Group, Bank Key, House Bank, Payment Method, Payment Term, Tax Code
   - AA: Asset Class, Transaction Type
   - CO: Profit Center / Cost Center hierarchy
   - MM: Material Type, Material Group, UOM, PR/PO Type, Movement Type
   - WM: stock type, document type, putaway/picking strategy

## BP (Business Process) — to-be process structure

Per SAP scope item (e.g., J62 — Asset Accounting):
- 2.1 Scope item name (e.g., J62 — Asset Accounting)
- 2.1.1 to 2.1.N: sub-processes
  - Mục đích (purpose)
  - Phạm vi áp dụng (scope)
  - Hạch toán (accounting entries) — only if FI-related
  - Sơ đồ quy trình nghiệp vụ (BPMN flow) — link to SAP Best Practices
  - Mô tả quy trình (step-by-step)

Typical Asset Accounting BP scope item set (J62 family):
- J62-00 Asset Acquisition Through Direct Capitalization
- J62-01 Master Data
- J62-02 Acquisition (no PO)
- J62-03 Retirement
- J62-04 Valuation
- J62-05 Month-End-Closing
- J62-06 Year-End-Closing
- BFH Asset Under Construction
- 1GB Group Ledger IFRS
- 1GF AuC Group Ledger IFRS
- 2QY SAP Fiori Analytical Apps for Asset Accounting

Each sub-process links to: `https://me.sap.com/processnavigator/SolS/EARL_SolS-013/<release>/SolP/<scope-id>?region=DE`

## FS (Functional Spec) — RICEFW custom dev structure

1. Tổng quan
   - Mục đích tài liệu
   - Phạm vi tài liệu
   - Tài liệu tham chiếu
   - Thuật ngữ viết tắt
2. Mô tả yêu cầu
   - Mục đích
   - Quy trình nghiệp vụ liên quan
   - Các giả định / Rủi ro / Ràng buộc
   - Mô tả yêu cầu nghiệp vụ
   - Format báo cáo/chứng từ (font, size, paper, format)
   - Hình thức báo cáo/chứng từ (form sample)
3. Mô tả thiết kế chức năng
   - Thông tin chung (T-code/Fiori App standard, T-code/Fiori App customized, Access path, Trigger)
   - Các bảng dữ liệu cần phát triển (custom tables, CDS Views)
   - Tham số lọc (parameters: name, EN/VN description, condition, default value, required, note)
   - Các màn hình chức năng (screen layout, form layout)
   - Nội dung chi tiết báo cáo/chứng từ (column-by-column spec)
   - Xử lí lỗi / trường hợp ngoại lệ
   - Bảo mật, phân quyền (S_TCODE, S_RFC, S_DEVELOP, custom authorization objects)
   - Test scenario / Test data

Worked example: see `references/fs-example-asset-barcode.md` (Asset BARCODE print custom Fiori app using `I_FIXEDASSET` CDS View on S/4HANA Cloud Public Edition).

## TC (Test script) — execution structure

XLSX with sheets:
- `TrangBia-Ky-ThayDoi` — cover, signoff, change history
- `Huong dan` — usage instructions
- `Danh muc` — index of all test scenarios + status tracking
- One sheet per test scenario named after scope item: e.g., `J62.01-01`, `J62.02-02, 1GB-01`, `BFH-01, 1GF-01`

Per-scenario columns (FIS standard test script layout):
- Mã bước quy trình (process step ID, traces back to BP doc)
- Mô tả bước
- Vai trò người thực hiện
- T-code / Fiori App
- Dữ liệu test
- Kết quả mong đợi
- Kết quả thực tế
- Trạng thái (Pass / Fail / Blocked / Not run)
- Bug ID (if Fail)

Cover signoff:
- FPT roles: Người tạo, Người xem xét (×2), Người xét duyệt (project manager)
- Customer roles: Người xem xét (Key User), Người xem xét (Process Owner), Người xét duyệt (Project Manager / IT Lead)

## When to use which type

| Need | Use |
|---|---|
| Survey workshop output | BBKS |
| Pre-workshop questionnaire | Cauhoikhaosat |
| System org structure (CC, PC, plants, sales orgs) | SL_KTHT |
| Per scope item to-be process | BP |
| Custom dev (RICEFW) ABAP/CDS/Fiori work | FS |
| Test execution evidence | TC_Testscript |
| Master data list (CC, PC, COA, BP, AC) | MD (xlsx, separate per master data type) |

## Cross-reference

- FS template structure: see `references/fs-template-structure.md`
- FS section templates: see `references/fs-section-templates.md`
- FS worked examples: `references/fs-example-asset-depreciation.md`, `references/fs-example-asset-barcode.md`
