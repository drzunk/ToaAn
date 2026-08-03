# FS Example — Asset BARCODE Print (S/4HANA Cloud Public Edition)

Worked FS doc showing the full FIS Word template applied to a Cloud Public Edition scenario where the standard scope item didn't cover barcode printing.

> **Bối cảnh (template):** Customer triển khai SAP S/4HANA Cloud Public Edition, scope `J62` (Asset Accounting). Họ cần in nhãn barcode trên tài sản / công cụ dụng cụ — standard không có Fiori app cho việc này. → Trigger FS doc.

## Header

| Field | Value |
|---|---|
| Mã dự án | `<COMPANY>_SAP_<YEAR>_PM` |
| Mã tài liệu | `<COMPANY>_SAP_<YEAR>_PM_FS_Asset_BARCODE` |
| Phân hệ | AA |
| Người lập | `<SA owner>` |
| Phiên bản | 0.1 |
| Ngày cập nhật | `<dd/mm/yyyy>` |

## 1. Tổng quan

### 1.1 Mục đích tài liệu

Mô tả thiết kế chức năng cho việc in nhãn barcode dán trên TSCĐ / CCDC để hỗ trợ kiểm kê.

### 1.2 Phạm vi tài liệu

Phạm vi phân hệ FI-AA: kế toán tài sản cho `<company-name>`.

### 1.3 Tài liệu tham chiếu

| STT | Tên tài liệu | Người tạo | Ghi chú |
|---|---|---|---|
| 1 | `<COMPANY>_SAP_<YEAR>_PM_BP_AA_v<X>.<Y>.docx` | SA team | Reference for J62 process |
| 2 | `<COMPANY>_SAP_<YEAR>_PM_SL_KTHT_v<X>.<Y>.docx` | SA team | Asset Class master data |

### 1.4 Thuật ngữ viết tắt

| STT | Thuật ngữ | Định nghĩa |
|---|---|---|
| 1 | TSCĐ | Tài sản cố định |
| 2 | CCDC | Công cụ dụng cụ |
| 3 | CDS | Core Data Services (S/4HANA query layer) |
| 4 | Fiori App | SAP frontend app |

## 2. Mô tả yêu cầu

### 2.1 Mục đích

Xây dựng chức năng in nhãn BARCODE cho tài sản, công cụ dụng cụ để dán trên thiết bị phục vụ kiểm kê.

### 2.2 Quy trình nghiệp vụ liên quan

- Master data quản lý tài sản (J62-01)
- Mua sắm và ghi nhận tài sản (J62-00, J62-02)

### 2.3 Các giả định

- Tài sản đã được tạo trong hệ thống và có Asset Number (ANLN1-ANLN2).
- Thiết bị in barcode hỗ trợ chuẩn Code 128 hoặc QR Code.

### 2.4 Rủi ro

- Trùng mã barcode nếu nhiều người chạy đồng thời (mitigation: dùng ANLN1+ANLN2 làm key, không tạo sequence riêng).
- Nhãn rách / mờ → cần re-print (chấp nhận, không track print history giai đoạn 1).

### 2.5 Ràng buộc

- S/4HANA Cloud Public Edition không cho phép tạo Smartform truyền thống → dùng Adobe Forms hoặc CSS-based PDF generation.

### 2.6 Mô tả yêu cầu nghiệp vụ

User đăng nhập vào Fiori → mở custom app `Asset BARCODE` → lọc theo Company Code, Asset Class, Asset Number range, Date range → hệ thống hiển thị danh sách → user chọn tài sản cần in → Click "Print" → file Excel chứa các form barcode được tạo và download.

### 2.7 Format báo cáo / chứng từ

- Font chữ: Times New Roman
- Cỡ chữ: 11
- Khổ giấy: A4 (orient: portrait)
- Định dạng: Excel (.xlsx)

### 2.8 Hình thức báo cáo / chứng từ

Mỗi nhãn chứa:
- Mã tài sản (ANLN1-ANLN2)
- Tên tài sản (ANLA-TXT50)
- Mã barcode (Code 128 / QR)
- Logo công ty
- Asset Class

## 3. Mô tả thiết kế chức năng

### 3.1 Thông tin chung

| Field | Value |
|---|---|
| T-code / Fiori App chuẩn | (none — không có) |
| T-code / Fiori App customized | `Asset BARCODE` |
| Đường dẫn | Fiori Launchpad → Asset Accounting → Asset BARCODE |
| Kích hoạt | User-initiated (manual) |

### 3.2 Các bảng dữ liệu cần phát triển

Không cần custom Z table — đọc từ CDS View standard.

| CDS View | Use |
|---|---|
| `I_FIXEDASSET` | Asset master (header) |
| `I_FixedAssetCompanyCode` | Asset attributes per CC |
| `I_FixedAssetClass` | Asset Class lookup |

### 3.3 Tham số lọc

| STT | Tham số (Parameters) | Mô tả (Description) | Điều kiện (Conditions) | Giá trị mặc định (Default) | Bắt buộc (Required) | Mô tả thêm (Note) |
|---|---|---|---|---|---|---|
| 1 | Company Code | Mã công ty | Single value | Mã CC user login | X | Search help, query từ CDS View `I_FIXEDASSET` |
| 2 | Asset Class | Nhóm tài sản | Multi-select | (rỗng) |  | Search help từ `I_FixedAssetClass` |
| 3 | Asset Number From / To | Số tài sản đầu / cuối | Range |  |  | ANLN1 |
| 4 | Capitalization Date From / To | Ngày vốn hóa | Range |  |  |  |
| 5 | Cost Center | Trung tâm chi phí | Multi-select |  |  | Search help từ `I_CostCenter` |

### 3.4 Các màn hình chức năng

#### Layout màn hình báo cáo

```
┌──────────────────────────────────────────────────────┐
│ Asset BARCODE                                        │
├──────────────────────────────────────────────────────┤
│ [Company Code: <CC> ▼] [Asset Class: ▼] [Asset From: __] [Asset To: __] │
│ [Date From: __] [Date To: __] [Cost Center: ▼]       │
│                                              [Search]│
├──────────────────────────────────────────────────────┤
│ ☐ Asset No.  Description  Asset Class  Cap Date     │
│ ☐ 100001-0   Máy in HP    AS6271       01/03/2026   │
│ ☐ 100002-0   Bàn làm việc AS6411       15/03/2026   │
├──────────────────────────────────────────────────────┤
│                              [Print Selected (Excel)] │
└──────────────────────────────────────────────────────┘
```

#### Form nhãn BARCODE

```
┌─────────────────────────┐
│ [LOGO]                  │
│                         │
│ Mã: 100001-0            │
│ Tên: Máy in HP          │
│ Class: AS6271           │
│                         │
│ [████ ███ █ ██████]     │ ← Code 128
│ 100001-0                │
└─────────────────────────┘
```

### 3.5 Nội dung chi tiết báo cáo / chứng từ

| Field | Source | Note |
|---|---|---|
| Mã tài sản | `I_FIXEDASSET-MainAssetNumber` + `-AssetSubnumber` | Format: NNNNNNNNNNNN-NNNN |
| Tên | `I_FIXEDASSET-FixedAssetDescription` | First 50 chars |
| Asset Class | `I_FIXEDASSET-FixedAssetClass` |  |
| Cap Date | `I_FIXEDASSET-CapitalizationDate` |  |
| Cost Center | `I_FixedAssetCompanyCode-CostCenter` |  |
| Barcode | Generated from `MainAssetNumber-AssetSubnumber` | Code 128, 25mm × 8mm |

### 3.6 Xử lí lỗi / trường hợp ngoại lệ

| Case | Handling |
|---|---|
| User không chọn asset nào → Print | Disable button hoặc message "Vui lòng chọn ít nhất 1 tài sản" |
| Asset đã thanh lý (Retirement Date < today) | Highlight đỏ trong list, vẫn cho phép in |
| CDS query timeout (> 30s) | Show error "Vui lòng thu hẹp phạm vi tìm kiếm" |
| Lỗi tạo Excel file | Log lỗi vào Application Log, hiển thị message generic |

### 3.7 Bảo mật, phân quyền

Auth object cần (custom hoặc reuse):
- `S_TCODE` — không applicable (Cloud không có T-code)
- `S_FIORI_APP` — assign Fiori app `ZAssetBarcode` vào role
- Business Catalog: `SAP_FIN_BC_AA_DISP` (display) hoặc custom `Z_FIN_BC_AA_BARCODE`
- Authorization values: BUKRS (CC), ANLKL (Asset Class)

### 3.8 Test scenario / Test data

#### Client: CUS

| Test Case | Steps | Expected | Reference |
|---|---|---|---|
| TC-01 | Login user with CC=`<test-CC>`, mở Fiori app `Asset BARCODE`, search Asset Class = AS6271, chọn 5 asset, click Print | File Excel download, mở ra có 5 nhãn đúng định dạng | TC sheet `J62.01-XX` |
| TC-02 | Search range Asset 100001 - 100100, chọn All, Print | Excel có 100 nhãn, không skip nào | |
| TC-03 | User không có authorization cho `<test-CC>`, mở app | App không hiển thị trong Launchpad | |

## Cross-reference

- FS template structure: `references/fs-template-structure.md`
- FS section templates: `references/fs-section-templates.md`
- Other worked example: `references/fs-example-asset-depreciation.md`
