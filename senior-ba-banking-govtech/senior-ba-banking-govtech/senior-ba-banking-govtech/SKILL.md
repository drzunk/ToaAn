---
name: senior-ba-banking-govtech
description: >-
  Bộ skill cho Senior Business Analyst làm dự án ngân hàng và dịch vụ công tại Việt Nam:
  khai thác yêu cầu, mô hình hóa quy trình BPMN as-is/to-be, viết BRD/SRS/user story và
  tiêu chí chấp nhận, mô hình dữ liệu và data mapping, đặc tả tích hợp API, kiến thức miền
  core banking, thanh toán NAPAS/VietQR, thủ tục hành chính, Cổng Dịch vụ công quốc gia,
  VNeID, chính quyền 2 cấp, khung pháp lý, quản lý bên liên quan, UAT và bàn giao.
  HÃY DÙNG SKILL NÀY khi người dùng nhắc tới: BA, phân tích nghiệp vụ, lấy yêu cầu, user
  story, BRD, SRS, use case, BPMN, quy trình nghiệp vụ, đặc tả, data dictionary, data
  mapping, tích hợp hệ thống, core banking, ngân hàng số, thanh toán, thủ tục hành chính,
  TTHC, dịch vụ công trực tuyến, một cửa liên thông, VNeID, Đề án 06, chuyển đổi số — hoặc
  khi họ cần biến yêu cầu mơ hồ thành tài liệu đội phát triển làm được.
  Không dùng cho: viết test case chi tiết, lập trình, quản lý dự án thuần.
license: MIT
metadata:
  version: "1.0"
  language: vi
  jurisdiction: VN
---

# Senior BA — Ngân hàng và Dịch vụ công

## Mục tiêu

Công việc của BA không phải là ghi lại điều khách hàng nói, mà là tìm ra điều họ thật sự
cần và diễn đạt nó chính xác đến mức đội phát triển không phải đoán.

Hai miền này khắc nghiệt theo hai cách khác nhau. Ngân hàng: yêu cầu sai làm mất tiền thật
và vướng quy định của cơ quan quản lý. Dịch vụ công: yêu cầu sai làm người dân phải đi lại
nhiều lần, và quy trình bị ràng buộc bởi văn bản pháp quy mà BA không được phép "tối ưu cho
gọn". Điểm chung: **không thể thiết kế giải pháp nếu chưa đọc văn bản gốc quy định nghiệp
vụ đó.**

## Bảng tra nhanh

| Bạn đang làm… | Đọc file |
|---|---|
| Phỏng vấn, workshop, đào yêu cầu từ bên liên quan | `references/elicitation.md` |
| Viết BRD, SRS, user story, tiêu chí chấp nhận | `references/requirements-spec.md` |
| Vẽ quy trình as-is/to-be, BPMN, swimlane, phân tích khoảng cách | `references/process-modeling.md` |
| Mô hình dữ liệu, từ điển dữ liệu, ánh xạ dữ liệu, di trú dữ liệu | `references/data-modeling.md` |
| Đặc tả tích hợp: API, message, mã lỗi, hợp đồng giữa các hệ thống | `references/integration-spec.md` |
| Kiến thức miền ngân hàng: sản phẩm, core, kênh, hạn mức, phí | `references/banking-domain.md` |
| Thanh toán: NAPAS, VietQR, ví, đối soát, thu hộ chi hộ | `references/payments-domain.md` |
| Thủ tục hành chính, DVC trực tuyến, một cửa liên thông, chính quyền 2 cấp | `references/public-services-domain.md` |
| Tích hợp nền tảng số quốc gia: VNeID, CSDLQG dân cư, NDXP, thanh toán trên Cổng | `references/govtech-integration.md` |
| Khung pháp lý hai miền, bảo vệ dữ liệu, giá trị pháp lý của chứng từ điện tử | `references/legal-compliance.md` |
| Thiết kế biểu mẫu điện tử, hành trình người dùng, khả năng tiếp cận | `references/ux-forms.md` |
| Quản lý bên liên quan, ưu tiên hóa, UAT, quản lý thay đổi, bàn giao | `references/stakeholder-delivery.md` |

## Câu hỏi khung trước mọi nhiệm vụ

Trước khi viết bất cứ tài liệu nào, phải trả lời được:

1. **Ai là người dùng thật, và ai là người quyết định?** Ở ngân hàng, người dùng cuối là
   khách hàng nhưng người duyệt yêu cầu là khối nghiệp vụ. Ở dịch vụ công, người dùng là
   người dân/doanh nghiệp còn người quyết định là cơ quan chủ quản, và hai bên có mục tiêu
   không giống nhau. Viết cho ai là chuyện phải rõ từ đầu.
2. **Văn bản nào quy định nghiệp vụ này?** Thông tư, nghị định, quyết định công bố thủ tục
   hành chính, quy chế nội bộ. Nếu chưa đọc văn bản gốc thì mọi đặc tả đều là phỏng đoán.
3. **Ràng buộc nào không được phép thay đổi?** Thời hạn giải quyết theo quy định, thành phần
   hồ sơ đã công bố, hạn mức do cơ quan quản lý đặt ra — BA không có quyền tối ưu những thứ
   này, chỉ có quyền kiến nghị sửa văn bản.
4. **Hệ thống hiện tại đang làm gì?** As-is trước to-be. Quy trình đang chạy luôn có lý do
   tồn tại, kể cả khi nhìn có vẻ vô lý.
5. **Dữ liệu đến từ đâu và đi về đâu?** Phần lớn rủi ro dự án nằm ở chỗ tích hợp, không phải
   ở giao diện.
6. **Đo thành công bằng gì?** Một yêu cầu không có tiêu chí đo được là một nguyện vọng.

## Nguyên tắc cốt lõi

**1. Yêu cầu phải nêu vấn đề, không nêu giải pháp.** Khi người dùng nói "cho tôi thêm một
nút xuất Excel", nhiệm vụ của BA là hỏi họ định làm gì với file đó. Rất nhiều lần câu trả
lời dẫn tới một giải pháp hoàn toàn khác và rẻ hơn. Ghi lại **nhu cầu** bên cạnh **đề xuất
của người dùng**, đừng trộn hai thứ làm một.

**2. Mỗi yêu cầu phải kiểm chứng được.** "Hệ thống phải thân thiện, nhanh chóng" không dùng
được. "95% giao dịch tra cứu trả kết quả dưới 2 giây" thì dùng được. Nếu không nghĩ ra cách
kiểm chứng, yêu cầu đó chưa xong.

**3. Ràng buộc pháp lý là dữ liệu đầu vào, không phải ý kiến.** Trích dẫn điều khoản cụ thể
vào tài liệu. Khi nghiệp vụ và văn bản mâu thuẫn, BA nêu mâu thuẫn ra chứ không tự chọn bên.

**4. Luồng ngoại lệ chiếm phần lớn công sức thật.** Happy path thường chỉ 20% khối lượng
code. Hồ sơ thiếu giấy tờ, thông tin không khớp cơ sở dữ liệu, người dân nộp sai cơ quan,
giao dịch bị đối tác từ chối, cán bộ cần trả hồ sơ bổ sung — đây mới là phần phải đặc tả kỹ.

**5. Không đặc tả cái mình chưa hiểu.** Nếu không giải thích được nghiệp vụ bằng lời của
mình cho người ngoài hiểu, đừng viết đặc tả. Quay lại hỏi tiếp.

**6. Mọi con số trong tài liệu phải có nguồn.** Hạn mức, thời hạn, phí, ngưỡng — ghi kèm
nguồn (điều khoản nào, ai xác nhận, ngày nào). Sáu tháng sau sẽ có người hỏi "sao lại là con
số này", và câu trả lời "em nghe anh A nói" không đứng vững.

**7. Tài liệu phục vụ người đọc, không phục vụ quy trình.** Một BRD 200 trang không ai đọc
kém giá trị hơn 20 trang có người đọc và phản biện.

## Quy trình làm việc chuẩn

```
1. Hiểu bối cảnh    → ai, vì sao, ràng buộc gì (đọc văn bản pháp quy trước)
2. Khảo sát as-is   → quy trình hiện tại, điểm đau, số liệu thực tế
3. Đào yêu cầu      → phỏng vấn, workshop, quan sát, phân tích tài liệu
4. Phân tích        → phân loại, tìm mâu thuẫn, tìm khoảng trống, đánh giá tác động
5. Mô hình hóa      → BPMN to-be, mô hình dữ liệu, sơ đồ tích hợp
6. Đặc tả           → BRD/SRS/user story + tiêu chí chấp nhận
7. Xác nhận         → rà soát với nghiệp vụ, với kỹ thuật, ký duyệt
8. Đồng hành        → làm rõ trong lúc phát triển, hỗ trợ UAT, quản lý thay đổi
```

Bước 1 và 2 bị bỏ qua nhiều nhất, và đó là nguyên nhân của hầu hết các lần làm lại. Ở dịch
vụ công, bỏ qua bước đọc quyết định công bố thủ tục hành chính gần như chắc chắn dẫn tới đặc
tả sai thành phần hồ sơ hoặc sai thời hạn giải quyết.

## Khi được giao một nhiệm vụ mơ hồ

Ví dụ: *"Làm cho tôi cái màn hình tra cứu hồ sơ"*. Đừng vẽ màn hình ngay. Trình tự:

1. Ai tra cứu — người dân, cán bộ tiếp nhận, hay lãnh đạo xem báo cáo? Ba đối tượng này cần
   ba thứ khác nhau.
2. Tra cứu để làm gì — biết hồ sơ tới bước nào, hay để xử lý tiếp?
3. Tra cứu bằng gì — mã hồ sơ, số định danh, số điện thoại? Mỗi cách có hệ quả về bảo mật.
4. Dữ liệu nằm ở đâu — một hệ thống hay nhiều hệ thống phải ghép?
5. Ai được thấy gì — người dân chỉ thấy hồ sơ của mình; cán bộ thấy hồ sơ trong thẩm quyền.
6. Bao nhiêu người dùng đồng thời, khối lượng dữ liệu bao nhiêu?

Sáu câu này thường lộ ra rằng "một màn hình" thật ra là ba tính năng cho ba vai trò.

## Anti-pattern

- **Chép lại lời người dùng làm đặc tả.** Đó là biên bản họp, không phải phân tích.
- **Vẽ to-be mà chưa hiểu as-is.** Quy trình cũ luôn có ràng buộc ẩn; bỏ qua chúng thì
  giải pháp mới sẽ chết ở khâu triển khai.
- **Đặc tả chỉ có happy path.** Đội phát triển sẽ tự quyết định luồng ngoại lệ, mỗi người
  một kiểu.
- **Dùng từ mơ hồ**: "xử lý tương ứng", "theo quy định hiện hành", "nếu cần thiết". Mỗi cụm
  này là một chỗ hệ thống sẽ được làm sai.
- **Tối ưu quy trình hành chính vượt thẩm quyền.** Rút bớt một bước mà văn bản pháp quy yêu
  cầu không phải cải tiến, đó là rủi ro pháp lý. Kiến nghị sửa văn bản là con đường đúng.
- **Không ghi lại quyết định và lý do.** Sáu tháng sau không ai nhớ vì sao chọn phương án B,
  và cuộc tranh luận sẽ lặp lại từ đầu.
- **Nhận thay đổi qua tin nhắn mà không cập nhật tài liệu.** Tài liệu lệch thực tế là tài
  liệu có hại hơn không có.

## Hoàn thành khi

- Mọi yêu cầu đều truy ngược được về một nhu cầu nghiệp vụ hoặc một điều khoản pháp quy.
- Mọi yêu cầu đều có tiêu chí chấp nhận kiểm chứng được.
- Luồng ngoại lệ được đặc tả không kém luồng chính.
- Mô hình dữ liệu và đặc tả tích hợp đã được đội kỹ thuật xác nhận là làm được.
- Nghiệp vụ đã rà soát và ký duyệt, không phải "đã gửi mail và im lặng".
- Có danh sách vấn đề còn mở, ghi rõ ai chịu trách nhiệm và hạn chót.
