# SAP Functional Specification (FS) — FIS template structure

Standard structure for SAP custom-development FS at FIS engagements. Use when authoring a feature-spec / FSD for SAP customizations (Reports, Forms, Interfaces, Conversions, Enhancements, Custom Fiori Apps, Workflows).

The doc renders to .docx via `/fis-docs export`. Section structure below maps 1:1 to the FIS Word template layout.

## Document outline

```
1. Cover Page
   - Project name + module
   - "TÀI LIỆU ĐẶC TẢ CHỨC NĂNG (FS)"
   - Function-specific name
   - Metadata table: Mã DA · Mã TL · Phân hệ · Người lập · Phiên bản · Ngày
   - Place + month/year

2. Quản lý thay đổi (Change Log)
   Table: Ngày | Mục thay đổi | Mô tả | Phiên bản

3. Mục lục (Table of Contents)

4. TỔNG QUAN
   1.1. Mục đích tài liệu
   1.2. Phạm vi tài liệu
   1.3. Tài liệu tham chiếu (table)
   1.4. Thuật ngữ viết tắt (table)

5. MÔ TẢ YÊU CẦU
   2.1. Yêu cầu nghiệp vụ
   2.2. Đối tượng sử dụng (table — role × responsibility)
   2.3. Quy trình nghiệp vụ
   2.4. Dữ liệu đầu vào
   2.5. Dữ liệu đầu ra
   2.6. Xử lý nghiệp vụ
   2.7. Format báo cáo / chứng từ (table)
   2.8. Hình thức báo cáo / chứng từ (description + table of layout zones for forms)

6. MÔ TẢ THIẾT KẾ CHỨC NĂNG
   3.1. Thông tin chung (T-code · Fiori App · Access path · Trigger)
   3.2. Các bảng dữ liệu cần phát triển
        3.2.1. Bảng trường hiển thị
        3.2.2. Custom Business Object / Custom table (if any)
   3.3. Tham số đầu vào (Selection Screen)
   3.4. Luồng xử lý (Processing Logic)
   3.5. Nguồn dữ liệu (CDS Views / Tables)

7. NỘI DUNG KHÁC
   4.1. Yêu cầu tiêu chí sắp xếp
   4.2. Các yêu cầu khác
   4.3. Bảo mật, phân quyền (table)
   4.4. Test scenarios (table — ≥ 5-8 entries: happy path + edge + validation)

8. PHỤ LỤC: Các điểm cần xác nhận
   - Bullet list of all [Unverified] / [Inference] points to verify with customer
```

## When to use which variant

| Custom dev type | Section weight |
|---|---|
| **Report** (báo cáo) | §2.4-2.7 detailed selection/output; §3.4 logic per chỉ tiêu |
| **Form** (chứng từ Adobe Form / Smartform) | §2.8 layout zones detailed; §3.2 print-specific fields |
| **Interface** (REST / SOAP / IDoc) | §2.4-2.5 mapping fields; §3.4 with sequence diagram + error handling |
| **Conversion** (data migration) | §3.4 source/target mapping; §3.4 validation rules + rollback |
| **Enhancement** (BAdI / user-exit) | §3.4 standard flow + enhancement point + custom logic |
| **Custom Fiori App** | §2.8 wireframe; §3.1 OData service + role; §3.5 CDS views annotated |

## Format requirements (when rendered to .docx)

- Font: Times New Roman throughout (body + headings).
- Body: 11pt.
- Heading scale: H1=16pt, H2=14pt, H3=13pt, Title=20pt.
- Page: A4. Margins 1in (top/bottom/left), 0.75in (right).
- Heading colours: H1 `#1F3864`, H2/H3 `#2E5597`.
- Tables: 4-pt black border, header shading `#D9E1F2`, label cells `#F2F2F2`.
- Header (every page): right-aligned `<doc-id> v<ver>`.
- Footer (every page): centered `Confidential — Trang X / Y`.

The `template:` frontmatter key in the MD points to the docx template that drives this rendering — `fis-ai-kit/templates/docx-templates/sap-fs.docx`.

## Reality filter

Any technical assumption about SAP standard objects (CDS view names, table names, authorization objects, T-codes) MUST be tagged `[Inference]` or `[Unverified]` until verified against the live system or SAP help portal. The §8 Phụ lục collects all such tags for customer confirmation.

## Anti-patterns

- Skipping §8 Phụ lục → assumptions ship to PRD without sign-off.
- Single-language section headings (English-only) → conflicts with FIS bilingual VN/EN norms expected by Vietnamese sponsors.
- Hardcoding company / project / vendor names → defeats template reusability.
- Test scenarios < 5 → low coverage; minimum is happy path + 2 edge + 2 validation.
