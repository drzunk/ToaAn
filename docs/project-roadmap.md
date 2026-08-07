---
title: "ToaAn — Lộ trình"
template: project-roadmap
version: "1.0"
status: "Đang dùng"
date: "2026-08-07"
---

# ToaAn — Lộ trình

Tài liệu này **sắp thứ tự** các khoản nợ và hạng mục mở rộng. Mô tả chi tiết từng khoản nợ, mức
P1–P3 và lý do chấp nhận nằm ở [`TECH-DEBT.md`](TECH-DEBT.md) — đó là nguồn sự thật, đừng chép lại
nội dung sang đây.

Lộ trình này không phải cam kết thời gian. Nó trả lời câu hỏi "làm gì tiếp theo khi có thời gian".

## Đã xong

### v1 — nền vận hành

Chốt theo [`V1-CHECKLIST.md`](V1-CHECKLIST.md):

- Bộ tài liệu vận hành đầy đủ: workflow, triage, schema case, tech debt, checklist DoD.
- Dashboard `CaseEditorServer` với tab sinh test case và API `generate-cases`.
- Ba nguồn case hoạt động song song: `local-cases.json`, Google Sheet, ma trận độ phủ.
- Field bắt buộc bước 1–3 fail sớm qua `setTextRequired`.
- Assert ổn định ở màn Xem lại thay vì `assertNotNull` trần.
- CI chạy `mvn -B -Punit test` trên mỗi push/PR.

### v1.1 — dọn nhẹ

- Xóa luồng Dashboard ghi đè file rồi chạy thẳng theo một màn. Còn đúng một luồng vận hành:
  Chọn → Thêm → Lưu → Chạy.
- `local-cases.json` mặc định rỗng; cả UI lẫn `/api/run` chặn khi danh sách case bật rỗng.
- Siết thêm field bắt buộc bước 1–3 sang Required API, gồm nhánh cá nhân, tổ chức, hành chính,
  người đại diện và đồng nguyên đơn.

### v1.2 — gắn vào FIS AI Kit

- Bộ tài liệu FIS trong `docs/`: tổng quan, bản đồ codebase, quy ước code, kiến trúc, lộ trình.
- [`FIS-WORKFLOW.md`](FIS-WORKFLOW.md) mô tả quy trình thay đổi code/doc.

## Đợt tiếp theo — P1

Ba khoản này ảnh hưởng trực tiếp tới độ tin cậy của kết quả chạy.

| Việc | Nợ liên quan | Ghi chú |
|---|---|---|
| Ổn định eform bước 4 trong iframe | TD-10 | Việc vận hành hơn là refactor: phân loại đúng ENV_DATA và FLAKE, không thêm sleep. Cần dữ liệu từ nhiều lượt chạy trước khi quyết định sửa gì |
| Hoàn tất chuyển soft-skip còn lại | TD-02 | Phần còn giữ soft-skip có chủ đích: email, nghề nghiệp, chức vụ người đại diện tổ chức, fallback địa chỉ, dropdown và địa chỉ hành chính lồng |
| Làm dày assert E2E ngoài màn Xem lại | TD-03 | Đã có marker + loại đơn ở bước 6; các bước giữa vẫn dựa nhiều vào việc chuyển bước thành công |

## Đợt sau — P2

| Việc | Nợ liên quan | Điều kiện kích hoạt |
|---|---|---|
| Mở rộng discovery sang địa chỉ lồng và CCCD người đại diện tổ chức | TD-08 | Khi cần thêm ca âm ở hai vùng đó |
| Hợp nhất hai nguồn danh sách field ca âm | TD-13 | Khi mở rộng discovery — hiện `FieldCoverageCatalog` và `CATALOG` trong `FieldDiscoverySweepTest` phải sửa đôi |
| Ép được override vào field trong eform iframe | TD-11 | Sau khi eform bước 4 ổn định (phụ thuộc TD-10) |
| Chia nhỏ `WebUI` theo call-site | TD-01 | Chỉ khi một thay đổi thật sự cần, không mở refactor riêng |
| Theo dõi ổn định session Chrome khi chạy song song | TD-07 | Liên tục, qua `ScenarioDispatch` |
| Giảm lệch giữa mega XPath và UI thật | TD-04 | Đồng bộ catalog mỗi khi UI đổi |

## Để ngỏ — P3

| Việc | Nợ liên quan |
|---|---|
| Nâng `TestCaseGenerator` lên pairwise đầy đủ | TD-05 |
| Đưa login ca âm vào cùng cơ chế `truongLoi` của wizard | TD-06 |
| Thêm cột chọn loại bị đơn vào schema `CaseRow` | TD-12 |
| Checklist quyền chia sẻ Google Sheet rõ hơn trong onboarding | TD-09 |

## Không làm

Giữ nguyên từ [`TECH-DEBT.md`](TECH-DEBT.md). Đây là ranh giới, không phải việc chưa tới lượt:

- Migrate sang Playwright.
- Sinh case bằng LLM / Vision.
- Thay `BaoCaoHtml` bằng Allure hoặc Extent. Lý do gỡ ExtentReports được ghi trong javadoc của
  `report/BaoCao.java` — đọc trước khi có ý định mang thư viện báo cáo nào về.
- Viết lại UI Case Editor.
- Rewrite `WebUI`.

## Cách cập nhật tài liệu này

Khi đóng một khoản nợ, sửa `TECH-DEBT.md` trước rồi mới chuyển dòng tương ứng ở đây lên mục
"Đã xong". Đừng để hai file nói khác nhau về cùng một TD.
