# Xác thực, phân quyền và bảo mật kênh số

Mục lục: [Ngưỡng xác thực](#ngưỡng-xác-thực-theo-quy-định-việt-nam) · [Sinh trắc học](#sinh-trắc-học) · [OTP](#otp) · [Thiết bị](#quản-lý-thiết-bị) · [Phiên](#phiên-đăng-nhập) · [OWASP](#các-lỗ-hổng-ưu-tiên-cho-ngân-hàng) · [Ràng buộc kênh](#các-ràng-buộc-khác-của-thông-tư-50)

> Phần quy định dưới đây dựa trên Thông tư 50/2024/TT-NHNN (ban hành 31/10/2024, hiệu lực
> 01/01/2025). Quy định có thể thay đổi — luôn đối chiếu văn bản hiện hành và đặc tả của dự
> án trước khi chốt ngưỡng trong test case.

## Ngưỡng xác thực theo quy định Việt Nam

Nội dung của Quyết định 2345/QĐ-NHNN đã được đưa vào Thông tư 50/2024/TT-NHNN, theo đó các
giao dịch chuyển tiền điện tử của cá nhân **trên 10 triệu đồng**, hoặc **tổng giá trị chuyển
tiền trong ngày vượt 20 triệu đồng**, hoặc **khi thay đổi thiết bị thực hiện giao dịch Mobile
Banking**, phải áp dụng một trong các biện pháp xác thực sinh trắc học.

Đây là ba điều kiện độc lập, nên phải test riêng từng cái và cả tổ hợp:

| Case | Số tiền | Dồn trong ngày trước đó | Kỳ vọng |
|---|---|---|---|
| Dưới cả hai ngưỡng | 5.000.000 | 0 | Không yêu cầu sinh trắc học |
| Đúng ngưỡng đơn lẻ | 10.000.000 | 0 | Theo đặc tả: "trên 10 triệu" nghĩa là đúng 10 triệu **chưa** kích hoạt — xác nhận lại với BA |
| Vượt ngưỡng đơn lẻ 1 đồng | 10.000.001 | 0 | Bắt buộc sinh trắc học |
| Nhiều lệnh nhỏ vượt dồn ngày | 3.000.000 | 18.000.000 | Lệnh này đẩy tổng lên 21 triệu → bắt buộc sinh trắc học |
| Dồn ngày chạm đúng 20 triệu | 2.000.000 | 18.000.000 | Xác nhận biên với BA |
| Đổi thiết bị | 100.000 | 0 | Bắt buộc sinh trắc học dù số tiền nhỏ |
| Reset dồn ngày | 5.000.000 | 19.000.000 hôm qua | Sang ngày mới, bộ đếm phải về 0 |

**Điểm hay sai trong triển khai, phải test kỹ:**
- Bộ đếm dồn ngày reset theo múi giờ nào và vào mấy giờ.
- Giao dịch bị hủy/hoàn có được trừ khỏi bộ đếm dồn ngày không.
- Bộ đếm tính theo kênh riêng hay gộp tất cả kênh.
- Chuyển tiền tới tài khoản của chính khách có tính vào ngưỡng không.
- Chặn ở server hay chỉ ở app — gọi thẳng API với số tiền vượt ngưỡng mà bỏ qua bước sinh
  trắc học phải bị từ chối. **Đây là case bắt buộc**; rất nhiều hệ thống chỉ kiểm ở client.

## Sinh trắc học

Thông tư 50 đặt yêu cầu định lượng cho hình thức khớp đúng bằng khuôn mặt: tỷ lệ từ chối sai
dưới 5% với tỷ lệ chấp nhận sai dưới 0,01% theo tiêu chuẩn FIDO Biometric Requirement trên
tập mẫu tối thiểu 10.000 mẫu, kèm khả năng phát hiện tấn công giả mạo vật thể sống
(Presentation Attack Detection). Giải pháp PAD phải được cấp chứng nhận bởi tổ chức hoặc
phòng thí nghiệm sinh trắc học được FIDO Alliance công nhận. Ngoài ra, nếu khách xác nhận
sai liên tiếp quá số lần do đơn vị quy định nhưng không quá 10 lần thì phải khóa chức năng
xác nhận bằng sinh trắc học, chỉ mở lại khi khách yêu cầu và phải kiểm tra khách trước khi
mở; thời gian thực hiện khớp đúng tối đa là 3 phút.

**Chuyển thành test case:**
- Khóa sau đúng số lần sai đã cấu hình (và không quá 10). Lần sai thứ N → khóa; thao tác
  tiếp theo phải bị từ chối kể cả khi đúng mặt.
- Mở khóa phải qua kênh có kiểm tra khách hàng, không tự động mở sau X phút.
- Quá 3 phút chưa hoàn tất khớp đúng → phiên xác thực hết hạn, phải làm lại từ đầu.
- Chống giả mạo: ảnh in, ảnh trên màn hình điện thoại, video quay lại → phải bị từ chối.
  Phần này thường test thủ công có kịch bản và biên bản, vì cần vật thật.
- Lần đầu dùng Mobile Banking trên thiết bị mới phải khớp sinh trắc học.
- Dữ liệu sinh trắc học khi lưu phải được mã hóa hoặc che giấu (Điều 19) — kiểm tra bằng
  cách đọc trực tiếp nơi lưu trữ ở môi trường test, không được thấy dữ liệu ở dạng rõ.

Lưu ý khi automation: **không dùng khuôn mặt thật của nhân viên** làm dữ liệu test lưu lâu
dài. Ở môi trường test nên có chế độ giả lập (stub) trả về kết quả khớp/không khớp theo cấu
hình, và chỉ chạy luồng sinh trắc học thật trong một bộ case thủ công riêng.

## OTP

- OTP sai → từ chối, không tiết lộ OTP đúng là gì qua thông báo lỗi.
- OTP hết hạn (thường 60–300 giây) → từ chối; kiểm tra đúng mốc hết hạn ở server.
- Dùng lại OTP đã dùng → từ chối. OTP là dùng một lần, kể cả còn trong thời hạn.
- Nhập sai quá số lần cho phép → khóa và yêu cầu khởi tạo lại giao dịch.
- Yêu cầu gửi lại OTP liên tục → phải có giới hạn tần suất, tránh bị dùng để quấy rối khách
  hàng hoặc đốt chi phí SMS.
- OTP của giao dịch A không dùng được cho giao dịch B (OTP phải gắn với nội dung giao dịch).
- Nội dung SMS OTP phải nêu rõ số tiền và người thụ hưởng, để khách phát hiện khi bị lừa.
- Đua điều kiện: xin OTP cho hai giao dịch gần như đồng thời → mỗi OTP chỉ xác nhận đúng
  giao dịch của nó.

## Quản lý thiết bị

Thông tư 50 yêu cầu với khách hàng cá nhân phải có chức năng kiểm tra khi khách truy cập lần
đầu hoặc truy cập bằng thiết bị khác với thiết bị gần nhất, tối thiểu gồm khớp đúng SMS OTP
hoặc Voice OTP, đồng thời khớp đúng sinh trắc học nếu quy định chuyên ngành yêu cầu thu thập
và lưu trữ thông tin sinh trắc học.

Case:
- Đăng nhập lần đầu trên thiết bị mới → bắt buộc xác thực bổ sung.
- Cài lại ứng dụng trên cùng thiết bị → tùy cách nhận diện thiết bị, có thể bị coi là mới.
  Hành vi phải nhất quán và đúng đặc tả.
- Danh sách thiết bị đã tin cậy: xem được, xóa được; xóa xong thiết bị đó phải bị buộc xác
  thực lại ở lần sau.
- Thiết bị đã bị gỡ bỏ nhưng còn token cũ → token phải bị vô hiệu ngay.
- Thiết bị đã bị bẻ khóa (root/jailbreak) → theo chính sách, thường là chặn.

## Phiên đăng nhập

- Hết hạn do không thao tác → quay về màn hình đăng nhập, **và** token phía server bị vô hiệu.
- Đăng xuất → token cũ gọi API phải trả 401, không chỉ xóa ở client.
- Đăng nhập đồng thời nhiều thiết bị → theo chính sách, thường ngân hàng chỉ cho một phiên.
- Đổi mật khẩu → mọi phiên khác bị đăng xuất.
- Ứng dụng chạy nền lâu rồi quay lại → yêu cầu xác thực lại.
- Ứng dụng không được có chức năng ghi nhớ mật khẩu truy cập (yêu cầu của Thông tư 50) —
  kiểm tra không có ô "ghi nhớ mật khẩu", và mật khẩu không nằm trong bộ nhớ lưu trữ cục bộ.

## Các lỗ hổng ưu tiên cho ngân hàng

Bám theo OWASP Top 10 nhưng ưu tiên theo thiệt hại tiền:

**Phân quyền hỏng (quan trọng nhất)** — dùng token của khách A gọi API xem/chuyển tiền tài
khoản của khách B. Test cho **mọi** endpoint có tham số định danh, không chỉ vài cái tiêu
biểu. Đây là lỗ hổng gây thiệt hại lớn nhất và dễ sót nhất vì UI không bao giờ tạo ra request
đó.

```python
@pytest.mark.parametrize("endpoint", ALL_ACCOUNT_ENDPOINTS)
def test_khong_truy_cap_tai_khoan_nguoi_khac(endpoint, token_a, account_b):
    r = client.get(endpoint.format(acc=account_b.no), headers=auth(token_a))
    assert r.status_code in (403, 404), f"{endpoint} lo du lieu tai khoan nguoi khac"
    assert account_b.no not in r.text
```

**Leo thang quyền** — người dùng vai trò giao dịch viên gọi API của vai trò kiểm soát viên.
Xem `maker-checker-rbac.md`.

**Thao túng tham số** — sửa số tiền, sửa tài khoản nguồn, sửa phí trong request. Server phải
tính lại mọi thứ, không tin giá trị từ client. Case kinh điển: sửa số tiền phí về 0.

**Số âm và tràn số** — số tiền âm, số tiền cực lớn, số có nhiều chữ số thập phân hơn cho phép.

**Chèn mã** — SQL injection ở ô tìm kiếm giao dịch, nội dung chuyển khoản; chèn script vào
nội dung chuyển khoản rồi xem nó hiển thị ở sao kê hoặc email.

**Ghi log thiếu** — thao tác nhạy cảm không để lại vết. Test bằng cách làm thao tác rồi kiểm
tra log tồn tại với đủ trường.

**Lộ thông tin qua thông báo lỗi** — "Tài khoản không tồn tại" và "Sai mật khẩu" phải cho
cùng một thông báo, nếu không kẻ tấn công dò được tài khoản nào có thật.

## Các ràng buộc khác của Thông tư 50

Những yêu cầu này dịch thẳng thành test case kiểm tra được:

- **Không gửi SMS hoặc thư điện tử có chứa đường dẫn liên kết** cho khách hàng, trừ khi khách
  yêu cầu. Test: kích hoạt mọi loại thông báo và quét nội dung tìm `http`/`https`.
- **Ứng dụng không được có chức năng ghi nhớ mật khẩu truy cập.**
- **Mã khóa bí mật, mã PIN, thông tin sinh trắc học khi lưu trữ phải được mã hóa hoặc che
  giấu** để bảo đảm tính bí mật.
- **Phân quyền truy cập dữ liệu khách hàng theo đúng chức năng nhiệm vụ, có giám sát mỗi lần
  truy cập.** Test: tài khoản nhân viên không thuộc chi nhánh quản lý không xem được hồ sơ;
  mỗi lần xem sinh một bản ghi giám sát.
- **Thông báo cho khách hàng khi xảy ra sự cố lộ lọt dữ liệu và báo cáo Ngân hàng Nhà nước.**
  Phần quy trình, nhưng cơ chế thông báo hàng loạt nên có kịch bản diễn tập.

Khi viết test case cho các mục này, ghi rõ điều khoản tham chiếu vào trường mô tả của case —
kiểm toán nội bộ sẽ cần đúng ánh xạ đó.
