# senior-ba-banking-govtech

Bộ Agent Skill cho Senior Business Analyst làm dự án **ngân hàng** và **dịch vụ công** tại
Việt Nam. Tương thích chuẩn Agent Skills: Claude Code, Claude.ai, Cursor, Codex, Copilot,
Gemini CLI, Windsurf.

## Cài đặt

**Claude.ai** — tải file `.skill`, vào Settings → Capabilities → Skills → Upload.

**Claude Code**
```bash
cp -r senior-ba-banking-govtech ~/.claude/skills/        # toàn cục
cp -r senior-ba-banking-govtech .claude/skills/          # theo dự án
```

**Cursor / Codex / Copilot / Windsurf**
```bash
cp -r senior-ba-banking-govtech .agents/skills/
```

## Nội dung

```
senior-ba-banking-govtech/
├── SKILL.md                          # Router: nguyên tắc, quy trình, bảng tra nhanh
└── references/
    ├── elicitation.md                # Phỏng vấn, workshop, quan sát, đọc văn bản pháp quy
    ├── requirements-spec.md          # BRD, user story, use case, quy tắc nghiệp vụ, phi chức năng
    ├── process-modeling.md           # BPMN, swimlane, RACI, gap analysis, đặc thù 2 miền
    ├── data-modeling.md              # ERD khái niệm, từ điển dữ liệu, data mapping, di trú
    ├── integration-spec.md           # Mẫu đặc tả giao diện, mã lỗi, idempotency, đối soát
    ├── banking-domain.md             # Bản đồ hệ thống ngân hàng, sản phẩm, kênh, hạn mức, phí
    ├── payments-domain.md            # NAPAS, VietQR, thu hộ chi hộ, đối soát, giao dịch treo
    ├── public-services-domain.md     # TTHC, DVC toàn trình/một phần, một cửa, chính quyền 2 cấp
    ├── govtech-integration.md        # VNeID, CSDLQG dân cư, nền tảng chia sẻ dữ liệu, chữ ký số
    ├── legal-compliance.md           # Khung pháp lý 2 miền, ma trận tuân thủ
    ├── ux-forms.md                   # Biểu mẫu điện tử, thông báo lỗi, hành trình, khả năng tiếp cận
    └── stakeholder-delivery.md       # Bên liên quan, ưu tiên hóa, quản lý thay đổi, UAT, bàn giao
```

Agent chỉ nạp `SKILL.md` khi kích hoạt rồi tự mở file reference cần dùng, nên bộ này không
chiếm nhiều ngữ cảnh dù nội dung dài.

## Dùng chung với bộ kiểm thử

Bộ này và `banking-test-automation` bổ sung cho nhau: BA viết tiêu chí chấp nhận → QA biến
thành test case. Cài cả hai thì agent tự chọn bộ phù hợp theo câu hỏi. Điểm nối giữa hai bộ
là quy tắc nghiệp vụ có mã (BR-xxx) và ma trận truy vết.

## Lưu ý quan trọng

- **Phần pháp lý là định hướng phân tích, không phải tư vấn pháp lý.** Các văn bản được tham
  chiếu (Nghị định 42/2022, Nghị định 118/2025, Luật Giao dịch điện tử 2023, Luật Bảo vệ dữ
  liệu cá nhân 2025 và Nghị định 356/2025, Thông tư 50/2024/TT-NHNN, Quyết định 29/2026/QĐ-TTg)
  đã được kiểm chứng tại thời điểm biên soạn, nhưng khu vực này thay đổi rất nhanh. Luôn đối
  chiếu bản hiện hành và làm việc với bộ phận pháp chế.
- **Lĩnh vực dịch vụ công đang trong giai đoạn chuyển đổi mạnh.** Thẩm quyền giải quyết thủ
  tục hành chính đã được phân định lại theo mô hình chính quyền 2 cấp từ 01/7/2025. Việc đầu
  tiên khi nhận dự án là xác minh lại thẩm quyền hiện hành của từng thủ tục.
- **Đọc nội dung trước khi cài.** Skill là chỉ dẫn agent tự động tuân theo. Trong môi trường
  ngân hàng hoặc cơ quan nhà nước, nên đưa về Git nội bộ và qua duyệt an toàn thông tin.

## Tùy biến

Thêm `references/project-context.md` mô tả dự án cụ thể: đơn vị chủ quản, hệ thống hiện có,
danh sách thủ tục hoặc sản phẩm trong phạm vi, các bên liên quan và người ký duyệt, các kết
nối cần xin. Rồi thêm một dòng vào bảng tra nhanh trong `SKILL.md`. Agent sẽ đọc nó trước khi
hỏi lại bạn những điều đã có câu trả lời.

Giấy phép: MIT.
