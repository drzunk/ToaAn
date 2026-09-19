# Bên liên quan, ưu tiên hóa và bàn giao

Mục lục: [Phân tích bên liên quan](#phân-tích-bên-liên-quan) · [Đặc thù hai miền](#đặc-thù-hai-miền) · [Ưu tiên hóa](#ưu-tiên-hóa) · [Quản lý thay đổi](#quản-lý-thay-đổi-yêu-cầu) · [UAT](#hỗ-trợ-uat) · [Bàn giao](#bàn-giao-và-vận-hành) · [Nhật ký quyết định](#nhật-ký-quyết-định)

## Phân tích bên liên quan

Lập bảng ngay tuần đầu, cập nhật suốt dự án:

| Bên liên quan | Vai trò | Quan tâm chính | Ảnh hưởng | Thái độ | Cách làm việc |
|---|---|---|---|---|---|
| Trưởng phòng nghiệp vụ | Chủ sở hữu yêu cầu | Đúng nghiệp vụ, không tăng việc cho nhân viên | Cao | Ủng hộ | Họp hằng tuần, duyệt từng phần |
| Cán bộ tác nghiệp | Người dùng hằng ngày | Thao tác nhanh, ít bước | Thấp về quyết định, **cao về việc hệ thống có được dùng thật không** | Dè dặt | Quan sát tại chỗ, thử nghiệm sớm |
| Bộ phận pháp chế | Kiểm soát tuân thủ | Đúng quy định | Cao (quyền phủ quyết) | Trung lập | Hỏi sớm, hỏi bằng văn bản |
| Bộ phận CNTT | Vận hành | Ổn định, bảo trì được | Cao | Trung lập | Rà soát kỹ thuật định kỳ |
| Đơn vị chủ quản dữ liệu | Cấp quyền kết nối | An toàn dữ liệu, đúng cơ sở pháp lý | Cao (chặn được dự án) | Thận trọng | Liên hệ sớm nhất có thể |

**Nhóm hay bị bỏ qua và trả giá về sau:**
- **Cán bộ tác nghiệp**: không có quyền quyết định nhưng có quyền không dùng. Hệ thống họ
  ghét sẽ bị bỏ qua bằng cách làm song song trên Excel.
- **Bộ phận vận hành**: họ sẽ sống với hệ thống này nhiều năm. Hỏi họ về nhật ký, cảnh báo,
  công cụ xử lý sự cố ngay từ giai đoạn phân tích.
- **Bộ phận chăm sóc khách hàng / tổng đài**: họ biết rõ nhất người dùng vướng ở đâu, và họ
  cần công cụ tra cứu để trả lời khách.
- **Người dân / khách hàng cuối**: không có mặt trong phòng họp. BA là người duy nhất đại
  diện cho họ — đây là trách nhiệm nghề nghiệp, không phải việc thêm.

## Đặc thù hai miền

**Ngân hàng:**
- Nhiều tầng phê duyệt, quyết định chậm nhưng chắc. Dự trù thời gian cho việc này trong kế hoạch.
- Khối rủi ro và tuân thủ có quyền phủ quyết. Kéo họ vào từ đầu thay vì để họ phản đối ở phút chót.
- Khối nghiệp vụ và khối CNTT thường nói hai ngôn ngữ khác nhau. Việc phiên dịch giữa hai bên
  chiếm phần đáng kể thời gian của BA và nên được thừa nhận là công việc thật.

**Dịch vụ công:**
- Người ký duyệt và người hiểu nghiệp vụ thường là hai người khác nhau. Cần cả hai trong
  các buổi làm việc quan trọng.
- Ràng buộc pháp lý cứng hơn: nhiều thứ không thương lượng được bằng lý lẽ hiệu quả.
- Chu kỳ ngân sách và thủ tục đầu tư ảnh hưởng tới phạm vi và tiến độ. Hiểu lịch này giúp
  BA đề xuất phạm vi khả thi.
- Có thể xảy ra thay đổi tổ chức giữa dự án (sáp nhập đơn vị, đổi thẩm quyền). Thiết kế nên
  tránh gắn cứng vào cơ cấu tổ chức cụ thể — dùng cấu hình thay vì mã hóa cứng.

## Ưu tiên hóa

**MoSCoW** cho phạm vi tổng thể:
- **Phải có**: thiếu nó thì hệ thống không dùng được, hoặc vi phạm quy định
- **Nên có**: quan trọng nhưng có đường vòng tạm thời
- **Có thì tốt**: cải thiện trải nghiệm
- **Lần này không làm**: ghi rõ để khỏi tranh luận lại

Quy tắc thực dụng: nhóm "phải có" không nên vượt 60% khối lượng. Nếu mọi thứ đều "phải có"
thì việc ưu tiên chưa diễn ra.

**Ma trận giá trị – chi phí** để xếp thứ tự trong cùng nhóm:

```
Giá trị cao, chi phí thấp  → làm trước
Giá trị cao, chi phí cao   → chia nhỏ, làm phần lõi trước
Giá trị thấp, chi phí thấp → làm khi rảnh
Giá trị thấp, chi phí cao  → loại, và ghi lý do loại
```

**Yếu tố đặc thù cần cộng vào đánh giá giá trị:**
- **Ràng buộc pháp lý có thời hạn**: một yêu cầu đến từ văn bản có hiệu lực từ ngày X thì
  ngày X là hạn cứng, không phải hạn mong muốn.
- **Rủi ro tiền**: ở ngân hàng, tính năng ngăn mất tiền ưu tiên cao hơn tính năng tiện lợi.
- **Khối lượng thực tế**: tính năng phục vụ thủ tục có 10.000 hồ sơ/năm quan trọng hơn thủ
  tục có 50 hồ sơ/năm, dù bên nào cũng kêu gấp.

## Quản lý thay đổi yêu cầu

Thay đổi là bình thường. Thay đổi không được ghi nhận mới là vấn đề.

**Quy trình tối thiểu:**
```
1. Ghi nhận: ai đề nghị, nội dung, lý do, ngày
2. Phân tích tác động: ảnh hưởng yêu cầu nào, màn hình nào, dữ liệu nào, tích hợp nào,
   tài liệu nào phải sửa, công sức ước tính
3. Quyết định: ai duyệt, có đánh đổi gì (bỏ gì để thêm cái này)
4. Cập nhật: sửa tài liệu, thông báo các bên, cập nhật kế hoạch
```

Bước 2 là bước BA tạo ra giá trị rõ nhất. Một đề nghị nghe có vẻ nhỏ ("thêm một trường vào
biểu mẫu") có thể kéo theo: sửa cơ sở dữ liệu, sửa giao diện tích hợp, sửa báo cáo, cập nhật
tài liệu hướng dẫn, và di trú dữ liệu cũ. Trình bày đầy đủ chuỗi này thường làm người đề
nghị tự cân nhắc lại.

**Nguyên tắc:** không nhận thay đổi qua tin nhắn hay hành lang. Mọi thay đổi phải đi qua quy
trình, kể cả khi người đề nghị là lãnh đạo. Cách nói lịch sự: "Em ghi nhận, để em phân tích
tác động rồi báo lại trong hôm nay."

## Hỗ trợ UAT

BA không phải người kiểm thử, nhưng BA là người giúp nghiệp vụ nghiệm thu đúng.

**Việc của BA trước UAT:**
- Chuẩn bị kịch bản nghiệm thu theo **quy trình nghiệp vụ đầu–cuối**, không theo màn hình.
  Người nghiệp vụ nghiệm thu theo cách họ làm việc, không theo cách hệ thống được xây.
- Bảo đảm có dữ liệu thử phù hợp, gồm cả các trường hợp khó.
- Nêu rõ tiêu chí đạt/không đạt cho từng kịch bản, dựa trên tiêu chí chấp nhận đã viết.

**Trong UAT:**
- Ngồi cùng người nghiệm thu, quan sát chỗ họ lúng túng — đó là phát hiện giá trị hơn cả
  danh sách lỗi.
- Phân loại phản hồi: lỗi (không đúng đặc tả), yêu cầu mới (đúng đặc tả nhưng đặc tả chưa
  đủ), hay hiểu nhầm (cần hướng dẫn). Ba loại này xử lý khác nhau, và trộn chúng lại là
  nguyên nhân của những tranh cãi kéo dài về "cái này có trong phạm vi không".
- Ghi lại ngay, đừng để cuối buổi nhớ lại.

**Sau UAT:** danh sách vấn đề còn tồn với mức độ và hướng xử lý (sửa trước khi lên chạy thật,
sửa ở đợt sau, hoặc chấp nhận). Mỗi mục "chấp nhận" phải có người chấp nhận rủi ro bằng tên
cụ thể.

## Bàn giao và vận hành

Dự án không kết thúc ở ngày lên chạy thật. Những thứ BA nên chuẩn bị:

**Tài liệu cho người dùng:** hướng dẫn theo nhiệm vụ ("cách nộp hồ sơ bổ sung") chứ không
theo chức năng ("màn hình quản lý hồ sơ"). Ngắn, có ảnh, đặt ở nơi người dùng đang cần chứ
không phải trong một tệp tải về.

**Tài liệu cho vận hành:** danh sách sự cố thường gặp và cách xử lý, ý nghĩa các mã lỗi,
liên hệ của từng hệ thống tích hợp, cách tra cứu một hồ sơ/giao dịch khi khách hàng khiếu nại.

**Đào tạo:** cán bộ tác nghiệp cần được đào tạo trước ngày lên chạy thật, và cần có người hỗ
trợ tại chỗ trong tuần đầu. Ở dịch vụ công, tuần đầu thường trùng với thời điểm người dân đổ
dồn vào, nên bố trí nhân sự hỗ trợ là việc phải lên kế hoạch từ sớm.

**Theo dõi sau khi chạy thật:** thống nhất trước các chỉ số sẽ theo dõi trong 2–4 tuần đầu
(xem `ux-forms.md`), và lịch rà soát để quyết định điều chỉnh gì.

**Chuyển giao cho đội bảo trì:** tài liệu phải phản ánh hệ thống **như đã xây**, không phải
như đã thiết kế ban đầu. Cập nhật lần cuối trước khi đóng dự án là việc tẻ nhạt nhưng quyết
định tài liệu còn giá trị hay thành rác.

## Nhật ký quyết định

Một tệp duy nhất, cập nhật liên tục. Đây là công cụ đơn giản nhất và hữu ích nhất mà nhiều
BA bỏ qua.

| Ngày | Quyết định | Bối cảnh / các phương án | Lý do chọn | Ai quyết | Ảnh hưởng tới |
|---|---|---|---|---|---|
| 12/3 | Không cho sửa thông tin lấy từ dữ liệu gốc | A: cho sửa tự do; B: khóa và hướng dẫn điều chỉnh dữ liệu gốc | Tránh lệch dữ liệu giữa hệ thống, phù hợp nguyên tắc nguồn đúng duy nhất | Trưởng phòng nghiệp vụ | UC-012, FR-045 |
| 20/3 | Thanh toán là điều kiện trả kết quả, không phải điều kiện tiếp nhận | A: thu trước khi tiếp nhận; B: thu trước khi trả | Tránh phải hoàn tiền khi hồ sơ bị từ chối | Lãnh đạo đơn vị | Quy trình, FR-060 |

Giá trị: sáu tháng sau khi có người hỏi "vì sao lại làm thế này", câu trả lời có sẵn kèm tên
người quyết định. Không có nhật ký này, cuộc tranh luận sẽ lặp lại từ đầu và thường ra kết
quả khác — rồi hệ thống phải sửa lần hai.
