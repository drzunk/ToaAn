# Kiến thức miền ngân hàng cho BA

Mục lục: [Bản đồ hệ thống](#bản-đồ-hệ-thống-một-ngân-hàng) · [Khái niệm cốt lõi](#khái-niệm-cốt-lõi) · [Sản phẩm](#nhóm-sản-phẩm-và-điều-cần-hỏi) · [Kênh](#kênh-phân-phối) · [Hạn mức và phí](#hạn-mức-và-phí) · [Onboarding](#mở-tài-khoản-và-định-danh) · [Câu hỏi kiểm tra](#câu-hỏi-tự-kiểm-tra)

## Bản đồ hệ thống một ngân hàng

Hiểu bản đồ này giúp BA biết một yêu cầu sẽ chạm vào đâu và cần hỏi ai.

```
        Kênh                  Tầng giữa                  Lõi và vệ tinh
┌──────────────────┐   ┌────────────────────┐   ┌──────────────────────┐
│ Quầy giao dịch   │   │ Cổng API / ESB     │   │ Core banking         │
│ Internet Banking │──▶│ Điều phối nghiệp vụ│──▶│ (tài khoản, bút toán)│
│ Mobile Banking   │   │ Xác thực, hạn mức  │   │ Thẻ (card management)│
│ ATM / POS        │   │ Chống gian lận     │   │ Tín dụng, tiền gửi   │
│ Đối tác, ví      │   │ Hàng đợi, sự kiện  │   │ Kho dữ liệu, báo cáo │
└──────────────────┘   └────────────────────┘   └──────────────────────┘
                                │
                     ┌──────────┴──────────┐
                     │ Bên ngoài           │
                     │ NAPAS, CITAD, SWIFT │
                     │ Tổ chức thẻ, eKYC   │
                     │ CIC, cơ quan thuế   │
                     └─────────────────────┘
```

**Hệ quả thực tế cho BA:**
- Core banking thường là hệ thống mua sẵn, sửa rất đắt và chậm. Yêu cầu chạm vào core cần
  được nêu sớm và ước lượng riêng.
- Logic nghiệp vụ nên nằm ở tầng giữa khi có thể, nhưng **bút toán luôn ở core**.
- Một tính năng "nhỏ" ở màn hình có thể kéo theo thay đổi ở 4 hệ thống. Luôn vẽ sơ đồ luồng
  dữ liệu trước khi cam kết phạm vi.

## Khái niệm cốt lõi

| Khái niệm | Ý nghĩa | Vì sao BA cần biết |
|---|---|---|
| CIF | Hồ sơ khách hàng, một khách nhiều tài khoản | Quyết định mô hình dữ liệu và nghiệp vụ hợp nhất khách hàng |
| Tài khoản thanh toán | Tài khoản giao dịch hằng ngày | Loại tiền tệ cố định theo tài khoản |
| Số dư sổ sách / khả dụng | Sổ sách là đã hạch toán; khả dụng đã trừ khoản đang giữ và số dư tối thiểu | Nhầm hai thứ này gây đặc tả sai ở mọi màn hình liên quan tiền |
| Khoản giữ (hold) | Tiền bị phong tỏa tạm, chưa hạch toán | Quyết định khách có chuyển tiền được không |
| Bút toán kép | Mọi giao dịch có tổng nợ bằng tổng có | Đặc tả phải nêu bút toán, không chỉ nêu số dư thay đổi |
| GL | Sổ cái, tài khoản kế toán nội bộ | Phí, thuế, tài khoản trung gian đều vào đây |
| Cut-off | Mốc giờ chia ngày làm việc | Giao dịch sau mốc này tính sang ngày kế tiếp |
| Value date | Ngày bắt đầu tính lãi, có thể khác ngày hạch toán | Ảnh hưởng tính lãi và đối soát |
| Maker–checker | Người tạo khác người duyệt | Gần như mọi nghiệp vụ nội bộ đều có |
| Đối soát | So khớp dữ liệu với đối tác cuối ngày | Mọi tích hợp thanh toán đều cần |

## Nhóm sản phẩm và điều cần hỏi

**Tài khoản thanh toán**
- Điều kiện mở, đối tượng được mở, hồ sơ cần gì
- Số dư tối thiểu duy trì, phí quản lý
- Trạng thái tài khoản: hoạt động, tạm khóa, phong tỏa, ngủ đông, đóng — và ai được chuyển
  giữa các trạng thái
- Tài khoản đồng sở hữu xử lý thế nào

**Tiền gửi có kỳ hạn**
- Kỳ hạn, lãi suất theo kỳ hạn, cách tính lãi (cuối kỳ, định kỳ, trả trước)
- Tất toán trước hạn: lãi tính lại thế nào
- Tự động gia hạn: gia hạn cả gốc và lãi hay chỉ gốc

**Tín dụng**
- Quy trình từ đề nghị → thẩm định → phê duyệt → giải ngân → thu nợ
- Chấm điểm tín dụng, tra cứu thông tin tín dụng
- Lịch trả nợ, trả nợ trước hạn, nợ quá hạn và phân loại nhóm nợ
- Tài sản bảo đảm

**Thẻ**
- Vòng đời: đăng ký, phát hành, kích hoạt, sử dụng, khóa, gia hạn, hủy
- Thẻ ghi nợ khác thẻ tín dụng về luồng tiền: ghi nợ trừ trực tiếp, tín dụng có chu kỳ sao
  kê và hạn thanh toán
- Ba giai đoạn giao dịch thẻ: cấp phép (giữ tiền) → quyết toán (trừ tiền thật) → bù trừ.
  Khoảng cách giữa chúng sinh ra nhiều yêu cầu nghiệp vụ

**Thanh toán và chuyển tiền** — xem `payments-domain.md`.

## Kênh phân phối

Cùng một nghiệp vụ nhưng mỗi kênh có ràng buộc khác nhau, và đây là nguồn gốc của nhiều yêu
cầu bị bỏ sót:

| Kênh | Hạn mức | Xác thực | Ràng buộc riêng |
|---|---|---|---|
| Quầy | Cao nhất | Giấy tờ tùy thân, chữ ký, maker-checker | Theo giờ làm việc |
| Internet Banking | Trung bình | Mật khẩu + OTP, sinh trắc học theo ngưỡng | Phiên dễ bị tấn công hơn |
| Mobile Banking | Trung bình | Sinh trắc học thiết bị, OTP | Quản lý thiết bị, cài lại app |
| ATM | Thấp, theo mệnh giá | Thẻ + PIN | Phụ thuộc tiền trong máy |
| Đối tác/ví | Theo thỏa thuận | Theo cơ chế liên kết | Hạn mức riêng, đối soát riêng |

**Câu hỏi BA phải hỏi với mọi tính năng mới:** tính năng này có trên kênh nào, hạn mức mỗi
kênh, và nếu khách làm dở ở kênh này có tiếp tục được ở kênh khác không?

## Hạn mức và phí

**Hạn mức có nhiều tầng, và chúng cộng dồn theo cách khác nhau:**
- Theo giao dịch (mỗi lần tối đa bao nhiêu)
- Theo ngày (tổng trong ngày), theo tháng
- Theo kênh, theo sản phẩm, theo nhóm khách hàng
- Theo quy định của cơ quan quản lý (không được vượt dù khách yêu cầu)
- Hạn mức khách tự đặt (trong khung ngân hàng cho phép)

Câu hỏi phải làm rõ: bộ đếm reset lúc nào, giao dịch bị hủy có trừ khỏi bộ đếm không, hạn
mức tính riêng từng kênh hay gộp, ai được duyệt vượt hạn mức.

**Phí:**
- Thu của ai (người gửi hay người nhận), thu lúc nào (khởi tạo hay khi thành công)
- Miễn giảm theo nhóm khách hàng, theo gói dịch vụ, theo chương trình khuyến mãi
- Phí có VAT không, làm tròn thế nào
- **Hoàn phí khi giao dịch thất bại** — hay bị quên trong đặc tả

Nguyên tắc đặc tả: biểu phí và hạn mức phải **cấu hình được**, không cố định trong mã nguồn.
Chúng thay đổi thường xuyên hơn mọi thứ khác và thay đổi thường gấp.

## Mở tài khoản và định danh

Quy trình mở tài khoản trực tuyến là nơi giao nhau giữa nghiệp vụ, công nghệ và quy định.

**Các bước điển hình:**
```
Nhập thông tin → Chụp/đọc giấy tờ tùy thân → Đối chiếu khuôn mặt →
Kiểm tra dữ liệu (dân cư, danh sách cảnh báo) → Chấm rủi ro →
Duyệt (tự động hoặc thủ công) → Tạo CIF → Mở tài khoản → Kích hoạt dịch vụ
```

**Điểm BA phải làm rõ:**
- Trường hợp nào được duyệt tự động, trường hợp nào chuyển thẩm định thủ công
- Xử lý khi ảnh giấy tờ mờ, thông tin không khớp, khuôn mặt không khớp — mỗi trường hợp một
  thông báo và một đường đi tiếp
- Khách hàng bỏ dở giữa chừng: giữ dữ liệu bao lâu, quay lại có phải làm lại từ đầu không
- Khách đã có CIF cũ: hợp nhất hay tạo mới
- Người nước ngoài, người chưa đủ tuổi, người mất năng lực hành vi
- Mức độ định danh ảnh hưởng tới hạn mức được dùng

Yêu cầu về xác thực và sinh trắc học chịu ràng buộc của quy định hiện hành — xem
`legal-compliance.md`.

## Câu hỏi tự kiểm tra

Trước khi nộp một đặc tả nghiệp vụ ngân hàng, tự trả lời:

- [ ] Nghiệp vụ này phát sinh bút toán gì, vào tài khoản nào?
- [ ] Ai làm, ai duyệt, ngưỡng nào lên cấp cao hơn?
- [ ] Có phí không, thu của ai, lúc nào, hoàn khi nào?
- [ ] Hạn mức nào áp dụng, lấy từ cấu hình hay cố định?
- [ ] Giao dịch thất bại giữa chừng thì tiền ở đâu, ai xử lý, trong bao lâu?
- [ ] Khách hàng được thông báo qua kênh nào, nội dung gồm gì?
- [ ] Nghiệp vụ này xuất hiện ở báo cáo nào, đối soát với ai?
- [ ] Có ràng buộc pháp quy nào không, điều khoản nào?
- [ ] Sau cut-off thì hành vi khác đi thế nào?
- [ ] Tính năng này có trên kênh nào, khác nhau ra sao giữa các kênh?

Mười câu này là bộ lọc tốt: một đặc tả trả lời được cả mười thường là đặc tả đội phát triển
làm được mà không phải quay lại hỏi.
