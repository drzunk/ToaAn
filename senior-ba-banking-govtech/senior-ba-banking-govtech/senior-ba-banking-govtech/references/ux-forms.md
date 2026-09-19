# Biểu mẫu điện tử và trải nghiệm người dùng

Mục lục: [Vai trò của BA](#vai-trò-của-ba-với-ux) · [Chân dung người dùng](#chân-dung-người-dùng-thực-tế) · [Thiết kế biểu mẫu](#thiết-kế-biểu-mẫu) · [Thông báo lỗi](#thông-báo-lỗi-và-hướng-dẫn) · [Hành trình](#bản-đồ-hành-trình) · [Khả năng tiếp cận](#khả-năng-tiếp-cận) · [Đo lường](#đo-lường-hiệu-quả)

## Vai trò của BA với UX

BA không thay thế người thiết kế, nhưng BA quyết định những thứ mà thiết kế không cứu được:
**hỏi cái gì, hỏi bao nhiêu, và hỏi khi nào.** Một biểu mẫu 40 trường sẽ khó dùng dù giao
diện đẹp đến đâu.

Câu hỏi BA phải đặt cho mỗi trường dữ liệu trên biểu mẫu:
1. Nghiệp vụ có thực sự cần trường này không? Ai dùng nó, để làm gì?
2. Có lấy được từ nguồn khác thay vì bắt người dùng nhập không?
3. Nếu người dùng không biết câu trả lời thì sao?
4. Trường này có ở bước đúng không, hay nên hỏi muộn hơn?

Kinh nghiệm thực tế: rà soát một biểu mẫu hành chính điển hình theo bốn câu này thường bỏ
được 30–50% số trường. Đây là cải tiến lớn nhất mà BA tạo ra được, và nó không tốn chi phí
thiết kế.

## Chân dung người dùng thực tế

Sai lầm phổ biến: thiết kế cho người dùng giống chính mình. Người dùng thật của hệ thống
ngân hàng và dịch vụ công ở Việt Nam rất đa dạng.

**Cần khảo sát và ghi vào tài liệu:**
- Độ tuổi và mức độ quen công nghệ
- Thiết bị: điện thoại cấu hình thấp chiếm bao nhiêu, màn hình nhỏ nhất cần hỗ trợ
- Kết nối: 3G chập chờn, dữ liệu di động có hạn
- Hoàn cảnh sử dụng: đang đứng ở quầy, đang vội, đang được người khác hỗ trợ làm hộ
- Tần suất: dùng hằng ngày (cán bộ) hay vài năm một lần (người dân làm thủ tục)

**Hệ quả thiết kế từ chân dung:**
- Người dùng tần suất thấp cần hướng dẫn trong luồng, không phải tài liệu riêng. Họ sẽ không
  đọc hướng dẫn trước khi làm.
- Người dùng tần suất cao (cán bộ) cần phím tắt và thao tác nhanh, ghét màn hình xác nhận thừa.
- Người làm hộ (con cái làm giúp cha mẹ) là kịch bản thật và phổ biến — luồng ủy quyền nên
  được xem xét thay vì giả vờ không tồn tại.
- Kết nối kém → lưu nháp tự động là yêu cầu bắt buộc, không phải tính năng hay có.

## Thiết kế biểu mẫu

**Nguyên tắc giảm tải cho người dùng:**

| Vấn đề | Cách xử lý |
|---|---|
| Quá nhiều trường | Bỏ trường không cần; tách thành nhiều bước; ẩn trường chỉ hiện khi liên quan |
| Người dùng phải tra cứu thông tin | Điền sẵn từ nguồn dữ liệu; gợi ý khi gõ; chọn từ danh sách thay vì gõ tay |
| Không biết còn bao xa mới xong | Hiện tiến độ theo bước, ước lượng thời gian còn lại |
| Sợ mất dữ liệu đã nhập | Lưu nháp tự động, thông báo rõ đã lưu; cho quay lại sau |
| Không hiểu trường này hỏi gì | Nhãn dùng từ của người dân, không dùng từ chuyên môn; có ví dụ; có giải thích ngắn tại chỗ |
| Tải tệp khó khăn | Nêu rõ định dạng và dung lượng **trước** khi chọn tệp; cho chụp ảnh trực tiếp; tự nén ảnh |

**Về ngôn ngữ trên biểu mẫu:** dùng từ người dân dùng. "Nơi thường trú" trong văn bản có thể
cần chú thích. Từ như "chủ thể dữ liệu", "đối tượng thụ hưởng", "tổ chức đề nghị" cần được
diễn đạt lại hoặc có giải thích. Kiểm tra bằng cách đưa cho một người ngoài ngành đọc và hỏi
họ hiểu gì.

**Về biểu mẫu do văn bản quy định:** nhiều biểu mẫu hành chính có mẫu cố định theo văn bản.
BA cần phân biệt: **nội dung bắt buộc** (không đổi được) và **cách trình bày trên màn hình**
(đổi được). Một tờ khai in ra phải đúng mẫu không có nghĩa là màn hình nhập liệu phải trông
giống tờ giấy đó. Đây là điểm BA hay nhượng bộ không cần thiết.

**Xác thực dữ liệu nhập:**
- Kiểm tra ngay khi người dùng rời khỏi trường, không đợi đến lúc bấm gửi
- Thông báo lỗi đặt cạnh trường lỗi, không gom lên đầu trang
- Không xóa dữ liệu đã nhập khi có lỗi
- Chấp nhận nhiều cách nhập cho cùng một thứ (số điện thoại có hoặc không có dấu cách, ngày
  tháng nhiều định dạng) rồi chuẩn hóa ở phía hệ thống — đừng bắt người dùng học định dạng

## Thông báo lỗi và hướng dẫn

Thông báo lỗi là nơi lộ rõ nhất hệ thống được thiết kế cho ai.

**Công thức:** điều gì xảy ra + vì sao + làm gì tiếp theo.

| Kém | Tốt |
|---|---|
| "Lỗi hệ thống" | "Chưa lấy được thông tin từ cơ sở dữ liệu dân cư. Bạn vui lòng nhập tay và đính kèm ảnh giấy tờ, cán bộ sẽ đối chiếu." |
| "Dữ liệu không hợp lệ" | "Số định danh cần đủ 12 chữ số. Bạn đang nhập 11 số." |
| "Giao dịch thất bại" | "Giao dịch chưa thực hiện được vì số dư khả dụng không đủ. Số dư khả dụng hiện tại là X, giao dịch cần Y (gồm phí Z)." |
| "Hồ sơ bị từ chối" | "Hồ sơ chưa được tiếp nhận vì thiếu [tên giấy tờ]. Bạn bổ sung tại đây, hồ sơ đã nhập được giữ nguyên." |

**Với trạng thái không chắc chắn** (giao dịch timeout, hồ sơ đang chờ cơ quan khác), thông
báo phải trung thực: không nói thành công khi chưa chắc, không nói thất bại khi chưa chắc.
"Đang xử lý, chúng tôi sẽ thông báo cho bạn trong vòng X giờ" là cách nói đúng — và kèm theo
nó là yêu cầu hệ thống thật sự phải thông báo.

**Không tiết lộ thông tin nhạy cảm qua thông báo lỗi.** "Tài khoản không tồn tại" và "Sai mật
khẩu" phải cho cùng một thông báo, nếu không kẻ xấu dò được tài khoản nào có thật.

## Bản đồ hành trình

Vẽ hành trình đầy đủ, không chỉ phần trên màn hình. Hành trình thật của một người dân làm
thủ tục thường bắt đầu từ trước khi mở trình duyệt và kết thúc sau khi nhận kết quả.

```
Biết mình cần làm thủ tục → Tìm hiểu cần gì → Chuẩn bị giấy tờ → Nộp hồ sơ →
Chờ và theo dõi → (Bổ sung nếu bị yêu cầu) → Nhận kết quả → Dùng kết quả cho việc khác
```

Với mỗi chặng, ghi: người dùng làm gì, nghĩ gì, cảm thấy thế nào, chạm vào kênh nào, và
điểm đau ở đâu.

**Các điểm đau hay gặp mà hệ thống có thể giải quyết:**
- Không biết cần chuẩn bị gì → danh sách kiểm tra trước khi bắt đầu, xem được mà không cần
  đăng nhập
- Chuẩn bị thiếu, phải làm lại từ đầu → lưu nháp, cho bổ sung sau
- Nộp xong không biết gì đang xảy ra → thông báo chủ động theo từng mốc, không bắt người dân
  tự vào tra
- Bị yêu cầu bổ sung nhưng không hiểu thiếu gì → nêu cụ thể thiếu giấy tờ nào, vì sao
- Không biết bao giờ xong → hiện ngày hẹn trả cụ thể, cập nhật khi thay đổi

Điểm đau "nộp xong không biết gì đang xảy ra" là điểm gây khiếu nại nhiều nhất và chi phí
giải quyết thấp nhất. Nên ưu tiên.

## Khả năng tiếp cận

Với hệ thống phục vụ người dân, đây không phải tính năng cao cấp mà là điều kiện để hệ thống
không loại trừ một phần dân số.

**Yêu cầu tối thiểu nên đưa vào đặc tả:**
- Đạt WCAG 2.1 mức AA cho các màn hình dành cho người dân
- Độ tương phản đủ; không dùng riêng màu sắc để truyền đạt thông tin (người mù màu)
- Cỡ chữ đọc được và phóng to được tới 200% mà không vỡ bố cục
- Thao tác được hoàn toàn bằng bàn phím
- Mọi hình ảnh truyền tải thông tin có mô tả thay thế
- Nhãn trường gắn đúng với ô nhập để trình đọc màn hình đọc được
- Không giới hạn thời gian thao tác quá ngắn, hoặc cho gia hạn
- Hoạt động trên màn hình nhỏ từ 360px và trên trình duyệt phổ biến hai phiên bản gần nhất

**Kênh thay thế cho người không dùng được thiết bị số** cũng là yêu cầu cần đặc tả: hỗ trợ
tại bộ phận một cửa, qua bưu chính công ích, hoặc người thân làm hộ. Một hệ thống trực tuyến
tốt không xóa bỏ các kênh này mà làm việc ăn khớp với chúng — ví dụ hồ sơ nộp trực tiếp vẫn
được số hóa và theo dõi trên cùng một hệ thống.

## Đo lường hiệu quả

Đặc tả nên kèm cách đo, nếu không sẽ không biết thiết kế có hiệu quả không.

| Chỉ số | Ý nghĩa | Cách lấy |
|---|---|---|
| Tỷ lệ hoàn thành | Bao nhiêu người bắt đầu thì làm xong | Nhật ký hệ thống theo bước |
| Bước bỏ dở nhiều nhất | Chỗ người dùng gặp khó | Phân tích phễu |
| Thời gian điền biểu mẫu | Độ phức tạp thực tế | Ghi thời gian từng bước |
| Tỷ lệ hồ sơ bị yêu cầu bổ sung | Chất lượng hướng dẫn đầu vào | Thống kê nghiệp vụ |
| Tỷ lệ lỗi theo trường | Trường nào gây khó nhất | Ghi nhận lỗi xác thực |
| Tỷ lệ nộp trực tuyến | Hiệu quả chuyển đổi kênh | So sánh theo kênh nộp |
| Số cuộc gọi hỗ trợ theo chủ đề | Chỗ hệ thống chưa tự giải thích được | Tổng đài |

Chỉ số "tỷ lệ hồ sơ bị yêu cầu bổ sung" đặc biệt giá trị ở dịch vụ công: nó đo trực tiếp
chất lượng của phần hướng dẫn và xác thực đầu vào, và giảm được nó là giảm một vòng đi lại
của người dân.
