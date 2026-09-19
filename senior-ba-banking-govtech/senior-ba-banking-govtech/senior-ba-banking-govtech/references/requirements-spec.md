# Viết đặc tả yêu cầu

Mục lục: [Chọn dạng tài liệu](#chọn-dạng-tài-liệu) · [BRD](#mẫu-brd) · [User story](#user-story-và-tiêu-chí-chấp-nhận) · [Use case](#use-case) · [Quy tắc nghiệp vụ](#quy-tắc-nghiệp-vụ) · [Phi chức năng](#yêu-cầu-phi-chức-năng) · [Chất lượng](#kiểm-tra-chất-lượng-yêu-cầu)

## Chọn dạng tài liệu

| Bối cảnh | Dạng phù hợp |
|---|---|
| Dự án thầu, có nghiệm thu theo hợp đồng | BRD/SRS đầy đủ, ký duyệt từng phần |
| Đội agile nội bộ, phát triển liên tục | User story + tiêu chí chấp nhận + tài liệu miền dùng chung |
| Quy trình nhiều nhánh, nhiều vai trò | Use case + BPMN |
| Logic điều kiện phức tạp (tính phí, xét duyệt) | Bảng quyết định + danh sách quy tắc nghiệp vụ |
| Tích hợp hệ thống | Đặc tả giao diện riêng — xem `integration-spec.md` |

Đa số dự án ngân hàng và dịch vụ công ở Việt Nam vẫn cần tài liệu ký duyệt được vì lý do hợp
đồng và kiểm toán, kể cả khi đội làm theo agile. Cách dung hòa thực tế: **một tài liệu miền
sống** (quy tắc nghiệp vụ, mô hình dữ liệu, quy trình) được cập nhật liên tục, cộng với
**user story** cho từng đợt phát triển, và **bản chốt theo mốc** để ký nghiệm thu.

## Mẫu BRD

```markdown
# BRD — [Tên dự án] — v[x.y] — [ngày]

## 1. Bối cảnh và vấn đề
Vấn đề đang gặp, số liệu chứng minh, hậu quả nếu không giải quyết.

## 2. Mục tiêu và chỉ số đo
| Mục tiêu | Chỉ số | Hiện tại | Mục tiêu | Đo bằng cách nào |
|---|---|---|---|---|
| Giảm thời gian xử lý hồ sơ | Thời gian trung bình | 5 ngày | 2 ngày | Báo cáo hệ thống một cửa |

## 3. Phạm vi
Trong phạm vi / Ngoài phạm vi (nêu rõ, kèm lý do)

## 4. Bên liên quan
Vai trò — quan tâm điều gì — mức ảnh hưởng — ai ký duyệt phần nào

## 5. Cơ sở pháp lý và ràng buộc
| Văn bản | Điều khoản | Ràng buộc cụ thể | Ảnh hưởng tới thiết kế |
|---|---|---|---|

## 6. Quy trình nghiệp vụ
As-is (kèm điểm đau) → To-be (kèm điểm thay đổi) — xem `process-modeling.md`

## 7. Yêu cầu chức năng
Theo nhóm nghiệp vụ, mỗi yêu cầu có mã, mô tả, mức ưu tiên, nguồn gốc

## 8. Quy tắc nghiệp vụ
Bảng riêng, đánh mã BR-xxx

## 9. Yêu cầu dữ liệu
Thực thể chính, từ điển dữ liệu, nguồn dữ liệu, di trú

## 10. Yêu cầu tích hợp
Danh sách hệ thống đối tác, chiều dữ liệu, tần suất

## 11. Yêu cầu phi chức năng
Hiệu năng, bảo mật, khả dụng, khả năng tiếp cận, lưu trữ

## 12. Giả định, phụ thuộc, rủi ro
## 13. Phụ lục: thuật ngữ, biểu mẫu, ảnh màn hình
```

Phần 5 là phần phân biệt BRD dùng được với BRD hình thức ở hai miền này. Đừng viết chung
chung "tuân thủ quy định hiện hành" — hãy liệt kê đúng điều khoản và dịch nó thành ràng buộc
thiết kế cụ thể.

## User story và tiêu chí chấp nhận

```
Là [vai trò cụ thể]
Tôi muốn [hành động]
Để [giá trị đạt được]
```

Vai trò phải cụ thể. "Là người dùng" là vô nghĩa — "Là cán bộ tiếp nhận tại Trung tâm Phục
vụ hành chính công cấp xã" mới cho biết bối cảnh, quyền hạn và ràng buộc.

**Ví dụ tốt (dịch vụ công):**
```
Là người dân nộp hồ sơ trực tuyến
Tôi muốn hệ thống tự điền thông tin cá nhân từ tài khoản định danh điện tử
Để không phải gõ lại và không bị sai lệch so với dữ liệu gốc

Tiêu chí chấp nhận:
- Khi đăng nhập bằng tài khoản định danh điện tử, các trường họ tên, ngày sinh, số định
  danh, nơi thường trú được điền sẵn và ở trạng thái không sửa được
- Trường được điền sẵn hiển thị dấu hiệu cho biết nguồn dữ liệu
- Nếu người dân phát hiện sai, có đường dẫn hướng dẫn thủ tục điều chỉnh dữ liệu gốc,
  hệ thống KHÔNG cho sửa trực tiếp trên tờ khai
- Nếu không lấy được dữ liệu (lỗi kết nối), hiển thị thông báo và cho phép nhập tay,
  đồng thời đánh dấu hồ sơ cần đối chiếu thủ công
- Thời gian lấy dữ liệu không quá 3 giây; quá thì chuyển sang nhập tay
```

**Ví dụ tốt (ngân hàng):**
```
Là khách hàng cá nhân dùng Mobile Banking
Tôi muốn được cảnh báo trước khi giao dịch vượt ngưỡng phải xác thực sinh trắc học
Để không bị gián đoạn giữa chừng khi đang vội

Tiêu chí chấp nhận:
- Khi số tiền nhập vào vượt ngưỡng quy định, màn hình nhập số tiền hiển thị ngay dòng
  thông báo sẽ cần xác thực sinh trắc học ở bước sau
- Khi tổng giao dịch trong ngày sắp chạm ngưỡng, hiển thị số tiền còn lại trước khi cần
  xác thực bổ sung
- Ngưỡng lấy từ cấu hình, không cố định trong mã nguồn (BR-014)
- Thông báo không tiết lộ tổng số tiền đã giao dịch nếu phiên đăng nhập chưa xác thực đủ mức
```

**Nguyên tắc viết tiêu chí chấp nhận:**
- Mỗi tiêu chí kiểm được bằng có/không, không cần diễn giải.
- Phủ cả luồng ngoại lệ, không chỉ luồng thành công.
- Nêu rõ điều **không được phép xảy ra**, không chỉ điều phải xảy ra.
- Tham chiếu tới quy tắc nghiệp vụ bằng mã thay vì lặp lại nội dung — khi quy tắc đổi chỉ
  phải sửa một chỗ.

**INVEST** để kiểm tra story: Độc lập, Thương lượng được, Có giá trị, Ước lượng được, Nhỏ
gọn, Kiểm thử được. Story không thỏa "có giá trị" thường là một công việc kỹ thuật bị viết
nhầm thành story.

## Use case

Dùng khi quy trình có nhiều nhánh và nhiều vai trò — phổ biến ở dịch vụ công.

```
UC-012  Tiếp nhận hồ sơ trực tuyến
Tác nhân chính:   Người dân
Tác nhân phụ:     Hệ thống định danh, cơ sở dữ liệu chuyên ngành, cán bộ tiếp nhận
Tiền điều kiện:   Người dân đã đăng nhập; thủ tục còn hiệu lực
Hậu điều kiện:    Hồ sơ ở trạng thái Chờ tiếp nhận, có mã hồ sơ, biên nhận đã gửi

Luồng chính:
1. Người dân chọn thủ tục
2. Hệ thống hiển thị biểu mẫu, điền sẵn dữ liệu từ tài khoản định danh
3. Người dân điền phần còn lại và tải lên thành phần hồ sơ
4. Hệ thống kiểm tra tính đầy đủ và định dạng tệp
5. Người dân ký số hoặc xác nhận theo hình thức được chấp nhận
6. Hệ thống sinh mã hồ sơ, gửi biên nhận điện tử, chuyển hồ sơ tới cơ quan có thẩm quyền

Luồng ngoại lệ:
4a. Thiếu thành phần hồ sơ → chỉ rõ thiếu gì, lưu nháp, không mất dữ liệu đã nhập
4b. Tệp vượt dung lượng hoặc sai định dạng → nêu giới hạn cụ thể, gợi ý cách xử lý
2a. Không kết nối được hệ thống định danh → cho nhập tay, đánh dấu cần đối chiếu
6a. Không xác định được cơ quan thẩm quyền (sau sắp xếp đơn vị hành chính) →
    chuyển về bộ phận điều phối, không để hồ sơ treo
5a. Người dân không có chữ ký số → áp dụng hình thức xác nhận thay thế theo quy định
```

Luồng ngoại lệ 6a là ví dụ điển hình của thứ chỉ BA hiểu bối cảnh mới nghĩ ra, và là chỗ hệ
thống hay hỏng trong giai đoạn chuyển đổi tổ chức.

## Quy tắc nghiệp vụ

Tách riêng khỏi mô tả chức năng. Lý do: một quy tắc được dùng ở nhiều chức năng, và quy tắc
thay đổi độc lập với giao diện.

```
BR-014  Ngưỡng xác thực sinh trắc học
Phát biểu:  Giao dịch chuyển tiền của khách hàng cá nhân phải xác thực sinh trắc học khi
            thỏa mãn ít nhất một điều kiện: (a) giá trị giao dịch vượt ngưỡng đơn lẻ;
            (b) tổng giá trị trong ngày vượt ngưỡng lũy kế; (c) thực hiện trên thiết bị
            khác thiết bị gần nhất.
Nguồn:      Thông tư 50/2024/TT-NHNN — [ghi điều khoản cụ thể]
Tham số:    NGUONG_DON_LE, NGUONG_LUY_KE_NGAY — cấu hình được, không hard-code
Áp dụng ở:  UC-021, UC-022, UC-030
Ngoại lệ:   [nếu có, ghi rõ]
Ngày hiệu lực / phiên bản: ...
```

Với logic nhiều điều kiện, dùng bảng quyết định thay vì văn xuôi — văn xuôi luôn để sót tổ
hợp:

| Loại khách | Hồ sơ đầy đủ | Thuộc diện ưu tiên | → Thời hạn giải quyết |
|---|---|---|---|
| Cá nhân | Có | Không | 5 ngày làm việc |
| Cá nhân | Có | Có | 2 ngày làm việc |
| Cá nhân | Không | — | Trả bổ sung trong 1 ngày, dừng đếm thời hạn |
| Tổ chức | Có | Không | 10 ngày làm việc |

## Yêu cầu phi chức năng

Phần bị viết qua loa nhiều nhất, và là phần gây tranh cãi nhất lúc nghiệm thu. Mỗi yêu cầu
phải có con số và cách đo.

| Nhóm | Ví dụ viết đúng |
|---|---|
| Hiệu năng | 95% yêu cầu tra cứu trả kết quả dưới 2 giây ở mức 200 người dùng đồng thời |
| Chịu tải | Hệ thống phục vụ 5.000 hồ sơ/ngày, đỉnh 800 hồ sơ/giờ vào ngày đầu tháng |
| Khả dụng | 99,5% theo tháng, không tính cửa sổ bảo trì đã công bố trước 3 ngày |
| Bảo mật | Dữ liệu cá nhân mã hóa khi lưu và khi truyền; nhật ký truy cập lưu tối thiểu [n] tháng |
| Khả năng tiếp cận | Đạt WCAG 2.1 mức AA cho các màn hình dành cho người dân |
| Tương thích | Hoạt động trên trình duyệt phổ biến 2 phiên bản gần nhất; màn hình từ 360px |
| Lưu trữ | Hồ sơ điện tử lưu theo quy định lưu trữ hiện hành, tra cứu được sau khi đóng hồ sơ |
| Khôi phục | RTO 4 giờ, RPO 15 phút |

Với dịch vụ công, khả năng tiếp cận và tương thích thiết bị cũ không phải chuyện phụ: người
dùng gồm cả người cao tuổi và người dùng điện thoại cấu hình thấp. Một hệ thống chỉ chạy
mượt trên máy mới là hệ thống loại trừ một phần dân số.

## Kiểm tra chất lượng yêu cầu

Rà mỗi yêu cầu qua danh sách này trước khi gửi đi rà soát:

- [ ] **Rõ ràng**: chỉ hiểu được theo một cách. Quét các từ nguy hiểm: "tương ứng", "phù
      hợp", "nếu cần", "theo quy định", "nhanh chóng", "thân thiện", "v.v.", "linh hoạt".
- [ ] **Kiểm chứng được**: nêu được cách chứng minh đã đáp ứng.
- [ ] **Nguyên tử**: một yêu cầu một việc. Có chữ "và" nối hai hành vi thì tách ra.
- [ ] **Nhất quán**: không mâu thuẫn với yêu cầu khác trong cùng tài liệu.
- [ ] **Truy vết được**: gắn với nhu cầu nghiệp vụ hoặc điều khoản pháp quy.
- [ ] **Khả thi**: đội kỹ thuật đã xem và nói làm được.
- [ ] **Không áp đặt giải pháp** trừ khi đó là ràng buộc thật.

Mẹo thực dụng: đọc to yêu cầu và tự hỏi "hai người đọc câu này có thể làm ra hai hệ thống
khác nhau không?". Nếu có, viết lại.
