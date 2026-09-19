# banking-test-automation

Bộ Agent Skill cho kiểm thử hệ thống ngân hàng và trung gian thanh toán tại Việt Nam. Tương
thích chuẩn Agent Skills: Claude Code, Claude.ai, Cursor, Codex, Copilot, Gemini CLI,
Windsurf và các client khác.

## Cài đặt

**Claude.ai** — tải file `.skill` rồi vào Settings → Capabilities → Skills → Upload.

**Claude Code**
```bash
# Toàn cục
cp -r banking-test-automation ~/.claude/skills/
# Hoặc chỉ cho một dự án
cp -r banking-test-automation .claude/skills/
```

**Cursor / Codex / Copilot / Windsurf**
```bash
cp -r banking-test-automation .agents/skills/
```

Kiểm tra: agent phải tự kích hoạt skill khi bạn hỏi những câu như *"viết test case chuyển
tiền liên ngân hàng"* hoặc *"test luồng maker-checker thế nào"*.

## Nội dung

```
banking-test-automation/
├── SKILL.md                        # Router: nguyên tắc, quy trình, bảng tra nhanh
└── references/
    ├── domain-model.md             # Bút toán kép, số dư, vòng đời giao dịch, phí/lãi/tỉ giá
    ├── test-data.md                # Sinh dữ liệu giả hợp lệ, Luhn, che PII, cô lập dữ liệu
    ├── payments-transfer.md        # NAPAS 247, VietQR, idempotency, giao dịch treo, tra soát
    ├── iso-messages.md             # ISO 8583 (thẻ) và ISO 20022 / SWIFT MT→MX
    ├── cards-3ds.md                # Auth–clearing–settlement, ATM/POS, 3DS, chargeback
    ├── auth-security.md            # Ngưỡng sinh trắc học TT50, OTP, thiết bị, OWASP cho bank
    ├── maker-checker-rbac.md       # Phê duyệt nhiều cấp, ma trận phân quyền, audit trail
    ├── eod-reconciliation.md       # Cut-off, batch, giả lập thời gian, đối soát, cân GL
    ├── nonfunctional.md            # Hiệu năng, chaos, giả lập đối tác, DR, mobile
    ├── compliance-vn.md            # TT50, Luật BVDLCN 2025/NĐ 356, PCI DSS, KYC/AML
    ├── automation-stack.md         # Chọn tầng test, locator, chống flaky, CI/CD, môi trường
    └── test-artifacts.md           # Mẫu test case, test plan, ma trận truy vết, báo cáo
```

Agent chỉ nạp `SKILL.md` khi kích hoạt, rồi tự mở file reference cần thiết — nên bộ này không
chiếm nhiều ngữ cảnh dù nội dung dài.

## Cảnh báo trước khi dùng trong môi trường ngân hàng

- **Đọc kỹ nội dung trước khi cài.** Skill là chỉ dẫn mà agent tự động tuân theo. Với môi
  trường ngân hàng, nên fork về Git nội bộ và cho bộ phận bảo mật duyệt thay vì cài trực tiếp.
- **Phần pháp lý chỉ mang tính định hướng, không phải tư vấn pháp lý.** Văn bản tham chiếu
  (Thông tư 50/2024/TT-NHNN, Luật Bảo vệ dữ liệu cá nhân 2025, Nghị định 356/2025/NĐ-CP) có
  thể đã thay đổi. Đối chiếu bản hiện hành và làm việc với bộ phận Pháp chế/Tuân thủ trước khi
  chốt tiêu chí nghiệm thu.
- **Ngưỡng và quy tắc nghiệp vụ trong tài liệu là ví dụ minh họa.** Luôn lấy con số thật từ
  đặc tả của dự án.

## Tùy biến cho ngân hàng của bạn

Thêm một file `references/project-context.md` mô tả: tên core, danh sách kênh, đối tác tích
hợp, biểu phí, các ngưỡng hạn mức thật, môi trường và cách truy cập. Rồi thêm một dòng vào
bảng tra nhanh trong `SKILL.md` trỏ tới file đó. Agent sẽ đọc nó trước khi hỏi lại bạn những
câu đã có câu trả lời.

## Nguồn tham khảo khi xây dựng

Khung tổ chức skill tham khảo từ các repo mã nguồn mở: `petrkindlmann/qa-skills`,
`fugazi/test-automation-skills-agents`, `SoftwareOneHN/qa-testing-kit`. Phần nghiệp vụ ngân
hàng, message chuẩn quốc tế và pháp lý Việt Nam được biên soạn riêng cho bộ này.

Giấy phép: MIT.
