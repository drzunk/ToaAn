# Miền dịch vụ công

> Thông tin pháp lý dưới đây phục vụ định hướng phân tích, không thay thế việc đọc văn bản
> gốc. Văn bản thay đổi thường xuyên — luôn đối chiếu bản hiện hành.

Mục lục: [Khái niệm](#khái-niệm-nền-tảng) · [Mức độ trực tuyến](#hai-mức-độ-dịch-vụ-công-trực-tuyến) · [Một cửa](#cơ-chế-một-cửa-và-một-cửa-liên-thông) · [Chính quyền 2 cấp](#chính-quyền-địa-phương-2-cấp) · [Vòng đời hồ sơ](#vòng-đời-hồ-sơ) · [Thời hạn](#tính-thời-hạn-giải-quyết) · [Câu hỏi khai thác](#câu-hỏi-khai-thác-đặc-thù)

## Khái niệm nền tảng

| Khái niệm | Ý nghĩa | Vì sao BA cần |
|---|---|---|
| Thủ tục hành chính (TTHC) | Trình tự, cách thức, hồ sơ, yêu cầu để cơ quan nhà nước giải quyết một công việc | Đơn vị nghiệp vụ cơ bản, có mã định danh |
| Quyết định công bố TTHC | Văn bản công bố chính thức nội dung thủ tục | **Nguồn chuẩn** cho thành phần hồ sơ, thời hạn, phí |
| Cơ sở dữ liệu quốc gia về TTHC | Nơi tập trung thông tin TTHC | Danh mục và thông tin dịch vụ công trực tuyến phải được cập nhật và đồng bộ ở đây |
| Bộ phận Một cửa | Nơi tiếp nhận và trả kết quả | Điểm chạm chính của người dân |
| Một cửa liên thông | Một lần nộp, nhiều cơ quan phối hợp giải quyết | Sinh ra yêu cầu điều phối và theo dõi phức tạp |
| Hệ thống thông tin giải quyết TTHC | Hệ thống của bộ/tỉnh xử lý hồ sơ | Nơi phần lớn công việc BA diễn ra |
| Cổng Dịch vụ công quốc gia | Cổng tập trung quốc gia | Nơi người dân nộp trực tuyến, và là điểm tích hợp bắt buộc |

## Hai mức độ dịch vụ công trực tuyến

Cách phân loại cũ theo 4 mức độ đã được thay bằng 2 mức. Theo Nghị định 42/2022/NĐ-CP, dịch
vụ công trực tuyến được phân thành hai mức: **trực tuyến toàn trình** và **trực tuyến một
phần**. Toàn trình là dịch vụ bảo đảm cung cấp toàn bộ thông tin về thủ tục hành chính, việc
thực hiện và giải quyết thủ tục hành chính đều được thực hiện trên môi trường mạng, việc trả
kết quả được thực hiện trực tuyến hoặc qua dịch vụ bưu chính công ích. Một phần là dịch vụ
không bảo đảm các điều kiện của toàn trình.

Nghị định cũng yêu cầu dịch vụ công trực tuyến khi cung cấp phải được chuẩn hóa và đồng bộ
về mã, tên dịch vụ, có biểu mẫu điện tử kèm theo, có hướng dẫn quy trình sử dụng cho tổ
chức, cá nhân và hướng dẫn quy trình xử lý cho cơ quan nhà nước, đồng bộ với Cơ sở dữ liệu
quốc gia về thủ tục hành chính.

**Hệ quả cho BA:** khi khách hàng nói "làm dịch vụ công mức 4", hãy quy đổi sang ngôn ngữ
hiện hành và xác định cụ thể điều gì còn phải làm ngoài mạng. Một dịch vụ chỉ cần một bước
bắt buộc đến trực tiếp (ví dụ lấy mẫu sinh trắc, xuất trình bản gốc) thì không phải toàn
trình — và việc nhận diện đúng điều này ngay từ đầu tránh được cam kết sai với cơ quan chủ
quản.

**Câu hỏi kiểm tra tính toàn trình:**
- Người dân có phải đến nộp bản gốc giấy tờ nào không?
- Có bước nào cần chữ ký tươi hoặc xuất trình trực tiếp không?
- Kết quả có bắt buộc là bản giấy không, hay bản điện tử có giá trị pháp lý?
- Phí lệ phí nộp trực tuyến được không?

## Cơ chế một cửa và một cửa liên thông

Nghị định 118/2025/NĐ-CP ngày 09/6/2025 quy định việc thực hiện thủ tục hành chính theo cơ
chế một cửa, một cửa liên thông tại Bộ phận Một cửa và Cổng Dịch vụ công quốc gia, có hiệu
lực từ 01/7/2025, thay thế Nghị định 61/2018.

**Các điểm ảnh hưởng trực tiếp tới thiết kế hệ thống:**

- Ở cấp tỉnh, Ủy ban nhân dân cấp tỉnh quyết định thành lập Trung tâm Phục vụ hành chính công
  cấp tỉnh là tổ chức hành chính thuộc Văn phòng Ủy ban nhân dân cấp tỉnh, có con dấu và tài
  khoản riêng. Ở cấp xã, Ủy ban nhân dân cấp xã quyết định thành lập Trung tâm Phục vụ hành
  chính công cấp xã. Địa phương chọn mô hình Trung tâm Phục vụ hành chính công một cấp trực
  thuộc Ủy ban nhân dân cấp tỉnh thì không tổ chức trung tâm cấp xã.
- **Trung tâm Phục vụ hành chính công cấp tỉnh và cấp xã tiếp nhận thủ tục hành chính không
  phụ thuộc vào địa giới hành chính trong phạm vi cấp tỉnh.** Đây là thay đổi lớn nhất về mặt
  thiết kế: nơi tiếp nhận và nơi giải quyết có thể khác nhau, nên hệ thống bắt buộc phải có
  cơ chế **định tuyến hồ sơ** tới cơ quan có thẩm quyền, và cơ chế theo dõi hồ sơ xuyên đơn vị.
- Tổ chức, cá nhân có thể nộp hồ sơ và nhận kết quả qua nhiều cách thức, trong đó có trực
  tuyến tại Cổng Dịch vụ công quốc gia và qua dịch vụ bưu chính công ích.

**Yêu cầu hệ thống phát sinh từ đây:**
- Định tuyến hồ sơ theo thẩm quyền, không theo nơi nộp
- Theo dõi trạng thái xuyên cơ quan, người dân chỉ cần một mã hồ sơ
- Phân quyền theo thẩm quyền giải quyết chứ không theo địa bàn nơi tiếp nhận
- Xử lý trường hợp không xác định được cơ quan thẩm quyền — phải có hàng chờ điều phối, không
  để hồ sơ treo

## Chính quyền địa phương 2 cấp

Từ 01/7/2025, mô hình chính quyền địa phương 2 cấp đi vào hoạt động. Thẩm quyền giải quyết
thủ tục hành chính đã được phân định lại: nhiều thủ tục vốn thuộc cấp huyện được chuyển về
cấp tỉnh hoặc xuống cấp xã, một số thủ tục bị bãi bỏ, và hàng trăm thủ tục được phân cấp từ
trung ương cho địa phương.

**Đây là thứ BA phải kiểm tra đầu tiên** khi làm bất cứ dự án dịch vụ công nào trong giai
đoạn này. Một đặc tả dựa trên bảng thẩm quyền cũ sẽ sai từ gốc.

**Danh sách kiểm tra:**
- [ ] Thủ tục này hiện thuộc thẩm quyền cấp nào? Có văn bản phân định mới không?
- [ ] Dữ liệu hồ sơ cũ gắn với đơn vị hành chính đã sáp nhập xử lý thế nào? Vẫn tra cứu được
      không, hiển thị tên cũ hay tên mới?
- [ ] Danh mục đơn vị hành chính dùng phiên bản nào, có lưu lịch sử không?
- [ ] Tài khoản cán bộ, phân quyền, luồng phê duyệt có được cập nhật theo tổ chức mới không?
- [ ] Địa chỉ trong hồ sơ cũ (theo đơn vị hành chính cũ) đối chiếu ra sao với dữ liệu mới?
- [ ] Kết quả đã ban hành theo thẩm quyền cũ còn giá trị không, tra cứu ở đâu?

Câu hỏi về dữ liệu lịch sử là câu hỏi hay bị bỏ qua và gây lỗi hàng loạt khi hệ thống lên
chạy thật.

## Vòng đời hồ sơ

```
Nộp ──→ Tiếp nhận ──┬─→ Hợp lệ ──→ Thụ lý ──→ Phê duyệt ──→ Trả kết quả ──→ Kết thúc
                    │                  │
                    └─→ Yêu cầu bổ sung│   (dừng đếm thời hạn)
                            │          │
                            └──────────┘
                                       └─→ Từ chối (có lý do bằng văn bản)

Nhánh khác: Rút hồ sơ (do người dân) | Quá hạn bổ sung → hủy
```

**Với mỗi chuyển đổi trạng thái, đặc tả phải nêu:**
- Ai có quyền thực hiện
- Điều kiện để chuyển
- Thời hạn cho bước này
- Thông báo gửi cho ai, qua kênh nào, nội dung gì
- Lưu vết: ai, lúc nào, lý do (bắt buộc với từ chối và yêu cầu bổ sung)

**Các trạng thái hay bị quên:** hồ sơ lưu nháp chưa nộp, hồ sơ chờ thanh toán phí, hồ sơ
đang chờ cơ quan phối hợp trả lời, hồ sơ có kết quả nhưng người dân chưa nhận.

## Tính thời hạn giải quyết

Đây là nơi sinh nhiều lỗi nhất và cũng là nơi người dân khiếu nại nhiều nhất.

**Các quy tắc phải làm rõ tường minh:**
- **Mốc bắt đầu**: tính từ khi tiếp nhận hồ sơ **hợp lệ**, không phải từ khi nộp. Hai thời
  điểm này khác nhau và hệ thống phải lưu cả hai.
- **Đơn vị**: ngày làm việc, không phải ngày lịch. Phải có danh mục ngày nghỉ lễ cấu hình
  được, cập nhật hằng năm.
- **Dừng và nối lại**: khi yêu cầu bổ sung, thời hạn dừng đếm; nối lại khi nhận đủ hồ sơ bổ
  sung. Hệ thống phải lưu từng khoảng dừng để giải trình được.
- **Liên thông**: tổng thời hạn so với thời hạn từng cơ quan. Nếu một cơ quan trễ thì tổng
  thể trễ — cần cảnh báo sớm chứ không chỉ báo khi đã trễ.
- **Gia hạn**: trường hợp nào được gia hạn, ai duyệt, có phải thông báo cho người dân không.
- **Cảnh báo sắp đến hạn**: trước bao lâu, gửi cho ai (cán bộ thụ lý và lãnh đạo).

**Yêu cầu dữ liệu tối thiểu để tính đúng:** thời điểm nộp, thời điểm tiếp nhận hợp lệ, các
khoảng dừng (bắt đầu–kết thúc–lý do), thời hạn quy định, thời hạn dự kiến trả, thời điểm trả
thực tế. Thiếu bất kỳ trường nào trong số này thì báo cáo đúng hạn/trễ hạn sẽ sai.

## Câu hỏi khai thác đặc thù

Dùng khi làm việc với cơ quan chủ quản:

**Về thủ tục**
- Mã thủ tục là gì, công bố tại quyết định nào, còn hiệu lực không?
- Thành phần hồ sơ gồm những gì? Giấy tờ nào đã có trong cơ sở dữ liệu quốc gia và có thể
  bỏ không bắt người dân nộp?
- Bản sao có cần chứng thực không? Bản điện tử được chấp nhận ở dạng nào?
- Biểu mẫu do văn bản nào quy định? Có bắt buộc giữ nguyên bố cục không?

**Về xử lý**
- Sau sắp xếp, cơ quan nào giải quyết? Có liên thông với cơ quan nào?
- Bao nhiêu bước, mỗi bước ai làm, thời hạn bao nhiêu?
- Trường hợp nào phải yêu cầu bổ sung, thường chiếm bao nhiêu phần trăm?
- Kết quả trả ở dạng gì, ai ký, ký số hay ký tươi?

**Về người dân**
- Nhóm nào dùng nhiều nhất, độ tuổi, thói quen sử dụng thiết bị?
- Bao nhiêu phần trăm nộp trực tuyến hiện nay? Vì sao phần còn lại không nộp trực tuyến?
- Người dân hay vướng ở bước nào — có số liệu không?
- Người không dùng được thiết bị số thì được hỗ trợ thế nào?

**Về vận hành**
- Mỗi ngày bao nhiêu hồ sơ, cao điểm khi nào?
- Cán bộ xử lý dùng mấy hệ thống, có phải nhập lại dữ liệu không?
- Báo cáo nào phải nộp cấp trên, tần suất, lấy số liệu từ đâu?
