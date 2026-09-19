# Khung pháp lý cho BA

> **Không phải tư vấn pháp lý.** Tài liệu này giúp BA biết phải đọc gì và hỏi gì. Mọi kết
> luận pháp lý phải do bộ phận pháp chế đưa ra. Văn bản thay đổi thường xuyên — luôn kiểm
> tra bản hiện hành.

Mục lục: [Cách làm việc với văn bản](#cách-làm-việc-với-văn-bản-pháp-quy) · [Bảng văn bản](#bảng-văn-bản-hay-dùng) · [Dữ liệu cá nhân](#bảo-vệ-dữ-liệu-cá-nhân) · [Giao dịch điện tử](#giao-dịch-điện-tử) · [Ngân hàng](#quy-định-chuyên-ngành-ngân-hàng) · [Mâu thuẫn](#xử-lý-mâu-thuẫn-pháp-lý) · [Ma trận tuân thủ](#ma-trận-tuân-thủ)

## Cách làm việc với văn bản pháp quy

BA không cần trở thành luật sư, nhưng cần biết đọc văn bản đủ để không thiết kế sai. Quy
trình bốn bước:

**1. Tìm đúng văn bản đang có hiệu lực.** Kiểm tra: văn bản này có bị thay thế chưa, có văn
bản sửa đổi bổ sung nào không, ngày hiệu lực là khi nào. Dùng nguồn chính thức, không dùng
bài tóm tắt trên mạng làm căn cứ.

**2. Trích ra ba loại nội dung:**
- **Ràng buộc bắt buộc** — có chữ "phải", "không được". Đây là thứ thiết kế không được vi phạm.
- **Con số cụ thể** — thời hạn, ngưỡng, tỷ lệ, số lần. Đây là tham số hệ thống.
- **Chỗ để ngỏ** — "do đơn vị quy định", "theo quy định của tổ chức". Đây là không gian thiết
  kế của BA, và cũng là chỗ phải hỏi nghiệp vụ để chốt.

**3. Dịch sang ràng buộc thiết kế.** Mỗi trích dẫn phải trả lời: điều này ảnh hưởng tới màn
hình nào, quy tắc nghiệp vụ nào, dữ liệu nào?

Ví dụ: quy định "phải ghi nhật ký mỗi lần truy cập dữ liệu khách hàng" → sinh ra yêu cầu về
bảng nhật ký, các trường cần ghi, thời gian lưu, màn hình tra cứu nhật ký, và phân quyền xem
nhật ký. Một câu trong văn bản thành năm yêu cầu trong đặc tả.

**4. Hỏi khi không chắc.** Trích dẫn nguyên văn điều khoản, nêu hai cách hiểu, và hỏi pháp
chế chọn cách nào. Đừng tự diễn giải rồi thiết kế theo.

## Bảng văn bản hay dùng

| Lĩnh vực | Văn bản | Nội dung chính với BA |
|---|---|---|
| Dữ liệu cá nhân | Luật Bảo vệ dữ liệu cá nhân 2025 (Luật 91/2025/QH15), hiệu lực 01/01/2026 | Quyền chủ thể dữ liệu, nghĩa vụ tổ chức xử lý |
| Dữ liệu cá nhân | Nghị định 356/2025/NĐ-CP, hiệu lực 01/01/2026, thay thế Nghị định 13/2023 | Phân loại dữ liệu cơ bản/nhạy cảm, yêu cầu về đồng ý, chuyển dữ liệu ra nước ngoài, biểu mẫu |
| Giao dịch điện tử | Luật Giao dịch điện tử 2023, hiệu lực 01/7/2024 | Chữ ký điện tử, chữ ký số, chứng thư điện tử, giá trị pháp lý |
| Dịch vụ công trực tuyến | Nghị định 42/2022/NĐ-CP | Hai mức toàn trình / một phần, chuẩn hóa mã và tên dịch vụ, biểu mẫu điện tử |
| Thủ tục hành chính | Nghị định 118/2025/NĐ-CP, hiệu lực 01/7/2025 | Cơ chế một cửa, Trung tâm Phục vụ hành chính công, tiếp nhận không phụ thuộc địa giới |
| Định danh điện tử | Nghị định 59/2022/NĐ-CP; Quyết định 06/QĐ-TTg (Đề án 06) | Định danh, xác thực điện tử, khai thác dữ liệu dân cư |
| Đăng nhập Cổng DVCQG | Quyết định 29/2026/QĐ-TTg | Từ 20/7/2026 dùng tài khoản định danh điện tử |
| Ngân hàng số | Thông tư 50/2024/TT-NHNN, hiệu lực 01/01/2025 | An toàn bảo mật dịch vụ trực tuyến, xác thực theo ngưỡng, bảo mật dữ liệu khách hàng |

Bảng này là điểm khởi đầu, không phải danh sách đầy đủ. Mỗi nghiệp vụ chuyên ngành còn có
văn bản riêng, và đó là thứ BA phải tự tìm cho từng dự án.

## Bảo vệ dữ liệu cá nhân

Nghị định 356/2025/NĐ-CP điều chỉnh danh mục dữ liệu cá nhân cơ bản và nhạy cảm, siết chặt
yêu cầu về sự đồng ý, làm rõ cơ chế và thời hạn thực hiện quyền của chủ thể dữ liệu, và quy
định cụ thể trường hợp và điều kiện chuyển giao dữ liệu cá nhân ra nước ngoài.

**Dịch sang yêu cầu hệ thống:**

| Nguyên tắc | Yêu cầu cụ thể BA phải viết |
|---|---|
| Sự đồng ý phải rõ ràng | Ô đồng ý không tick sẵn; tách riêng từng mục đích; lưu bản ghi đồng ý kèm thời điểm, phiên bản điều khoản, phương thức |
| Tối thiểu hóa dữ liệu | Rà từng trường trên biểu mẫu, hỏi "thiếu trường này thì nghiệp vụ có làm được không"; nếu có thì bỏ |
| Giới hạn mục đích | Dữ liệu thu cho mục đích A không dùng cho mục đích B nếu không có cơ sở |
| Quyền truy cập | Chức năng xuất dữ liệu cá nhân của chính mình, trong thời hạn quy định |
| Quyền rút đồng ý | Chức năng rút, và hệ quả của việc rút phải rõ ràng với người dùng |
| Quyền chỉnh sửa | Luồng yêu cầu chỉnh sửa; nếu dữ liệu gốc ở cơ sở dữ liệu khác thì hướng dẫn đúng nơi |
| An toàn dữ liệu | Mã hóa khi lưu và khi truyền với dữ liệu nhạy cảm; phân quyền; nhật ký truy cập |
| Chuyển dữ liệu ra nước ngoài | Rà soát mọi dịch vụ bên thứ ba: lưu trữ đám mây, phân tích, theo dõi lỗi, gửi thông báo |
| Thông báo sự cố | Quy trình và cơ chế thông báo hàng loạt cho người bị ảnh hưởng |

**Rà soát dòng dữ liệu ra ngoài** là việc BA nên chủ động đề xuất: lập danh sách mọi dịch vụ
bên thứ ba mà hệ thống gọi tới, dữ liệu gì được gửi đi, máy chủ đặt ở đâu. Danh sách này
thường dài hơn mọi người tưởng và chứa vài thứ không ai để ý.

## Giao dịch điện tử

Câu hỏi BA phải trả lời cho mỗi chứng từ trong hệ thống:

- Chứng từ này có cần giá trị pháp lý như bản giấy không?
- Cần ký ở hình thức nào, ai ký?
- Lưu trữ bao lâu, ở định dạng nào, kiểm tra lại tính toàn vẹn thế nào sau nhiều năm?
- Khi tranh chấp, dùng gì để chứng minh nội dung và thời điểm?

Chi tiết về chữ ký số xem `govtech-integration.md`.

## Quy định chuyên ngành ngân hàng

Thông tư 50/2024/TT-NHNN quy định về an toàn, bảo mật cho việc cung cấp dịch vụ trực tuyến
trong ngành Ngân hàng, hiệu lực từ 01/01/2025, áp dụng với tổ chức tín dụng, chi nhánh ngân
hàng nước ngoài, tổ chức cung ứng dịch vụ trung gian thanh toán và công ty thông tin tín dụng.

**Các nội dung sinh ra yêu cầu hệ thống trực tiếp:**

- **Xác thực theo ngưỡng giao dịch**: giao dịch chuyển tiền của cá nhân trên 10 triệu đồng,
  hoặc tổng trong ngày vượt 20 triệu đồng, hoặc khi thay đổi thiết bị thực hiện giao dịch
  Mobile Banking, phải áp dụng xác thực sinh trắc học. → Sinh ra: bộ đếm lũy kế ngày, cơ chế
  nhận diện thiết bị, luồng xác thực bổ sung, và quy tắc nghiệp vụ có tham số cấu hình được.
- **Kiểm tra khi truy cập lần đầu hoặc bằng thiết bị khác**: với khách hàng cá nhân, tối thiểu
  gồm khớp đúng SMS OTP hoặc Voice OTP, đồng thời khớp sinh trắc học nếu quy định chuyên ngành
  yêu cầu thu thập và lưu trữ thông tin sinh trắc học. → Sinh ra: quản lý danh sách thiết bị
  tin cậy, luồng thiết bị mới.
- **Bảo mật thông tin khách hàng** (Điều 19): dữ liệu khách hàng phải được bảo đảm an toàn
  theo quy định pháp luật; mã khóa bí mật, mã PIN, thông tin sinh trắc học khi lưu trữ phải
  được mã hóa hoặc che giấu; thiết lập quyền truy cập đúng chức năng nhiệm vụ và giám sát mỗi
  lần truy cập; quản lý truy cập thiết bị lưu trữ dữ liệu; thông báo cho khách hàng khi xảy
  ra sự cố lộ lọt và báo cáo Ngân hàng Nhà nước. → Sinh ra: yêu cầu mã hóa, phân quyền theo
  chức danh, nhật ký truy cập, quy trình thông báo sự cố.
- **Không gửi tin nhắn, thư điện tử chứa đường dẫn liên kết** cho khách hàng, trừ trường hợp
  theo yêu cầu của khách hàng. → Sinh ra: rà soát toàn bộ mẫu thông báo.
- **Ứng dụng không được có chức năng ghi nhớ mã khóa bí mật truy cập.** → Sinh ra: ràng buộc
  thiết kế màn hình đăng nhập.

Khi viết đặc tả, trích dẫn điều khoản vào phần cơ sở pháp lý và tham chiếu tới nó từ quy tắc
nghiệp vụ. Đội kiểm thử và kiểm toán nội bộ sẽ dùng chính ánh xạ này.

## Xử lý mâu thuẫn pháp lý

Hai mâu thuẫn hay gặp và cách BA nên xử lý:

**1. Quyền xóa dữ liệu cá nhân ↔ nghĩa vụ lưu trữ chứng từ.** Chủ thể dữ liệu có quyền yêu
cầu xóa, nhưng pháp luật kế toán và lưu trữ buộc giữ chứng từ nhiều năm. Cách xử lý: nêu mâu
thuẫn bằng văn bản, xin ý kiến pháp chế, và thiết kế theo hướng phân tách — xóa/ẩn dữ liệu
không cần cho nghĩa vụ lưu trữ, giữ phần bắt buộc với phân quyền chặt. **Không tự quyết.**

**2. Quy trình theo văn bản ↔ thực tế tại đơn vị.** Văn bản quy định một đằng, đơn vị làm một
nẻo vì văn bản không chạy được. Cách xử lý: thiết kế theo văn bản là mặc định, ghi nhận thực
tế như một vấn đề cần giải quyết, và đề xuất kiến nghị sửa văn bản nếu thực tế có lý. Thiết
kế hệ thống chạy theo cách làm sai quy định là đưa rủi ro cho khách hàng.

**Nguyên tắc chung:** BA nêu vấn đề, người có thẩm quyền quyết định, và quyết định được ghi
lại kèm người quyết. Không bao giờ để một lựa chọn pháp lý nằm im lặng trong mã nguồn.

## Ma trận tuân thủ

Sản phẩm bàn giao nên có, đặc biệt với dự án khu vực công và ngân hàng:

| Văn bản | Điều khoản | Yêu cầu tóm tắt | Yêu cầu hệ thống | Kiểm chứng bằng |
|---|---|---|---|---|
| TT 50/2024 | Đ.19 | Mã hóa/che giấu PIN, sinh trắc học khi lưu | FR-087, FR-088 | Kiểm tra dữ liệu lưu trữ |
| NĐ 42/2022 | — | Chuẩn hóa mã, tên dịch vụ, có biểu mẫu điện tử | FR-012 | Đối chiếu với CSDLQG về TTHC |
| NĐ 118/2025 | — | Tiếp nhận không phụ thuộc địa giới trong tỉnh | FR-031, FR-032 | Kịch bản nộp chéo địa bàn |
| Luật BVDLCN | — | Sự đồng ý rõ ràng, tách theo mục đích | FR-004 | Rà soát màn hình đăng ký |

Giá trị của ma trận này: khi kiểm toán hoặc thanh tra hỏi "hệ thống đáp ứng yêu cầu này ở
đâu", câu trả lời có sẵn trong một bảng thay vì phải dò lại toàn bộ tài liệu.
