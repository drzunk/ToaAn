# Đặc tả tích hợp hệ thống

Mục lục: [Trước khi viết](#trước-khi-viết-đặc-tả) · [Mẫu đặc tả](#mẫu-đặc-tả-giao-diện) · [Mã lỗi](#thiết-kế-mã-lỗi) · [Bất đồng bộ](#đồng-bộ-hay-bất-đồng-bộ) · [Idempotency](#chống-trùng-và-tra-soát) · [Phi chức năng](#thỏa-thuận-vận-hành) · [Câu hỏi với đối tác](#câu-hỏi-bắt-buộc-hỏi-đối-tác)

## Trước khi viết đặc tả

Phần lớn rủi ro dự án nằm ở chỗ tích hợp, không phải ở màn hình. Trả lời trước sáu câu:

1. **Hệ thống nào là nguồn đúng của dữ liệu này?** Hai hệ thống cùng cho phép sửa một dữ
   liệu là công thức của xung đột.
2. **Chiều dữ liệu**: một chiều hay hai chiều? Ai gọi ai?
3. **Thời điểm**: theo thời gian thực, theo lô định kỳ, hay theo sự kiện?
4. **Ai sở hữu giao diện?** Ta định nghĩa và đối tác tuân theo, hay ngược lại? Điều này quyết
   định ai chịu chi phí khi có thay đổi.
5. **Chuyện gì xảy ra khi bên kia không phản hồi?** Câu hỏi quan trọng nhất và hay bị bỏ qua.
6. **Ai vận hành và liên hệ khi sự cố?** Có kênh hỗ trợ và cam kết phản hồi không?

Ở dịch vụ công, thêm một câu: **kết nối qua nền tảng tích hợp chia sẻ dữ liệu hay kết nối
trực tiếp?** Việc này ảnh hưởng tới thủ tục xin kết nối, thời gian chờ, và cách xử lý lỗi.
Xem `govtech-integration.md`.

## Mẫu đặc tả giao diện

```markdown
# IF-005 — Tra cứu thông tin cư trú từ cơ sở dữ liệu quốc gia về dân cư

## Tổng quan
Mục đích:        Xác minh nơi thường trú của người nộp hồ sơ, thay cho việc yêu cầu
                 nộp giấy tờ chứng minh
Hệ thống gọi:    Hệ thống một cửa
Hệ thống nhận:   Dịch vụ tra cứu dân cư (qua nền tảng tích hợp)
Kiểu:            Đồng bộ, theo yêu cầu
Tần suất ước tính: ~2.000 lượt/ngày, đỉnh 400 lượt/giờ
Mức nhạy cảm:    Dữ liệu cá nhân — phải có cơ sở pháp lý và ghi nhật ký truy cập

## Điều kiện gọi
- Người dân đã đồng ý cho tra cứu (ghi nhận sự đồng ý kèm thời điểm)
- Có số định danh cá nhân hợp lệ

## Dữ liệu gửi đi
| Trường | Kiểu | Bắt buộc | Mô tả | Ví dụ |
|---|---|---|---|---|
| soDinhDanh | chuỗi(12) | Có | Số định danh cá nhân | "0010xxxxxxxx" |
| hoTen | chuỗi(255) | Có | Dùng để đối chiếu | "Nguyễn Văn A" |
| maYeuCau | chuỗi(36) | Có | Định danh yêu cầu, duy nhất | UUID |
| maThuTuc | chuỗi(20) | Có | Cơ sở pháp lý của việc tra cứu | "1.001234" |

## Dữ liệu nhận về
| Trường | Kiểu | Mô tả | Xử lý khi thiếu |
|---|---|---|---|
| ketQua | chuỗi | Mã kết quả | Bắt buộc có |
| hoTen | chuỗi | Họ tên theo dữ liệu gốc | Nếu thiếu → coi như không tra được |
| noiThuongTru | đối tượng | Mã tỉnh, mã xã, địa chỉ chi tiết | Nếu thiếu mã xã → đánh dấu cần chuẩn hóa |
| thoiDiemCapNhat | ngày giờ | Dữ liệu gốc cập nhật lần cuối | Hiển thị cho cán bộ biết độ mới |

## Quy tắc đối chiếu
- Họ tên so khớp không phân biệt hoa thường và dấu → nếu khác, KHÔNG tự động từ chối mà
  chuyển cán bộ xem xét (người Việt hay có sai lệch dấu giữa các hệ thống)
- Nếu số định danh không tồn tại → thông báo cho người dân kiểm tra lại, không khẳng định
  người dân khai sai

## Xử lý lỗi
| Tình huống | Hành vi hệ thống | Thông báo cho người dùng |
|---|---|---|
| Hết thời gian chờ (>5s) | Chuyển sang nhập tay, đánh dấu hồ sơ cần đối chiếu | "Chưa lấy được dữ liệu, bạn vui lòng nhập và đính kèm giấy tờ" |
| Dịch vụ trả lỗi hệ thống | Như trên, ghi nhật ký kỹ thuật | Như trên |
| Không tìm thấy dữ liệu | Cho nhập tay, đánh dấu | "Không tìm thấy thông tin, vui lòng kiểm tra lại số định danh" |
| Dữ liệu trả về không khớp | Chuyển cán bộ xem xét, không chặn nộp hồ sơ | "Hồ sơ sẽ được cán bộ đối chiếu thêm" |

## Nhật ký và lưu vết
Ghi lại: ai tra cứu, tra cứu ai, lúc nào, vì thủ tục nào, kết quả gì. Lưu tối thiểu [n] tháng.

## Thỏa thuận vận hành
Thời gian phản hồi mong đợi: p95 < 2s  |  Thời gian chờ tối đa: 5s
Giới hạn tần suất: [x] lượt/giây  |  Khả dụng cam kết: [x]%
Cơ chế thử lại: tối đa 2 lần, giãn cách tăng dần, KHÔNG thử lại khi lỗi là "không tìm thấy"
```

Phần **Xử lý lỗi** và **Thỏa thuận vận hành** là hai phần phân biệt đặc tả dùng được với đặc
tả chỉ liệt kê trường dữ liệu. Không có hai phần này, đội phát triển sẽ tự quyết và mỗi
giao diện xử lý lỗi một kiểu.

## Thiết kế mã lỗi

Nguyên tắc: **mã dành cho hệ thống, thông báo dành cho người.** Đừng để đội phát triển phải
đọc chuỗi tiếng Việt để quyết định luồng xử lý.

| Mã | Ý nghĩa kỹ thuật | Hành vi hệ thống | Thông báo cho người dùng |
|---|---|---|---|
| 0000 | Thành công | Tiếp tục | — |
| 1001 | Dữ liệu đầu vào không hợp lệ | Không thử lại, trả về sửa | Nêu rõ trường nào sai |
| 2001 | Không tìm thấy | Không thử lại | "Không tìm thấy thông tin…" |
| 3001 | Vượt hạn mức / vi phạm quy tắc nghiệp vụ | Không thử lại | Giải thích quy tắc |
| 5001 | Lỗi hệ thống bên nhận | Thử lại có giãn cách | "Hệ thống bận, vui lòng thử lại" |
| 5002 | Hết thời gian chờ, **chưa rõ kết quả** | **Không thử lại mù**, phải tra soát | "Đang xử lý, chúng tôi sẽ thông báo" |

Phân biệt 5001 và 5002 là điều quan trọng nhất trong bảng này, đặc biệt với giao dịch tiền:
lỗi rõ ràng thì thử lại an toàn; timeout thì **không biết bên kia đã xử lý hay chưa**, thử
lại mù có thể tạo giao dịch trùng.

**Yêu cầu với bảng mã lỗi:** đầy đủ (mọi mã đối tác có thể trả về đều có dòng), có mã mặc
định cho trường hợp lạ, và mỗi mã có đúng một hành vi hệ thống.

## Đồng bộ hay bất đồng bộ

| Tiêu chí | Đồng bộ | Bất đồng bộ |
|---|---|---|
| Người dùng cần kết quả ngay | ✓ | ✗ |
| Xử lý lâu (> vài giây) | ✗ | ✓ |
| Bên nhận có thể bận/chậm | ✗ | ✓ |
| Cần đảm bảo không mất yêu cầu | ✗ | ✓ (qua hàng đợi) |
| Đơn giản để triển khai | ✓ | ✗ |

Với bất đồng bộ, đặc tả phải nêu thêm: cơ chế thông báo kết quả (gọi ngược, người dùng tự
tra cứu, hay thông báo đẩy), thời gian tối đa chấp nhận được, và cách xử lý khi quá hạn mà
không có kết quả.

Ở dịch vụ công, mô hình phổ biến là: nộp hồ sơ đồng bộ (người dân cần mã hồ sơ ngay), xử lý
bất đồng bộ (mất nhiều ngày), thông báo kết quả qua nhiều kênh.

## Chống trùng và tra soát

Mọi giao diện thay đổi dữ liệu hoặc tiền đều cần:

- **Mã yêu cầu duy nhất** do bên gọi sinh, bên nhận lưu lại. Gửi lại cùng mã phải trả về
  cùng kết quả, không tạo bản ghi mới.
- **Quy tắc thử lại rõ ràng**: mã lỗi nào được thử lại, tối đa mấy lần, giãn cách bao nhiêu.
- **Cơ chế tra soát**: khi kết quả không rõ, có giao diện truy vấn trạng thái theo mã yêu cầu.
  Thiếu giao diện này thì mọi timeout đều phải xử lý thủ công.
- **Đối soát định kỳ**: cuối ngày hai bên đối chiếu danh sách giao dịch. Đặc tả nêu rõ file
  hoặc giao diện đối soát, thời điểm, và cách xử lý chênh lệch.

Ba thứ này hay bị coi là "chi tiết kỹ thuật" và bỏ ra khỏi tài liệu BA. Thực tế chúng là
**yêu cầu nghiệp vụ**: chúng quyết định điều gì xảy ra với hồ sơ hoặc tiền của người dân khi
hệ thống trục trặc.

## Câu hỏi bắt buộc hỏi đối tác

Trước khi chốt đặc tả với bên thứ ba, hỏi đủ:

**Kỹ thuật**
- Tài liệu giao diện phiên bản nào, cập nhật lần cuối khi nào?
- Có môi trường thử nghiệm không? Cấp tài khoản thế nào, mất bao lâu?
- Xác thực bằng cơ chế gì? Khóa/chứng thư hết hạn bao lâu, quy trình gia hạn?
- Giới hạn tần suất là bao nhiêu? Vượt thì bị gì?
- Danh sách đầy đủ mã lỗi có thể trả về?

**Vận hành**
- Cam kết về thời gian phản hồi và khả dụng?
- Lịch bảo trì, báo trước bao lâu?
- Kênh hỗ trợ sự cố, thời gian phản hồi cam kết?
- Khi đổi phiên bản giao diện, báo trước bao lâu, có chạy song song không?

**Pháp lý và dữ liệu**
- Cơ sở pháp lý để trao đổi dữ liệu này là gì? Cần ký thỏa thuận gì?
- Dữ liệu được phép dùng vào việc gì, lưu bao lâu?
- Yêu cầu về ghi nhật ký truy cập?

Câu hỏi về môi trường thử nghiệm nên hỏi **sớm nhất có thể**. Thời gian chờ cấp quyền kết
nối tới các hệ thống dùng chung thường tính bằng tuần và là nguyên nhân chậm tiến độ hay
gặp nhất mà lẽ ra dự đoán được từ đầu.
