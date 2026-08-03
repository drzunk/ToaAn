# Vietnam Localization

## VAT codes

| Code | Rate | Use |
|---|---|---|
| V0 | 0% | export, certain services |
| V1 | 10% | standard rate |
| V2 | 5% | reduced rate (essential goods) |
| V3 | 8% | temporary reduction (Nghị quyết 43/2022/QH15, Nghị quyết 110/2023/QH15 — 2024-2026) |
| ZZ | Không chịu thuế | non-taxable supplies |

VAT đầu vào (Input) tách 3 nhóm:
- HHDV nội địa (domestic goods/services)
- HHDV nhập khẩu (imported goods/services)
- Tài sản (fixed assets)

VAT đầu ra (Output) tách 2 nhóm:
- HĐ SXKD (production / business invoice)
- Hàng bán bị trả lại, giảm giá hàng bán (returns, discounts)

## E-invoice (Hóa đơn điện tử)

- Mandatory since 1 July 2022 for most businesses (Nghị định 123/2020/NĐ-CP, Thông tư 78/2021/TT-BTC).
- Integration with Tổng cục Thuế: `https://hoadondientu.gdt.gov.vn`.
- Format: XML signed with company digital certificate.
- Statuses: draft → sent → accepted by tax → reported.
- Reverse / replace via formal "hóa đơn thay thế" or "hóa đơn điều chỉnh".

## VAS (Vietnam Accounting Standards)

- Chart of accounts per Thông tư 200/2014/TT-BTC.
- Different from US GAAP / IFRS — local FIS implementations need parallel ledger if reporting both.
- Báo cáo tài chính theo VAS: BS (CĐKT), IS (KQHĐKD), CF (LCTT), Equity (TĐVCSH).

### Common VN COA buckets (Thông tư 200)

| Range | Group |
|---|---|
| 1xx | Tài sản ngắn hạn (current assets) |
| 2xx | Tài sản dài hạn (long-term assets) — TSCĐ, BĐS đầu tư |
| 3xx | Nợ phải trả |
| 4xx | Vốn chủ sở hữu |
| 5xx | Doanh thu |
| 6xx | Chi phí (621-642) |
| 7xx | Thu nhập khác |
| 8xx | Chi phí khác |
| 9xx | Xác định kết quả kinh doanh, ngoại bảng |

Asset Class binding (FIS standard pattern, Thông tư 200/99/96):
- Tách asset class theo nhóm chi phí 627 (sản xuất), 641 (bán hàng), 642 (quản lý)
- Bỏ TS đầu tài khoản 215, 217 (legacy/obsolete groups for new projects)
- 241 — XDCBDD (Xây dựng cơ bản dở dang / AuC) → maps to scope item BFH

### Currency types (S/4HANA Cloud, default 3 types)

| Type | Use | Typical VN value |
|---|---|---|
| Transaction currency | Loại tiền tệ của giao dịch | per transaction |
| Company Code currency | Loại tiền tệ quy đổi ra theo quốc gia sở tại | VND |
| Global currency | Loại tiền tệ kế toán quản trị cấp tập đoàn | VND (or USD if multi-country group) |

### Exchange rate types (FIS proposed standard)

| Type | Purpose |
|---|---|
| `M` | Tỷ giá bình quân giao dịch hàng ngày (daily average for transactions) |
| `Z1` | Tỷ giá đánh giá cuối kỳ (period-end revaluation) |

Default — set to **Classic Foreign Currency Valuation** (not Advance) — hạch toán chênh lệch và ghi âm lại bút toán vào đầu kỳ sau.

### Reversal Reasons (FIS standard 4 types)

| Code | Meaning |
|---|---|
| 01 | Hủy ghi âm cùng kỳ (negative reversal in same period) |
| 02 | Hủy ghi đảo cùng kỳ (offset reversal in same period) |
| 03 | Hủy ghi âm khác kỳ (negative reversal across periods) |
| 04 | Hủy ghi đảo khác kỳ (offset reversal across periods) |

### Payment methods

| Method | VN name | Note |
|---|---|---|
| Tiền mặt | Cash | T-account 111 |
| Chuyển khoản | Bank transfer | T-account 112 (default), or 113 (in-transit) — typically split to enable 113 routing |
| Tín dụng | Credit | |
| L/C | Letter of Credit | International trade |

Hạch toán quy ước:
- Phải thu (Receivable) → đi thẳng qua 112, không qua 113
- Phải trả (Payable) → tách payment method chuyển khoản để route qua cả 113 và 112

### Payment terms (FIS standard)

Phải thu / phải trả: thanh toán ngay, 15, 30, 45, 60, 90 ngày.

### Posting Period Variant

Default: Posting Period Variant = Mã Company Code.

### Fiscal Year Variant

Default: K4 (calendar year, 12 periods Jan-Dec). Custom variants only if customer needs non-calendar fiscal year.

## Tax reports

- Báo cáo thuế GTGT (VAT) — monthly.
- Báo cáo thuế TNCN (PIT) — monthly + annual.
- Báo cáo thuế TNDN (CIT) — quarterly + annual.
- Báo cáo SD hóa đơn — quarterly.

## Standard custom reports per Thông tư 200 (FIS deliverables)

Common list customers request as custom dev (each typically becomes one FS doc):

| Report | VN | Module |
|---|---|---|
| Phiếu thu | Cash receipt voucher | FI |
| Phiếu chi | Cash payment voucher | FI |
| Phiếu kế toán | Accounting voucher | FI |
| Sổ quỹ tiền mặt | Cash book | FI |
| Báo cáo tình hình tăng giảm TSCĐ | Asset movement report | AA |
| Bảng kê VAT đầu vào | Input VAT list | FI |
| Bảng kê VAT đầu ra | Output VAT list | FI |
| Sổ chi tiết tài khoản | GL detail report | FI |
| Báo cáo tổng hợp công nợ phải thu/phải trả | AR/AP aging summary | FI |
| Sổ chi tiết công nợ | AR/AP detail | FI |
| Bảng cân đối số phát sinh | Trial balance | FI |
| Báo cáo kết quả kinh doanh (KQKD) | Income Statement | FI |
| Báo cáo tình hình tài chính | Balance Sheet | FI |
| Báo cáo lưu chuyển tiền tệ | Cash Flow (gián tiếp / indirect method) | FI |

## Master data coding (FIS naming conventions)

### Plant code (4 chars)

Quy tắc 1 — kho thông thường: `XXX-Y`
- `XXX` = 3 ký tự đầu của Company Code
- `Y` = số thứ tự tăng dần theo nhà máy (bắt đầu từ 1)

Quy tắc 2 — kho không giá trị: `XXX-K`
- `XXX` = 3 ký tự đầu Company Code
- Always `K`

### Storage Location code (Sloc, 4 chars)

Quy tắc 1 — kho thông thường: `X-YYY`
- `X` = mã loại hàng hóa: 1=VBC (vật bao bì), 2=NVL (nguyên vật liệu), 3=CCDC (công cụ dụng cụ), 4=BTP (bán thành phẩm), 5=TP (thành phẩm)
- `YYY` = 3 số bắt đầu 000, tăng dần

Quy tắc 2 — kho không giá trị: `9-YYY`

### Storage Type (Warehouse Management, 4 chars)

Format: `XX-YY`
- `XX` = 2 ký tự đầu của Sloc
- `YY` = STT tăng dần per loại storage type

Có **interim storage type** (virtual) cho:
- Receiving area
- Shipping area
- Quality inspection
- Returns

### Vendor / Customer Group (Business Partner Group)

FIS standard 5-nhóm pattern:
1. Trong nước (domestic) — auto-numbering
2. Nước ngoài (foreign) — auto-numbering
3. Vãng lai (one-time) — auto-numbering
4. Nội bộ (intercompany) — code = Company Code
5. Nhân viên (employee) — code = customer-prefix + employee ID, pad shorter IDs with leading zeros

### Profit Center

Max 10 chars (alphanum). Naming rule project-specific.
Typical VN manufacturer pattern: 1 PC per business block (R&D, sản xuất, kinh doanh, hỗ trợ) + 1 PC per production line (Phòng sản xuất 1..N).

### Cost Center Categories (SAP defaults usually retained)

- 9: Marketing
- C: Professional Service
- E: Development
- F: Production
- G: Logistics
- H: Service cost center
- L: Management
- M: Material
- S: Social
- V: Sales
- W: Administration

### Segment

For segment reporting (báo cáo theo Segment). Naming pattern: `<group-code>_<segment-letter>` (e.g., `<XXXX>_A`, `<XXXX>_B` for multiple segments).

## Anti-patterns

- Single ledger for VAS + IFRS → reconciliation hell at year-end. Use parallel ledgers (1GA for IFRS + 2VA for VAS).
- Hard-coding VAT rate in custom code → breaks every legislative update. Use VAT code (V0/V1/V2/V3) abstraction.
- Inventing custom Reversal Reason codes per business unit → reporting fragmentation. Stick to the 4 FIS-standard codes.
- Skipping segment definition at project start → cannot retrofit segment reporting later without data migration.
- Activating Advance Foreign Currency Valuation when business doesn't actually need it — Classic is sufficient for most VN companies.
