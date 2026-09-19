# Miền thanh toán

Mục lục: [Các phương thức](#các-phương-thức-thanh-toán-tại-việt-nam) · [Vai trò các bên](#vai-trò-các-bên) · [VietQR](#vietqr) · [Thu hộ chi hộ](#thu-hộ-chi-hộ) · [Đối soát](#đối-soát-và-quyết-toán) · [Giao dịch treo](#giao-dịch-treo-và-tra-soát) · [Đặc tả](#điều-phải-có-trong-mọi-đặc-tả-thanh-toán)

## Các phương thức thanh toán tại Việt Nam

| Phương thức | Đặc điểm | Điểm BA cần làm rõ |
|---|---|---|
| Chuyển khoản nội bộ | Cùng ngân hàng, tức thời | Phí, hạn mức, tài khoản đích không tồn tại |
| Chuyển nhanh liên ngân hàng (NAPAS 247) | 24/7, tức thời, có hạn mức theo quy định | Mã lỗi đối tác, hoàn tiền tự động, đối soát cuối ngày |
| Chuyển liên ngân hàng qua hệ thống của Ngân hàng Nhà nước | Theo lô, trong giờ làm việc, giá trị lớn | Cut-off, trạng thái chờ qua đêm, ngày làm việc |
| Thẻ (POS, cổng thanh toán trực tuyến) | Qua tổ chức thẻ, có cấp phép và quyết toán | Khoảng cách giữa cấp phép và quyết toán, hoàn tiền, tranh chấp |
| Ví điện tử | Liên kết tài khoản/thẻ, hạn mức riêng | Luồng liên kết, hủy liên kết, nạp/rút |
| Mã QR | Chuẩn dùng chung, quét bằng nhiều ứng dụng | QR tĩnh/động, nội dung cố định, hạn dùng |
| Thanh toán trực tuyến trên cổng dịch vụ công | Thu phí, lệ phí, nghĩa vụ tài chính | Đối soát với kho bạc, xác nhận đã thu cho hồ sơ |

Mỗi phương thức có **tập mã lỗi riêng**, **cơ chế hoàn tiền riêng** và **chu kỳ đối soát
riêng**. Đặc tả gộp chung nhiều phương thức vào một luồng là sai lầm hay gặp.

## Vai trò các bên

```
Người trả tiền → Đơn vị chấp nhận → Đơn vị thanh toán/cổng → Tổ chức chuyển mạch →
Ngân hàng phát hành / Ngân hàng thụ hưởng
```

Với mỗi bên, BA cần biết: họ giữ dữ liệu gì, họ quyết định gì, họ báo lỗi kiểu gì, và họ đối
soát với ta theo chu kỳ nào. Sơ đồ luồng tiền và luồng thông tin **không trùng nhau** —
thông tin chạy tức thời, tiền thường chạy theo lô cuối ngày. Đây là nguồn gốc của mọi yêu
cầu về đối soát và tra soát.

## VietQR

Mã QR theo chuẩn dùng chung, chứa thông tin ngân hàng thụ hưởng và số tài khoản, tùy chọn
thêm số tiền và nội dung.

**Phân biệt hai loại:**
- **QR tĩnh**: không có số tiền, người chuyển tự nhập. Dán cố định tại điểm bán, dùng lại
  nhiều lần.
- **QR động**: sinh theo từng giao dịch, có số tiền và thường có mã tham chiếu. Dùng để đối
  soát tự động.

**Điều BA phải đặc tả:**
- Nội dung chuyển khoản dùng để đối chiếu: sinh theo quy tắc nào, dài bao nhiêu ký tự, có ký
  tự đặc biệt không (nhiều hệ thống chỉ chấp nhận chữ và số không dấu)
- QR động có hạn dùng không, hết hạn thì sao
- Xử lý khi người dùng sửa số tiền, sửa nội dung — nghiệp vụ chấp nhận hay từ chối
- Xử lý khi hai người cùng quét một QR động
- Đối chiếu tiền về: dựa vào nội dung chuyển khoản hay dựa vào thông báo biến động số dư?
  Cơ chế nào là nguồn đúng?
- Khi tiền về nhưng không khớp mã tham chiếu → vào hàng chờ xử lý thủ công, không được im lặng

## Thu hộ chi hộ

Mô hình phổ biến khi ngân hàng làm đầu mối thu cho đơn vị khác (điện, nước, học phí, phí dịch
vụ công).

**Luồng chuẩn:**
```
Truy vấn nghĩa vụ → Hiển thị cho khách xác nhận → Thực hiện thanh toán →
Báo có cho đơn vị thụ hưởng → Đơn vị xác nhận đã ghi nhận → Đối soát cuối ngày
```

**Các câu hỏi quyết định thiết kế:**
- Bước truy vấn và bước thanh toán cách nhau bao lâu thì phải truy vấn lại? (Nghĩa vụ có thể
  đã được thanh toán ở kênh khác trong lúc đó.)
- Nếu đơn vị thụ hưởng không xác nhận, tiền đã trừ của khách thì sao? Thời hạn tra soát và
  hoàn tiền tự động là bao nhiêu?
- Thanh toán một phần có được chấp nhận không?
- Thanh toán trùng cùng một nghĩa vụ: chặn, hay cho phép và hoàn sau?
- Phí do ai chịu?
- Chu kỳ chuyển tiền cho đơn vị thụ hưởng: ngay hay cuối ngày? Điều này quyết định rủi ro
  của ngân hàng.

## Đối soát và quyết toán

Đây là phần BA hay bỏ sót vì nó không có giao diện người dùng, nhưng nó là phần khiến dự án
không đóng được.

**Đặc tả đối soát cần nêu:**

| Nội dung | Câu hỏi |
|---|---|
| Đối tượng đối soát | Đối soát với ai, dữ liệu nào so dữ liệu nào |
| Chu kỳ và thời điểm | Cuối ngày lúc mấy giờ, theo mốc cut-off của ai |
| Hình thức | File (định dạng, cách đặt tên, nơi đặt) hay giao diện truy vấn |
| Khóa đối chiếu | Đối chiếu theo trường nào — mã giao dịch của ta hay của đối tác |
| Tiêu chí khớp | Khớp hoàn toàn hay cho phép sai lệch trong ngưỡng |
| Xử lý chênh lệch | Ai xử lý, trong bao lâu, quy trình ra sao |
| Dòng kiểm tra tổng | Tổng số bản ghi và tổng số tiền, dùng để phát hiện file thiếu |

**Các loại chênh lệch và cách xử lý — phải ghi rõ trong đặc tả:**
- Có ở ta, không có ở đối tác → có thể giao dịch chưa tới nơi, cần tra soát
- Có ở đối tác, không có ở ta → nguy hiểm hơn, có thể ta đã bỏ sót ghi nhận
- Khớp giao dịch nhưng lệch số tiền
- Trùng bản ghi trong file đối tác
- File tới muộn hoặc không tới

Nguyên tắc: **hệ thống không bao giờ được tự động xóa hay tự sửa bản ghi lệch.** Mọi chênh
lệch vào hàng chờ có người xử lý và có lưu vết quyết định.

## Giao dịch treo và tra soát

Tình huống nguy hiểm nhất: đã trừ tiền khách, gửi sang đối tác, rồi mất phản hồi.

**Các trạng thái phải có trong mô hình dữ liệu:**
```
Khởi tạo → Đang xử lý → ┬→ Thành công
                        ├→ Thất bại (đã hoàn tiền)
                        └→ Chưa rõ → Đang tra soát → ┬→ Thành công
                                                     └→ Đã hoàn tiền
```

**Đặc tả phải trả lời:**
- Sau bao lâu không có phản hồi thì chuyển sang trạng thái tra soát?
- Ai tra soát — hệ thống tự động gọi lại đối tác, hay con người?
- Sau bao lâu không tra soát được thì tự động hoàn tiền cho khách?
- Nếu đã hoàn tiền rồi đối tác mới báo thành công thì sao? (Không được tự trừ lại — phải đưa
  vào hàng chờ xử lý thủ công và cảnh báo.)
- Khách hàng thấy gì trong lúc giao dịch ở trạng thái chưa rõ? Thông báo phải trung thực:
  không nói "thất bại" khi chưa chắc, cũng không nói "thành công".

Trải nghiệm ở trạng thái chưa rõ là chỗ ngân hàng mất niềm tin của khách nhanh nhất. Đặc tả
nội dung thông báo cho trạng thái này cần được nghiệp vụ và truyền thông cùng duyệt.

## Điều phải có trong mọi đặc tả thanh toán

- [ ] Sơ đồ luồng tiền **và** luồng thông tin, chỉ rõ chúng khác nhau ở đâu
- [ ] Danh sách đầy đủ trạng thái giao dịch và các chuyển đổi hợp lệ
- [ ] Bảng mã lỗi của đối tác, ánh xạ sang hành vi hệ thống và thông báo cho người dùng
- [ ] Quy tắc chống trùng: mã tham chiếu duy nhất, hành vi khi gửi lại
- [ ] Kịch bản timeout và quy trình tra soát, kèm thời hạn cụ thể
- [ ] Quy tắc hoàn tiền: khi nào, bao lâu, hoàn cả phí không
- [ ] Đặc tả đối soát: chu kỳ, khóa đối chiếu, xử lý chênh lệch
- [ ] Bút toán tương ứng với mỗi trạng thái (phối hợp với kế toán)
- [ ] Nội dung thông báo cho khách ở từng trạng thái, gồm cả trạng thái chưa rõ
- [ ] Hạn mức áp dụng và nguồn của hạn mức đó
