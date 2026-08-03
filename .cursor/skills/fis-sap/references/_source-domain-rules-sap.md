# Domain Rules — SAP ERP

Khi BA viết PRD cho project SAP ERP customization (FIS đặc thù).

## SAP modules thường gặp

| Module | Tên | Phạm vi nghiệp vụ |
|---|---|---|
| FI | Financial Accounting | Sổ cái, AR/AP, asset accounting |
| CO | Controlling | Cost center, profit center, budget |
| MM | Materials Management | Mua hàng, kho, vendor |
| SD | Sales & Distribution | Bán hàng, billing, shipping |
| PP | Production Planning | MRP, BOM, work order |
| HR/HCM | Human Capital Mgmt | Nhân sự, payroll, time |
| PM | Plant Maintenance | Bảo trì thiết bị |
| QM | Quality Management | QC, inspection |

## Terminology phổ biến (PRD/Story phải dùng đúng)

- **Master data** — dữ liệu cốt lõi (customer, vendor, material)
- **Transaction code (T-code)** — mã giao dịch (vd VA01 tạo sales order)
- **Movement type (mvt type)** — mã di chuyển kho (101 nhập, 261 xuất sản xuất)
- **Posting period** — kỳ kế toán
- **Org structure** — Company code / Plant / Storage location / Sales org
- **Document number range** — số chứng từ tự sinh

## Common BA touchpoints

### 1. Customization PRD typical sections
- Functional requirement: T-code modification, custom field addition, screen variant
- Authorization: role-based (PFCG profile), org level restriction
- Data migration: master data load (LSMW/LTMC), open item migration
- Cutover: go-live checklist, data lock window
- Integration: SAP-to-non-SAP via IDoc/RFC/REST

### 2. Validation rules (FR-style)
- Posting date trong open period
- Document split per profit center
- Vendor invoice 3-way match (PO + GR + IR)
- Tax code mapping per country (VAT VN: V1=10%, V2=5%, V0=0%)

### 3. Compliance specific VN
- Hóa đơn điện tử (e-invoice) phải tích hợp Tổng cục Thuế VN
- Báo cáo thuế GTGT, TNCN, TNDN format theo Thông tư 200
- VAS (Vietnam Accounting Standards) — chart of accounts riêng

## Personas SAP context

| Persona | Role |
|---|---|
| End user | Accountant, Buyer, Sales rep — daily T-code usage |
| Power user | Department key user — UAT + training |
| Functional consultant | Customize config |
| Technical consultant (ABAP) | Develop custom Z-program |
| Basis admin | System admin, transports, performance |
| Authorization admin | PFCG roles, SOD analysis |

## Anti-patterns

- ❌ Custom code khi standard SAP có configuration option
- ❌ Modify standard table thay vì append/extension
- ❌ Hardcode org structure trong custom program
- ❌ Skip transport request → orphan dev system

## Reference
- SAP Help Portal: https://help.sap.com
- SAP Activate methodology
- VN localization: SAP Best Practices Vietnam
