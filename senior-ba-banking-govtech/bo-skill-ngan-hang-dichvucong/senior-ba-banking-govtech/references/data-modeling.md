# Mô hình dữ liệu và ánh xạ dữ liệu

Mục lục: [Vai trò của BA](#vai-trò-của-ba-với-dữ-liệu) · [Mô hình khái niệm](#mô-hình-khái-niệm) · [Từ điển dữ liệu](#từ-điển-dữ-liệu) · [Ánh xạ dữ liệu](#ánh-xạ-dữ-liệu) · [Di trú dữ liệu](#di-trú-dữ-liệu) · [Dữ liệu dùng chung](#dữ-liệu-dùng-chung-và-danh-mục) · [Vòng đời dữ liệu](#vòng-đời-và-lưu-trữ)

## Vai trò của BA với dữ liệu

BA không thiết kế cơ sở dữ liệu vật lý. Việc của BA là trả lời được: **hệ thống cần biết
những gì, mỗi thứ nghĩa là gì, lấy từ đâu, ai được thấy, giữ bao lâu.** Nếu không làm rõ
những câu này thì đội phát triển sẽ tự quyết, và hậu quả lộ ra khi tích hợp hoặc khi báo cáo
ra số không khớp.

Dấu hiệu mô hình dữ liệu chưa xong: hai phòng ban dùng cùng một từ với hai nghĩa khác nhau.
Ví dụ "khách hàng đang hoạt động" — với bộ phận bán hàng là người còn liên lạc được, với bộ
phận vận hành là tài khoản chưa đóng, với bộ phận rủi ro là có giao dịch trong 90 ngày. Ba
định nghĩa, ba con số báo cáo, và những cuộc họp dài vô ích.

## Mô hình khái niệm

Vẽ thực thể và quan hệ ở mức nghiệp vụ, không có khóa ngoại, không có kiểu dữ liệu. Mục tiêu
là để người nghiệp vụ nhìn vào xác nhận đúng/sai.

**Ví dụ miền dịch vụ công:**
```
Công dân ──(1:n)── Hồ sơ ──(n:1)── Thủ tục hành chính
                     │                      │
                  (1:n)                  (1:n)
                     │                      │
              Thành phần hồ sơ      Thành phần yêu cầu
                     │
                  (1:n)
                     │
              Bước xử lý ──(n:1)── Cán bộ ──(n:1)── Cơ quan
```

**Ví dụ miền ngân hàng:**
```
Khách hàng (CIF) ──(1:n)── Tài khoản ──(1:n)── Giao dịch ──(1:n)── Bút toán
       │                        │
    (1:n)                    (n:1)
       │                        │
   Giấy tờ định danh      Sản phẩm ──(1:n)── Biểu phí / Hạn mức
```

**Câu hỏi phải trả lời cho mỗi quan hệ:**
- Bản số thật là bao nhiêu (1:1, 1:n, n:n)? Một công dân có thể có nhiều hồ sơ cùng thủ tục
  đang xử lý không? Một tài khoản có thể có nhiều chủ không?
- Quan hệ bắt buộc hay tùy chọn? Hồ sơ bắt buộc phải gắn với công dân, nhưng công dân có thể
  chưa có hồ sơ nào.
- Khi xóa một bên thì bên kia thế nào? (Ở cả hai miền, câu trả lời gần như luôn là "không
  xóa, chỉ đánh dấu ngừng hiệu lực" vì lý do lưu trữ và kiểm toán.)

## Từ điển dữ liệu

Mỗi trường dữ liệu quan trọng cần một dòng. Đây là tài liệu BA phải viết, không phải dev.

| Trường | Ý nghĩa nghiệp vụ | Kiểu / Định dạng | Bắt buộc | Quy tắc hợp lệ | Nguồn | Nhạy cảm |
|---|---|---|---|---|---|---|
| so_dinh_danh | Số định danh cá nhân | Chuỗi 12 số | Có | 12 chữ số, kiểm tra với cơ sở dữ liệu dân cư | Hệ thống định danh | Có — dữ liệu cá nhân |
| ngay_nop | Thời điểm hồ sơ được nộp | Ngày giờ, có múi giờ | Có | Không ở tương lai | Hệ thống sinh | Không |
| ngay_hop_le | Thời điểm hồ sơ được xác nhận đầy đủ | Ngày giờ | Không | ≥ ngay_nop; **mốc bắt đầu tính thời hạn** | Cán bộ tiếp nhận | Không |
| so_tien | Số tiền giao dịch | Số nguyên, đơn vị đồng | Có | > 0; ≤ hạn mức theo BR-014 | Người dùng nhập | Không |
| trang_thai | Trạng thái hồ sơ | Danh mục | Có | Thuộc danh sách trạng thái hợp lệ | Hệ thống | Không |

**Những điều hay bị bỏ sót và gây lỗi về sau:**
- **Đơn vị đo**: số tiền tính bằng đồng hay nghìn đồng? Ghi rõ. Dùng số nguyên cho tiền,
  không dùng số thực.
- **Múi giờ**: lưu UTC hay giờ địa phương? Nghiệp vụ tính theo GMT+7 nhưng hệ thống thường
  lưu UTC — chỗ này sinh lỗi lệch ngày.
- **Ngày lịch hay ngày làm việc**: "5 ngày" trong văn bản hành chính gần như luôn là ngày
  làm việc, và phải trừ ngày nghỉ lễ.
- **Độ dài tối đa**: tên người Việt có thể dài; địa chỉ sau sắp xếp đơn vị hành chính đổi;
  trường quá ngắn sẽ cắt mất dữ liệu khi tích hợp.
- **Giá trị rỗng có nghĩa gì**: chưa nhập, không áp dụng, hay bằng không? Ba thứ khác nhau.
- **Dữ liệu nhạy cảm**: đánh dấu để áp dụng phân quyền, mã hóa, và quy tắc che khi hiển thị.

## Ánh xạ dữ liệu

Khi dữ liệu chảy giữa hai hệ thống, mọi khác biệt phải được giải quyết tường minh trong tài
liệu, không để lúc lập trình mới phát hiện.

| Nguồn | Trường nguồn | Đích | Trường đích | Quy tắc chuyển đổi | Xử lý khi không ánh xạ được |
|---|---|---|---|---|---|
| Cổng dịch vụ công | gioiTinh: "Nam"/"Nữ" | Hệ thống chuyên ngành | gender: 1/2 | Nam→1, Nữ→2 | Giá trị khác → từ chối hồ sơ, ghi lỗi |
| Hệ thống định danh | diaChiThuongTru (chuỗi) | Hệ thống nội bộ | tinh, xa, diaChiChiTiet | Tách theo mã đơn vị hành chính hiện hành | Không tách được → giữ nguyên chuỗi, đánh dấu cần chuẩn hóa |
| Core banking | amount (BigDecimal) | Cổng thanh toán | amount (chuỗi, không thập phân) | VND nhân 1, bỏ phần thập phân | Có phần thập phân khác 0 → lỗi, không tự làm tròn |

**Ba câu hỏi bắt buộc cho mỗi dòng ánh xạ:**
1. Kiểu và độ dài có vừa không? Trường nguồn 200 ký tự đổ vào trường đích 50 ký tự sẽ mất
   dữ liệu — quyết định cắt hay từ chối, và ghi rõ.
2. Tập giá trị có khớp không? Danh mục hai bên hiếm khi trùng hoàn toàn.
3. Chuyện gì xảy ra khi dữ liệu nguồn thiếu hoặc sai? Mặc định, từ chối, hay đánh dấu xử lý
   thủ công? **Không bao giờ để trống ô này** — đây là nguồn gốc của phần lớn sự cố tích hợp.

**Đặc biệt lưu ý với địa giới hành chính**: sau sắp xếp đơn vị hành chính, dữ liệu địa chỉ cũ
và mới cùng tồn tại. Mọi ánh xạ liên quan địa chỉ phải nêu rõ dùng danh mục phiên bản nào và
xử lý ra sao với dữ liệu lịch sử. Đây là điểm gây lỗi hàng loạt ở cả hai miền.

## Di trú dữ liệu

Dự án thay thế hệ thống cũ luôn có phần này, và nó luôn bị đánh giá thấp về công sức.

**Các câu hỏi BA phải làm rõ:**
- Di trú những gì: toàn bộ lịch sử, hay chỉ dữ liệu đang hoạt động? Dữ liệu cũ tra cứu ở đâu?
- Chất lượng dữ liệu nguồn ra sao? Chạy thử một đợt phân tích để đếm: bao nhiêu bản ghi
  thiếu trường bắt buộc, bao nhiêu trùng lặp, bao nhiêu sai định dạng. Con số này quyết định
  kế hoạch, và thường tệ hơn mọi người nghĩ.
- Quy tắc xử lý dữ liệu bẩn: sửa tự động theo quy tắc, để nguyên và đánh dấu, hay loại ra
  xử lý thủ công? Ai quyết định và ai làm phần thủ công?
- Đối chiếu sau di trú: đếm số bản ghi, đối chiếu tổng số tiền/số hồ sơ, kiểm tra mẫu ngẫu
  nhiên. Tiêu chí đạt phải ghi rõ trước khi chạy.
- Phương án quay lui nếu di trú hỏng.
- Giai đoạn chạy song song: hai hệ thống cùng chạy bao lâu, dữ liệu đồng bộ theo chiều nào?

## Dữ liệu dùng chung và danh mục

Danh mục (đơn vị hành chính, dân tộc, tôn giáo, ngành nghề, loại giấy tờ, mã ngân hàng, mã
thủ tục) là thứ nhỏ nhưng gây nhiều lỗi.

**Nguyên tắc:**
- Xác định **nguồn duy nhất đúng** cho mỗi danh mục. Ở dịch vụ công thường là danh mục dùng
  chung quốc gia; ở ngân hàng thường là hệ thống core.
- Danh mục phải có **hiệu lực theo thời gian**, không xóa giá trị cũ. Hồ sơ nộp năm ngoái
  phải hiển thị đúng tên đơn vị hành chính tại thời điểm đó.
- Cơ chế cập nhật: thủ công hay đồng bộ tự động, tần suất nào, ai chịu trách nhiệm.
- Ánh xạ giữa danh mục nội bộ và danh mục quốc gia phải được duy trì như một bảng dữ liệu,
  không nhúng vào mã nguồn.

## Vòng đời và lưu trữ

Với mỗi loại dữ liệu, xác định:
- **Tạo**: ai tạo, qua kênh nào, cần phê duyệt không.
- **Sửa**: ai được sửa, sửa rồi có lưu vết giá trị cũ không (ở hai miền này câu trả lời gần
  như luôn là có).
- **Đọc**: ai được đọc, đọc có ghi nhật ký không, dữ liệu nhạy cảm che thế nào khi hiển thị.
- **Ngừng hiệu lực**: đánh dấu thay vì xóa. Xóa vật lý chỉ làm khi hết thời hạn lưu trữ và
  có quy định cho phép.
- **Lưu trữ**: giữ bao lâu theo quy định về lưu trữ và kế toán; sau đó chuyển kho lưu trữ hay
  hủy; hủy thì lập biên bản thế nào.

Điểm mâu thuẫn hay gặp: **quyền xóa dữ liệu cá nhân** của chủ thể dữ liệu so với **nghĩa vụ
lưu trữ chứng từ** theo pháp luật kế toán và lưu trữ. Đây là câu hỏi BA phải nêu ra sớm và
có ý kiến chính thức của bộ phận pháp chế, không phải tự quyết. Xem `legal-compliance.md`.
