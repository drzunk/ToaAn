# SAP FS — section templates

Boilerplate wording for each FS section. Drop in, then fill the [bracketed] slots.

## §1.1 Mục đích tài liệu

> Tài liệu này mô tả chi tiết yêu cầu nghiệp vụ và thiết kế chức năng của **[Tên chức năng]** trên hệ thống SAP **[S/4HANA Cloud / S/4HANA on-premise / ECC]** triển khai cho **[Tên khách hàng / đơn vị]**, làm cơ sở để phát triển, kiểm thử và nghiệm thu.

## §1.2 Phạm vi tài liệu

Tài liệu áp dụng cho:

- **Trong phạm vi (in-scope):** [danh sách module / quy trình / chức năng cụ thể]
- **Ngoài phạm vi (out-of-scope):** [danh sách điểm rõ ràng không bao hàm]

## §1.3 Tài liệu tham chiếu (template table)

| STT | Mã tài liệu | Tên tài liệu | Phiên bản | Tác giả |
|---|---|---|---|---|
| 1 | PRD-NNNN | Tài liệu yêu cầu sản phẩm | v1.0 | [BA] |
| 2 | SAP Help — [Topic] | help.sap.com link | — | SAP |

## §1.4 Thuật ngữ viết tắt (template table)

| Viết tắt | Thuật ngữ đầy đủ | Mô tả |
|---|---|---|
| FS | Functional Specification | Đặc tả chức năng |
| BAPI | Business API | Hàm chuẩn SAP để gọi từ ngoài |
| IDoc | Intermediate Document | Tài liệu trung gian SAP |
| CDS | Core Data Services | Lớp data model trên HANA |

## §2.1 Yêu cầu nghiệp vụ (template prose)

> Hiện tại, **[mô tả tình trạng hiện tại / pain point]**.
>
> Yêu cầu xây dựng **[tên chức năng]** để **[đầu ra mong đợi]**.
>
> Lợi ích kỳ vọng:
> - **[Benefit 1]**
> - **[Benefit 2]**

## §2.2 Đối tượng sử dụng (template table)

| Đối tượng | Vai trò | Trách nhiệm |
|---|---|---|
| [User role] | [SAP role e.g. Kế toán viên AP] | [Tasks they perform with this function] |

## §2.3 Quy trình nghiệp vụ (template prose + diagram)

> Quy trình gồm các bước:
> 1. [Step 1]
> 2. [Step 2]
> ...
>
> Sơ đồ luồng (Mermaid `flowchart` recommended):
>
> ```mermaid
> flowchart LR
>   A[Start] --> B[Step 1]
>   B --> C{Decision}
>   C -->|Yes| D[Step 2a]
>   C -->|No| E[Step 2b]
> ```

## §3.1 Thông tin chung (template table)

| Mục | Giá trị |
|---|---|
| T-code custom | Z[XXX] |
| Fiori App custom | [APP_NAME] |
| Access path | [SAP Easy Access path] |
| Trigger | Manual / Scheduled (job name) / Event |
| Phân hệ | [FI / CO / MM / SD / ...] |

## §3.2 Bảng dữ liệu (template table)

### §3.2.1 Bảng trường hiển thị

| STT | Trường | Mô tả | Kiểu dữ liệu | Bắt buộc | Nguồn |
|---|---|---|---|---|---|
| 1 | [field_name] | [description] | CHAR(10) | Y | Table-Field |

## §3.3 Tham số đầu vào — Selection Screen (template table)

| STT | Tham số | Mô tả | Kiểu | Mặc định | Bắt buộc |
|---|---|---|---|---|---|
| 1 | [P_BUKRS] | Mã công ty | T001-BUKRS | Y | Y |

## §3.4 Luồng xử lý (template prose with pseudo-code blocks)

> 1. Validate input parameters.
> 2. Query data sources (see §3.5).
> 3. Apply business rules.
> 4. Format output / call BAPI / generate IDoc.
> 5. Commit and return.
>
> ```pseudo
> IF P_BUKRS IS INITIAL THEN
>   ERROR 'Mã công ty không được rỗng'.
> ENDIF.
>
> SELECT ... FROM ... INTO TABLE ...
> ```

## §3.5 Nguồn dữ liệu (template table)

| Nguồn | Loại | Mô tả |
|---|---|---|
| BKPF / BSEG | Tables (ECC) | Document header / line items |
| I_GLAccountLineItem | CDS view (S/4HANA) | GL line item analytical view |

## §4.3 Bảo mật, phân quyền (template table)

| Auth object | Field | Value | Note |
|---|---|---|---|
| S_TCODE | TCD | Z[XXX] | Execute the custom T-code |
| F_BKPF_BUK | BUKRS | [list] | Restrict by company code |

## §4.4 Test scenarios (template table — minimum 5-8 rows)

| TC ID | Scenario | Pre-condition | Steps | Expected | Type |
|---|---|---|---|---|---|
| TC-01 | Happy path — valid input | User has role [X] | Run T-code Z[XXX] with valid params | Output Excel ALV with N rows | Positive |
| TC-02 | Validation — empty mã công ty | — | Run with empty BUKRS | Error 'Mã công ty không được rỗng' | Negative |
| TC-03 | Authorization — user without role | User does NOT have role [X] | Run T-code Z[XXX] | "Bạn không có quyền truy cập" | Security |

## §8 Phụ lục — Các điểm cần xác nhận

> Các điểm sau cần khách hàng xác nhận trước khi triển khai:
>
> - [ ] **[Inference]** Tên CDS view chính xác là `I_GLAccountLineItem`? (đề xuất theo SAP Help cho S/4HANA 2023; verify trên hệ thống KH).
> - [ ] **[Unverified]** Authorization object `F_BKPF_BUK` áp dụng đúng cho biến thể S/4HANA Cloud Public hay không?
> - [ ] **[Inference]** Mã hàng phân quyền theo nhóm Profit Center ngoài Company Code không?
