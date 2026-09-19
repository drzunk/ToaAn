# Mô hình hóa quy trình nghiệp vụ

Mục lục: [As-is trước](#as-is-trước-to-be) · [Ký hiệu BPMN](#ký-hiệu-bpmn-cần-dùng) · [Swimlane](#swimlane-và-phân-định-trách-nhiệm) · [Phân tích khoảng cách](#phân-tích-khoảng-cách) · [Quy trình hành chính](#đặc-thù-quy-trình-hành-chính) · [Quy trình ngân hàng](#đặc-thù-quy-trình-ngân-hàng) · [Lỗi hay gặp](#lỗi-hay-gặp-khi-vẽ)

## As-is trước to-be

Cám dỗ lớn nhất là vẽ ngay quy trình lý tưởng. Đừng. Quy trình hiện tại chứa thông tin mà
không ai nói ra trong phỏng vấn: các bước kiểm soát sinh ra sau một sự cố, các bước thủ công
bù cho hệ thống thiếu tính năng, các bước tồn tại vì một văn bản nội bộ.

**Vẽ as-is đúng cách:**
- Vẽ cái đang chạy thật, không vẽ cái đáng lẽ phải chạy. Hai thứ này thường khác nhau.
- Đánh dấu ở mỗi bước: ai làm, mất bao lâu, dùng hệ thống nào, tỷ lệ lỗi/trả lại.
- Đánh dấu các bước thủ công, các lần gõ lại dữ liệu, các lần chờ.
- Hỏi về từng bước kiểm soát: "bước này sinh ra vì lý do gì?" Câu trả lời quyết định bước
  đó có được bỏ trong to-be hay không.

**Số liệu cần có trên sơ đồ as-is** (không có số liệu thì không chứng minh được cải tiến):
tổng thời gian từ đầu đến cuối, thời gian xử lý thực tế so với thời gian chờ, số lần chuyển
tay, tỷ lệ hồ sơ phải làm lại, khối lượng mỗi ngày.

## Ký hiệu BPMN cần dùng

Không cần dùng hết BPMN. Bộ ký hiệu dưới đây đủ cho 95% trường hợp và quan trọng là **người
nghiệp vụ đọc hiểu được**:

| Ký hiệu | Dùng khi |
|---|---|
| Sự kiện bắt đầu / kết thúc | Vòng tròn mảnh / đậm. Mỗi quy trình có thể nhiều điểm kết thúc |
| Tác vụ (task) | Hình chữ nhật bo góc. Động từ + danh từ: "Thẩm định hồ sơ" |
| Cổng loại trừ (XOR) | Hình thoi có dấu ×. Chọn đúng một nhánh |
| Cổng song song (AND) | Hình thoi có dấu +. Các nhánh chạy cùng lúc, chờ hết mới đi tiếp |
| Cổng bao hàm (OR) | Hình thoi có vòng tròn. Có thể nhiều nhánh cùng chạy |
| Sự kiện hẹn giờ | Vòng tròn có đồng hồ. Dùng cho thời hạn, nhắc hạn, tự động từ chối |
| Sự kiện message | Vòng tròn có phong bì. Chờ/gửi tín hiệu sang hệ thống khác |
| Tiến trình con | Chữ nhật có dấu +. Gom chi tiết để sơ đồ chính đọc được |
| Kho dữ liệu | Hình trụ. Đánh dấu nơi đọc/ghi dữ liệu quan trọng |

**Quy tắc đặt tên:** tác vụ dùng động từ chủ động ("Xác minh thông tin cư trú"), cổng đặt
dưới dạng câu hỏi ("Hồ sơ đầy đủ?"), nhánh ghi rõ điều kiện ("Đủ" / "Thiếu"). Cổng không có
nhãn điều kiện trên nhánh là lỗi phổ biến nhất khiến sơ đồ mơ hồ.

**Độ chi tiết:** sơ đồ chính nên vừa một trang A4 ngang, khoảng 10–15 tác vụ. Chi tiết hơn
thì tách thành tiến trình con. Sơ đồ phải in ra dán tường mà người nghiệp vụ chỉ trỏ được —
đó là thước đo thực dụng.

## Swimlane và phân định trách nhiệm

Mỗi lane một vai trò, không phải một con người hay một phòng ban. Ở dịch vụ công thường là:
Người dân | Cán bộ tiếp nhận | Cán bộ thụ lý | Lãnh đạo phê duyệt | Hệ thống | Cơ quan phối hợp.

Mỗi lần mũi tên cắt qua ranh giới lane là một lần chuyển giao — và mỗi lần chuyển giao là
một điểm có thể trễ, mất, hoặc hiểu nhầm. Đếm số lần cắt lane là cách nhanh nhất để đo độ
phức tạp của quy trình.

Bổ sung bằng **ma trận RACI** cho các bước gây tranh cãi về trách nhiệm:

| Bước | Tiếp nhận | Thụ lý | Lãnh đạo | Phòng chuyên môn |
|---|---|---|---|---|
| Kiểm tra tính hợp lệ hồ sơ | R | C | I | — |
| Thẩm định nội dung | I | R | A | C |
| Ký kết quả | I | C | R/A | I |

R làm, A chịu trách nhiệm cuối, C được hỏi ý kiến, I được thông báo. Ô có hai chữ R là dấu
hiệu trách nhiệm chưa rõ — đây chính là chỗ hồ sơ hay bị đùn đẩy.

## Phân tích khoảng cách

Đặt as-is và to-be cạnh nhau, lập bảng:

| Bước | As-is | To-be | Thay đổi | Tác động | Ai bị ảnh hưởng |
|---|---|---|---|---|---|
| Xác minh cư trú | Cán bộ gọi điện xác minh, 1–2 ngày | Tra cứu trực tuyến từ cơ sở dữ liệu quốc gia | Tự động hóa | Giảm 1,5 ngày | Cán bộ thụ lý, cần đào tạo |
| Nộp bản sao hộ khẩu | Người dân nộp bản sao | Bỏ, lấy dữ liệu từ hệ thống | Bỏ bước | Giảm 1 giấy tờ | Người dân, cần truyền thông |

Cột **tác động** và **ai bị ảnh hưởng** quan trọng không kém cột thay đổi. Một cải tiến bỏ
qua yếu tố con người sẽ bị kháng cự âm thầm và quy trình cũ sẽ tiếp tục chạy song song.

**Kiểm tra tính khả thi pháp lý của to-be**: với mỗi bước bị bỏ, trả lời — bước đó có phải
là yêu cầu của văn bản pháp quy không? Nếu có thì không bỏ được bằng thiết kế hệ thống, phải
kiến nghị sửa văn bản. Ghi rõ điều này trong tài liệu thay vì âm thầm bỏ qua.

## Đặc thù quy trình hành chính

**Nguồn chuẩn là quyết định công bố thủ tục hành chính**, không phải lời kể. Nó quy định:
trình tự thực hiện, cách thức thực hiện, thành phần hồ sơ, thời hạn giải quyết, đối tượng,
cơ quan giải quyết, kết quả, phí lệ phí, và căn cứ pháp lý. Mọi sơ đồ quy trình phải khớp
với văn bản này; chỗ nào thực tế khác văn bản thì ghi chú rõ.

**Các yếu tố phải thể hiện trên sơ đồ:**
- **Mốc bắt đầu tính thời hạn**: từ lúc nhận hồ sơ hợp lệ, không phải từ lúc nhận hồ sơ.
  Đây là chi tiết quyết định cách hệ thống đếm ngày.
- **Dừng và nối lại đếm thời hạn** khi trả hồ sơ bổ sung.
- **Ngày làm việc** chứ không phải ngày lịch, có tính ngày nghỉ lễ.
- **Liên thông**: hồ sơ đi qua nhiều cơ quan, mỗi cơ quan có thời hạn riêng nhưng tổng thời
  hạn bị ràng buộc.
- **Kênh nộp song song**: trực tiếp tại bộ phận một cửa, trực tuyến, qua bưu chính công ích.
  Ba kênh cùng chảy vào một quy trình xử lý, không được tách thành ba quy trình riêng.
- **Trả kết quả** theo nhiều hình thức, gồm bản điện tử.

**Sau sắp xếp chính quyền 2 cấp**, một việc bắt buộc khi vẽ to-be: xác định lại thẩm quyền
giải quyết. Nhiều thủ tục vốn ở cấp huyện đã chuyển xuống cấp xã hoặc lên cấp tỉnh, và việc
tiếp nhận tại Trung tâm Phục vụ hành chính công không còn phụ thuộc địa giới hành chính
trong phạm vi cấp tỉnh — nghĩa là người dân nộp ở một nơi nhưng hồ sơ được giải quyết ở nơi
khác. Sơ đồ phải thể hiện bước định tuyến này. Xem `public-services-domain.md`.

## Đặc thù quy trình ngân hàng

- **Maker–checker**: hầu như mọi thao tác ảnh hưởng tiền hoặc quyền đều có bước duyệt riêng.
  Vẽ rõ người tạo và người duyệt ở hai lane khác nhau.
- **Ngưỡng theo giá trị**: số tiền quyết định số cấp duyệt. Thể hiện bằng cổng loại trừ với
  điều kiện ghi rõ tham số, không ghi số cứng trên sơ đồ.
- **Điểm không quay lại**: sau bước nào thì giao dịch không hủy được nữa. Đánh dấu rõ, vì
  đây là nguồn gốc của yêu cầu về quy trình tra soát.
- **Xử lý ngoài giờ và cuối ngày**: giao dịch sau cut-off đi nhánh khác.
- **Luồng bù trừ**: khi giao dịch thất bại giữa chừng, phải có nhánh hoàn tiền. Sơ đồ chỉ có
  nhánh thành công là sơ đồ chưa xong.

## Lỗi hay gặp khi vẽ

- **Cổng không ghi điều kiện trên nhánh** → người đọc tự đoán.
- **Quy trình chỉ có một điểm kết thúc** → thực tế luôn có nhiều cách kết thúc: hoàn thành,
  từ chối, rút hồ sơ, hết hạn.
- **Trộn as-is và to-be trong một sơ đồ** → không ai biết đang nhìn cái gì.
- **Vẽ cả màn hình vào sơ đồ quy trình** → sơ đồ quy trình nói về nghiệp vụ, không phải giao
  diện. Giao diện có tài liệu riêng.
- **Bỏ qua vai trò hệ thống** → các bước tự động cũng là bước, cũng có thể lỗi, cũng cần
  đặc tả.
- **Không thể hiện thời gian** → quy trình không có thời hạn trên sơ đồ thì không phân tích
  được điểm nghẽn.
- **Sơ đồ đẹp nhưng không ai xác nhận** → luôn đưa cho người làm thật xem và sửa trực tiếp.
