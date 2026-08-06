# Nợ kỹ thuật — ToaAn v1

Phạm vi: chấp nhận cho v1 / làm sau. Không dùng doc này để mở rewrite lớn trong sprint vận hành.

| ID | Mô tả | Mức | v1 |
|---|---|---|---|
| TD-01 | `WebUI` rất lớn (~3.5k LOC), nhiều XPath/mega helper địa chỉ | P2 | Chấp nhận — sửa theo call-site, không rewrite |
| TD-02 | Soft-skip (`setTextWithCheck` / `boQua`) trên field tùy chọn và một số dropdown | P1 | Đã siết field bắt buộc bước 1–3 qua `setTextRequired`; phần còn lại (masked optional, dropdown, địa chỉ lồng) giữ soft-skip |
| TD-03 | Assert E2E mỏng — trước đây đến Xem lại chủ yếu `assertNotNull(review)` | P1 | Đã thêm tín hiệu ổn định trên màn Xem lại (marker + loại đơn) |
| TD-04 | Mega XPath / synonym catalog dễ lệch UI | P2 | Chấp nhận — tab Config locator hỗ trợ tra; sync catalog khi UI đổi |
| TD-05 | `TestCaseGenerator` rule-based, chưa pairwise full / chưa cover mọi nhánh eform | P3 | Chấp nhận — mid/full matrix lo độ phủ; generator lo case cấu hình theo màn |
| TD-10 | Ca âm bước 4 chỉ phủ form textarea. Field trong eform (iframe, schema động) discovery thấy được nhưng `tryFieldOverride` chưa ép được | P2 | Chấp nhận v1 — `FieldCoverageCatalog` để trống `B4_EFORM` để không sinh case sai mode |
| TD-11 | Loại bị đơn không có trong schema `CaseRow`, nên ca âm MST bị đơn phải đi qua luồng Phá sản | P3 | Chấp nhận — thêm cột chọn loại bị đơn khi cần mở rộng |
| TD-06 | Login ca âm dùng `LoginTest` / `-Plogin`, không qua `truongLoi` wizard | P3 | Cố ý — hiển thị trên tab Sinh (màn Đăng nhập); chạy bằng `/api/run-login`; ca dương `untilStep=0` vẫn qua `local-cases` |
| TD-07 | Parallel phụ thuộc session Chrome ổn định | P2 | Chấp nhận — theo dõi `ScenarioDispatch` |
| TD-08 | Discovery chưa quét đủ địa chỉ lồng / CCCD người ĐD tổ chức | P2 | Làm sau khi mở rộng ca âm |
| TD-09 | Sheet / quyền chia sẻ dễ nhầm “hỏng tool” | P3 | Checklist README + WORKFLOW |
| TD-10 | Eform bước 4 iframe — flake / form chưa xuất bản | P1 (vận hành) | Triage ENV_DATA/FLAKE; không sleep mới |

**Không làm trong v1:** migrate Playwright, LLM/Vision sinh case, Allure/Extent thay `BaoCaoHtml`, rewrite Case Editor UI.
