# Fit-to-Standard Workshop (SAP Activate)

SAP Activate methodology — phase **Explore** delivers fit-to-standard workshops where SAP standard scope items are demonstrated against customer requirements. Output: a list of "fits" (use as-is) vs "gaps" (extend or change scope).

## Purpose for FIS engagements

FIS S/4HANA Cloud engagements use this to:
1. Pre-fill the **Cauhoikhaosat** (Configuration Questionnaire) before workshop — L2 questions
2. Run workshops to confirm fit / identify gaps — L3 questions answered live
3. Produce **BBKS** (Biên bản khảo sát) per workshop session
4. Drive the **SL** (Solution) and **BP** (Business Process) docs from workshop output
5. Justify each FS (custom dev) by referencing which standard scope item was insufficient

## Question levels

| Level | Purpose | Where answered |
|---|---|---|
| L1 | Sales discovery | Pre-sales scoping |
| L2 | Pre-workshop info gathering | Customer fills before workshop |
| L3 | Workshop decision points | During workshop (with consultant guidance) |

L2 questions categorized as **stop/no-go** if missing — workshop cannot start without answers.

## Cauhoikhaosat (Configuration Questionnaire) structure

XLSX with sheets:
- `Template Overview` — purpose, procedure, target audience
- `Accelerator` — main question table
- `Status` — completion tracking
- `Change History` — version log

`Accelerator` columns (FICO questionnaire typically ~250+ rows):

| Column | Use |
|---|---|
| `Process` | Functional area (Finance / Sourcing / Sell / Make / ...) |
| `Project Relevant? Y/N` | Customer marks if applicable |
| `Scope Ref` | SAP scope item ID(s), e.g., `J58, 1GA, 2VA` |
| `SAP ID` | SSCUI code, e.g., `120100` |
| `SSCUI Reference` | SSCUI app name, e.g., "Document Splitting Characteristics" |
| `Area` | Configuration area, e.g., "Accounting and Financial Close" |
| `Topic` | Configuration topic, e.g., "G/L Accounts" |
| `Chủ đề` (VN) | Vietnamese topic |
| `Topic Definition` | EN explanation of topic + standard SAP behavior |
| `Nội dung trao đổi` (VN) | VN summary for discussion |
| `Question` (EN) | Original SAP question |
| `Câu hỏi` (VN) | Translated VN question |
| `Level` | L2 / L3 |
| `Solution` | SAP edition applicability (Public / Private) |
| `FPT đề xuất` | FPT consultant's proposed answer |
| `Customer's Response` | Empty until customer fills |
| `Remark` | Workshop notes |

## Workshop flow

```
Pre-workshop (1 week before):
  ├─ Customer receives Cauhoikhaosat
  ├─ Customer fills L2 answers
  └─ FPT validates completeness; flag stop/no-go gaps

Workshop day (typically 4-8 hours per module):
  ├─ Demo standard scope items
  ├─ Walk through L3 questions live
  ├─ Decisions captured in BBKS table:
  │    STT | Chủ đề | Câu hỏi | Trao đổi nghiệp vụ
  └─ Action items captured in BBKS closing table:
       STT | Tài liệu cần cung cấp | Người thực hiện | Thời hạn

Post-workshop:
  ├─ FPT writes BBKS (within 24-48h)
  ├─ Customer signs off BBKS
  ├─ Outputs feed into SL_KTHT, BP, FS docs
  └─ Gaps → triggers FS doc creation
```

## Common L3 question types (FICO)

### Org structure questions
- "Anh chị vui lòng liệt kê danh sách các Company Code sẽ thực hiện triển khai trên hệ thống"
  - Typical decision: 1 Company Code per legal entity, 4-char code following group-aware pattern
- "Anh chị vui lòng đưa ra cấu trúc tổ chức liên quan đến Profit Center phục vụ việc quản lý tài chính của đơn vị"
  - Typical decision (manufacturer): 1 PC per business block (R&D / hỗ trợ sản xuất / kinh doanh / hỗ trợ) + 1 PC per production line

### Currency / FX
- "Anh chị vui lòng xem xét các loại Currency Type tiêu chuẩn"
  - Decision: 3 loại — Transaction currency / Company Code currency (VND) / Global currency (VND)
- "Exchange Rate Type tiêu chuẩn"
  - Decision: 2 loại — `M` (tỷ giá bình quân giao dịch hàng ngày) + `Z1` (tỷ giá đánh giá cuối kỳ)

### Posting period / fiscal year
- "Năm tài chính của công ty bắt đầu từ ngày nào"
  - Decision: 1/1 đến 31/12, K4 fiscal year variant (recommend by SAP)
- "Posting Period Variant"
  - Decision: Posting Period Variant = Mã Company Code (default)

### Reversal
- "Anh chị vui lòng thống nhất các loại Reversal Reason mong muốn"
  - Decision: 4 loại
    - 01: Hủy ghi âm cùng kỳ
    - 02: Hủy ghi đảo cùng kỳ
    - 03: Hủy ghi âm khác kỳ
    - 04: Hủy ghi đảo khác kỳ

### Payment / AR-AP
- "Payment Term mong muốn"
  - Decision: Phải thu/trả: Thanh toán ngay, 15, 30, 45, 60, 90 ngày
- "Payment Method"
  - Decision: Tiền mặt / Chuyển khoản / Tín dụng / L/C
- "Tách payment method chuyển khoản để set up cho đi tiền qua cả 113 và 112"

### VAT (Vietnam)
- "VAT đầu vào / đầu ra hiện đang sử dụng"
  - Decision: Không chịu thuế, 0%, 5%, 8%, 10%
  - VAT đầu vào tách 3 nhóm: HHDV nội địa / HHDV nhập khẩu / Tài sản
  - VAT đầu ra tách 2 nhóm: HĐ SXKD / hàng bán bị trả lại, giảm giá hàng bán

### Asset
- "Asset Class hiện tại"
  - Decision: Tạo nhóm theo Thông tư 200 và 99; bỏ TS đầu tài khoản 215 và 217; tách asset class theo nhóm chi phí 627, 641, 642
- "Phương thức khấu hao"
  - Decision: 3 phương thức — KH đường thẳng theo ngày / KH đường thẳng theo tháng (CCDC) / Không tính khấu hao

### Reports
- Customer cung cấp danh sách báo cáo cần phát triển → FPT đánh giá fit / gap → FS doc cho gap
- See `references/vn-localization.md` "Standard custom reports per Thông tư 200" for the typical VN report shopping list (Phiếu thu/chi, Sổ quỹ, BC TSCĐ, Bảng kê VAT, Sổ chi tiết tài khoản, BC công nợ, Bảng cân đối phát sinh, KQKD, LCTT...)

## Best practices

1. **Always pre-fill `FPT đề xuất` column** before sending Cauhoikhaosat — saves customer time, surfaces decision points
2. **Capture full attendee list** in BBKS header table — customer + FPT roles
3. **Action items must have owner + deadline** — use 3-col table at end of BBKS
4. **L3 decisions feed directly into SL doc Section 3 (Danh mục dùng chung)** — copy decision text verbatim
5. **Defer "extension proposals"** to separate Change Request — don't conflate with fit-to-standard
6. **Workshop attendance**: minimum 1 Key User + 1 Process Owner from customer + 1 SA + 1 Team Lead from FPT

## Cross-reference

- FIS document types: `references/fis-document-types.md`
- S/4HANA scope items: `references/s4hana-cloud-scope-items.md`
- VN-specific decisions: `references/vn-localization.md`
- Master data templates: `references/master-data.md`
