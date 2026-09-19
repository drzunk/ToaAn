# Khai thác yêu cầu

Mục lục: [Chọn kỹ thuật](#chọn-kỹ-thuật-theo-tình-huống) · [Phỏng vấn](#phỏng-vấn) · [Workshop](#workshop) · [Quan sát](#quan-sát-tại-chỗ) · [Phân tích tài liệu](#phân-tích-tài-liệu) · [Câu hỏi theo miền](#bộ-câu-hỏi-theo-miền) · [Xử lý mâu thuẫn](#xử-lý-thông-tin-mâu-thuẫn)

## Chọn kỹ thuật theo tình huống

| Tình huống | Kỹ thuật phù hợp | Lý do |
|---|---|---|
| Chưa biết gì về nghiệp vụ | Phân tích tài liệu trước, rồi phỏng vấn | Đừng tiêu thời gian của chuyên gia để hỏi điều có trong văn bản |
| Nhiều bên liên quan bất đồng | Workshop có người điều phối | Ép các bên nghe nhau, ra quyết định tại chỗ |
| Quy trình phức tạp, người làm khó diễn đạt | Quan sát tại chỗ | Người làm lâu năm thường bỏ qua bước mà họ cho là hiển nhiên |
| Cần con số thực tế | Phân tích dữ liệu hệ thống hiện tại | Ý kiến chủ quan về "thường xuyên" rất lệch so với số liệu |
| Yêu cầu mơ hồ, khó hình dung | Bản mẫu (prototype) | Người ta phản biện cái nhìn thấy dễ hơn cái phải tưởng tượng |
| Nhiều người dùng phân tán | Khảo sát + phỏng vấn sâu vài người | Khảo sát cho bề rộng, phỏng vấn cho chiều sâu |

Thực tế luôn phải phối hợp nhiều kỹ thuật. Một nguồn duy nhất luôn thiếu.

## Phỏng vấn

**Chuẩn bị (quan trọng hơn buổi phỏng vấn):**
- Đọc trước tài liệu, văn bản pháp quy, màn hình hệ thống hiện có. Đến buổi phỏng vấn với
  giả thuyết, không đến với trang giấy trắng.
- Gửi trước 3–5 câu hỏi chính để người được phỏng vấn chuẩn bị số liệu.
- Xác định rõ mục tiêu buổi này: hiểu quy trình, hay xác nhận một giả thuyết, hay lấy quyết
  định? Đừng gộp cả ba.

**Trong buổi:**
- Mở bằng câu hỏi rộng, thu hẹp dần: "Anh mô tả giúp em một ngày làm việc điển hình" →
  "Bước nào tốn thời gian nhất" → "Cụ thể hồ sơ dạng này thì anh làm sao".
- Hỏi về trường hợp thật, không hỏi về nguyên tắc: "Lần gần nhất anh phải trả hồ sơ là khi
  nào, vì lý do gì" hiệu quả hơn "Khi nào thì trả hồ sơ".
- Đào tới ngoại lệ: "Nếu thông tin không khớp thì sao?", "Nếu người dân không có giấy đó?",
  "Nếu đối tác không phản hồi?". Chuỗi câu hỏi "nếu... thì sao" là công cụ chính của BA.
- Hỏi về tần suất và khối lượng: mỗi ngày bao nhiêu hồ sơ, bao nhiêu phần trăm rơi vào
  trường hợp này. Con số quyết định mức đầu tư cho từng luồng.
- Im lặng sau câu trả lời. Phần thông tin giá trị nhất thường đến sau ba giây im lặng.
- Ghi lại nguyên văn thuật ngữ nghiệp vụ họ dùng, đừng tự dịch sang từ của mình.

**Tránh:**
- Câu hỏi dẫn dắt: "Chắc anh cũng muốn hệ thống tự động duyệt phải không?"
- Hỏi giải pháp thay vì hỏi vấn đề: "Anh muốn màn hình thế nào?"
- Gật đầu cho qua khi không hiểu. Hỏi lại ngay, kể cả khi thấy ngại.

**Sau buổi:** gửi biên bản tóm tắt trong 24 giờ, nêu rõ các điểm đã hiểu và các điểm còn
băn khoăn, kèm hạn xác nhận. Im lặng không phải là đồng ý — với quyết định quan trọng phải
có xác nhận tường minh.

## Workshop

Dùng khi cần nhiều bên cùng thống nhất, hoặc khi phỏng vấn riêng cho ra các câu trả lời mâu
thuẫn nhau.

**Chuẩn bị:** gửi trước tài liệu nền và câu hỏi cần quyết. Đảm bảo có mặt người **có thẩm
quyền quyết định**, không chỉ người biết việc — đây là lỗi tổ chức workshop phổ biến nhất.

**Cấu trúc 2 giờ điển hình:**
```
10' Mục tiêu và luật chơi
30' Cùng vẽ quy trình as-is lên bảng (dùng ký hiệu đơn giản, không cần chuẩn BPMN)
30' Đánh dấu điểm đau, bỏ phiếu mức độ nghiêm trọng
30' Phác thảo to-be cho 2–3 điểm đau lớn nhất
15' Chốt quyết định, ghi vấn đề còn mở
5'  Bước tiếp theo, ai làm gì trước khi nào
```

**Vai trò**: BA điều phối, không phát biểu quan điểm nhiều. Nếu BA vừa điều phối vừa bảo vệ
một phương án thì workshop mất tính khách quan — khi cần bảo vệ phương án, hãy nhờ người
khác điều phối.

**Kết quả bắt buộc mang về**: ảnh chụp bảng, danh sách quyết định, danh sách vấn đề còn mở
với người chịu trách nhiệm. Không có ba thứ này thì workshop chỉ là buổi nói chuyện.

## Quan sát tại chỗ

Kỹ thuật bị đánh giá thấp nhưng hiệu quả nhất ở hai miền này.

Ở quầy giao dịch ngân hàng hoặc bộ phận một cửa, ngồi quan sát nửa ngày sẽ thấy những thứ
không ai kể trong phỏng vấn: cán bộ mở ba hệ thống song song và gõ lại dữ liệu giữa chúng,
một cuốn sổ giấy bên cạnh máy tính, mẩu giấy nhớ dán màn hình ghi các mã hay dùng, người dân
phải gọi điện hỏi lại vì tờ khai có từ họ không hiểu.

**Cách làm:**
- Xin phép và giải thích rõ mục đích — không phải để đánh giá cán bộ.
- Quan sát trước, hỏi sau. Ghi lại thời gian từng bước.
- Chú ý đặc biệt vào **giải pháp tự chế**: mỗi cuốn sổ ngoài hệ thống, mỗi file Excel cá
  nhân, mỗi bước gõ lại dữ liệu là một yêu cầu chưa được đáp ứng.
- Đếm số lần chuyển đổi giữa các hệ thống trong một hồ sơ.

## Phân tích tài liệu

Thứ tự đọc ở hai miền:

**Ngân hàng:** quy chế sản phẩm → quy trình nghiệp vụ nội bộ → biểu phí → thông tư/quyết định
của Ngân hàng Nhà nước liên quan → tài liệu hệ thống hiện tại → báo cáo sự cố và khiếu nại
của khách hàng (nguồn vàng để tìm điểm đau thật).

**Dịch vụ công:** quyết định công bố thủ tục hành chính (nguồn chuẩn về thành phần hồ sơ,
trình tự, thời hạn, phí lệ phí) → văn bản quy phạm pháp luật chuyên ngành → quy trình nội bộ
của cơ quan → dữ liệu trên Cổng Dịch vụ công (tỷ lệ hồ sơ trực tuyến, tỷ lệ trả hồ sơ, thời
gian xử lý thực tế).

Khi đọc văn bản pháp quy, trích ra ba loại thông tin và ghi kèm số điều khoản: **ràng buộc
bắt buộc**, **con số cụ thể** (thời hạn, phí, ngưỡng), và **chỗ văn bản để ngỏ** cho cơ quan
tự quy định — chỗ để ngỏ chính là chỗ BA có không gian thiết kế.

## Bộ câu hỏi theo miền

**Cho nghiệp vụ ngân hàng:**
- Sản phẩm này áp dụng cho nhóm khách hàng nào, có loại trừ ai không?
- Hạn mức và phí lấy từ đâu — cấu hình được hay cố định trong mã?
- Ai được phép làm, ai được phép duyệt, ngưỡng nào thì lên cấp cao hơn?
- Giao dịch thất bại giữa chừng thì tiền đi đâu, ai xử lý?
- Nghiệp vụ này phát sinh bút toán gì, vào tài khoản kế toán nào?
- Cần báo cáo gì cho Ngân hàng Nhà nước, tần suất ra sao?
- Kênh nào được dùng: quầy, internet banking, mobile, ATM? Có khác nhau về hạn mức không?

**Cho dịch vụ công:**
- Thủ tục này đã được công bố chưa, mã thủ tục là gì?
- Thành phần hồ sơ gồm những gì, giấy tờ nào có thể thay bằng dữ liệu từ cơ sở dữ liệu quốc
  gia thay vì bắt người dân nộp?
- Thời hạn giải quyết theo quy định là bao nhiêu ngày làm việc, tính từ mốc nào?
- Cơ quan nào tiếp nhận, cơ quan nào giải quyết, có liên thông với cơ quan khác không?
- Sau sắp xếp chính quyền 2 cấp, thẩm quyền giải quyết thủ tục này nằm ở cấp nào?
- Kết quả trả về dạng gì — bản điện tử có chữ ký số, bản giấy, hay cả hai?
- Có phí, lệ phí không, thu qua kênh nào?
- Trường hợp nào phải trả hồ sơ bổ sung, quy trình bổ sung ra sao, có tính lại thời hạn không?
- Người dân không có điện thoại thông minh thì làm thế nào?

## Xử lý thông tin mâu thuẫn

Mâu thuẫn là chuyện bình thường, và cách xử lý phân biệt BA junior với senior.

1. **Ghi lại cả hai phiên bản** kèm nguồn, đừng chọn bên vội.
2. **Tìm nguyên nhân**: hai người mô tả hai trường hợp khác nhau? Một người nói quy định,
   người kia nói thực tế? Quy trình đã đổi mà chưa cập nhật tài liệu?
3. **Đối chiếu với nguồn có thẩm quyền cao hơn**: văn bản pháp quy > quy chế nội bộ > lời kể.
4. **Đưa ra bàn công khai** nếu vẫn mâu thuẫn. Đừng đi hỏi riêng từng người để rồi tự chắp
   vá — cách đó tạo ra một phiên bản không ai đồng ý.
5. **Ghi quyết định cuối cùng và ai quyết định**, vào nhật ký quyết định của dự án.

Riêng ở dịch vụ công, mâu thuẫn hay gặp nhất là giữa **quy trình theo văn bản** và **cách
làm thực tế tại đơn vị**. Cả hai đều là dữ liệu quan trọng: văn bản là ràng buộc bắt buộc,
thực tế cho biết vì sao văn bản không chạy được. Đặc tả tốt phải xử lý cả hai, không phải
chỉ chép lại một bên.
