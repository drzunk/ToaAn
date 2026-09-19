# Tích hợp nền tảng số quốc gia

> Lĩnh vực này thay đổi nhanh. Mọi thông tin dưới đây cần đối chiếu với tài liệu kỹ thuật
> hiện hành của đơn vị chủ quản nền tảng trước khi đưa vào đặc tả.

Mục lục: [Bản đồ nền tảng](#bản-đồ-các-nền-tảng-dùng-chung) · [Định danh điện tử](#định-danh-và-xác-thực-điện-tử) · [Dữ liệu dân cư](#khai-thác-dữ-liệu-dân-cư) · [Nền tảng chia sẻ dữ liệu](#nền-tảng-tích-hợp-chia-sẻ-dữ-liệu) · [Thanh toán](#thanh-toán-trực-tuyến-nghĩa-vụ-tài-chính) · [Chữ ký số](#chữ-ký-số-và-giá-trị-pháp-lý) · [Quy trình xin kết nối](#quy-trình-xin-kết-nối)

## Bản đồ các nền tảng dùng chung

```
┌─────────────────────────────────────────────────────────┐
│  Cổng Dịch vụ công quốc gia                             │
│  (điểm vào tập trung cho người dân, doanh nghiệp)       │
└────────────────┬────────────────────────────────────────┘
                 │
     ┌───────────┼────────────┬──────────────┬─────────────┐
     ▼           ▼            ▼              ▼             ▼
┌─────────┐ ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌──────────┐
│Định danh│ │CSDLQG   │ │Nền tảng  │ │Thanh toán  │ │Chữ ký số │
│điện tử  │ │dân cư   │ │chia sẻ   │ │trực tuyến  │ │công cộng │
│(VNeID)  │ │         │ │dữ liệu   │ │            │ │          │
└─────────┘ └─────────┘ └──────────┘ └────────────┘ └──────────┘
                 │              │
                 ▼              ▼
      ┌────────────────────────────────────────┐
      │ Hệ thống thông tin giải quyết TTHC      │
      │ cấp bộ / cấp tỉnh                       │
      └────────────────────────────────────────┘
                 │
                 ▼
      ┌────────────────────────────────────────┐
      │ CSDL chuyên ngành (đất đai, hộ tịch,   │
      │ bảo hiểm, thuế, doanh nghiệp…)         │
      └────────────────────────────────────────┘
```

Với mỗi mũi tên, BA phải xác định: cần kết nối không, xin kết nối ở đâu, mất bao lâu, dữ
liệu nào được phép lấy, và cơ sở pháp lý là gì.

## Định danh và xác thực điện tử

Nền tảng định danh quốc gia là ứng dụng VNeID, xây dựng trên cơ sở dữ liệu về định danh, dân
cư và xác thực điện tử, thuộc Đề án 06 (Quyết định số 06/QĐ-TTg ngày 06/01/2022). Nghị định
59/2022/NĐ-CP quy định về định danh và xác thực điện tử.

**Thay đổi quan trọng với BA:** Quyết định 29/2026/QĐ-TTg (ban hành 04/6/2026) thống nhất
việc truy cập Cổng Dịch vụ công quốc gia bằng tài khoản định danh điện tử **từ ngày
20/7/2026**. Nghĩa là các phương thức đăng nhập khác không còn được dùng cho cổng này.

**Hệ quả thiết kế phải xử lý:**
- Bỏ các phương thức đăng nhập cũ, di chuyển tài khoản hiện có sang tài khoản định danh
- Người dân chưa có tài khoản định danh mức phù hợp thì đi đường nào? Phải có hướng dẫn
  trong luồng, không chỉ báo lỗi
- Dữ liệu hồ sơ cũ gắn với tài khoản cũ được liên kết sang tài khoản mới thế nào
- Tổ chức, doanh nghiệp dùng định danh tổ chức — luồng khác cá nhân
- Người nước ngoài, người chưa đủ điều kiện có tài khoản định danh

**Mức độ định danh** ảnh hưởng tới quyền thực hiện: nhiều thủ tục yêu cầu mức cao hơn. BA
phải lập bảng: thủ tục nào cần mức nào, và hành vi hệ thống khi người dùng chưa đạt mức.

**Với ngân hàng:** nhiều ngân hàng đã dùng VNeID trong mở tài khoản và xác thực giao dịch.
Khi đặc tả, cần làm rõ dùng ở bước nào (đối chiếu thông tin, xác thực người dùng, hay cả
hai), và luồng dự phòng khi không kết nối được.

## Khai thác dữ liệu dân cư

Nguyên tắc cốt lõi của cải cách thủ tục hành chính: **giấy tờ nào cơ quan nhà nước đã có
trong cơ sở dữ liệu thì không bắt người dân nộp lại.**

**Việc của BA:** rà từng thành phần hồ sơ trong quyết định công bố thủ tục, đối chiếu với dữ
liệu có thể khai thác, và lập bảng:

| Thành phần hồ sơ | Nguồn dữ liệu thay thế | Khai thác được? | Nếu không khai thác được |
|---|---|---|---|
| Bản sao giấy tờ tùy thân | CSDLQG về dân cư | Có | Cho tải lên bản chụp, cán bộ đối chiếu |
| Giấy xác nhận cư trú | CSDLQG về dân cư | Có | Như trên |
| Giấy tờ chứng minh quan hệ nhân thân | CSDL hộ tịch | Tùy địa phương | Nộp bản chụp |
| Giấy tờ chuyên ngành | CSDL chuyên ngành tương ứng | Cần khảo sát từng trường hợp | Nộp bản chụp |

**Ba điều luôn phải đặc tả kèm:**
1. **Sự đồng ý của người dân** cho việc tra cứu dữ liệu của họ — ghi nhận kèm thời điểm và
   phạm vi.
2. **Luồng dự phòng** khi không tra cứu được. Không bao giờ để người dân mắc kẹt vì hệ thống
   bên kia lỗi.
3. **Xử lý khi dữ liệu không khớp** với thông tin người dân khai. Không tự động từ chối —
   sai lệch dấu tiếng Việt, tên viết tắt, địa chỉ theo đơn vị hành chính cũ đều là nguyên
   nhân phổ biến và chính đáng. Chuyển cán bộ xem xét.

## Nền tảng tích hợp chia sẻ dữ liệu

Việc kết nối giữa các hệ thống của cơ quan nhà nước thường đi qua nền tảng tích hợp chia sẻ
dữ liệu (cấp quốc gia và cấp bộ/tỉnh) thay vì kết nối trực tiếp từng cặp.

**Điều BA cần nắm:**
- Kết nối qua nền tảng nghĩa là có thêm một lớp trung gian: ảnh hưởng độ trễ, cách xử lý
  lỗi, và cách truy vết khi sự cố (lỗi ở ta, ở nền tảng, hay ở bên cung cấp?).
- Mỗi dịch vụ chia sẻ có tài liệu kỹ thuật riêng do đơn vị chủ quản dữ liệu ban hành. Lấy
  đúng phiên bản.
- Thủ tục xin kết nối mất thời gian — xem phần cuối.
- Có giới hạn tần suất truy vấn. Thiết kế phải tính tới việc dùng bộ nhớ đệm hợp lý cho dữ
  liệu ít thay đổi, nhưng **không lưu trữ lại dữ liệu cá nhân vượt quá mục đích và thời hạn
  cho phép**.

## Thanh toán trực tuyến nghĩa vụ tài chính

Thu phí, lệ phí và các nghĩa vụ tài chính khác gắn với hồ sơ thủ tục hành chính.

**Luồng chuẩn:**
```
Xác định nghĩa vụ (theo thủ tục và trường hợp cụ thể) →
Sinh mã thanh toán gắn với hồ sơ →
Người dân thanh toán qua kênh được kết nối →
Nhận xác nhận đã thu →
Cập nhật trạng thái hồ sơ →
Đối soát với kho bạc/ngân hàng
```

**Câu hỏi BA phải làm rõ:**
- Nghĩa vụ tài chính tính theo quy tắc nào, có trường hợp miễn giảm không, ai xác định?
- Thanh toán là điều kiện để tiếp nhận hồ sơ hay để trả kết quả? Hai cách khác nhau hoàn
  toàn về luồng.
- Thanh toán rồi nhưng hồ sơ bị từ chối thì hoàn tiền thế nào, ai quyết, bao lâu?
- Thanh toán trùng xử lý ra sao?
- Người dân nộp tiền mặt tại quầy thì ghi nhận vào hệ thống ở bước nào?
- Đối soát với kho bạc theo chu kỳ nào, ai xử lý chênh lệch?
- Chứng từ nộp tiền ở dạng điện tử có giá trị pháp lý gì, lưu bao lâu?

Phần đối soát ở đây tương tự đối soát ngân hàng — xem `payments-domain.md`.

## Chữ ký số và giá trị pháp lý

Luật Giao dịch điện tử 2023 có hiệu lực từ 01/7/2024, quy định về chữ ký điện tử, chữ ký số,
hợp đồng điện tử, chứng thư điện tử và giá trị pháp lý của chúng.

**Câu hỏi BA phải trả lời cho mỗi loại chứng từ trong hệ thống:**
- Chứng từ này cần ký ở mức nào — chữ ký số, chữ ký điện tử chuyên dùng, hay chỉ cần xác
  nhận điện tử?
- Ai ký: người dân, cán bộ, hay con dấu của cơ quan?
- Người dân không có chữ ký số cá nhân thì dùng hình thức nào thay thế? (Cần kiểm tra quy
  định hiện hành cho từng loại thủ tục — một số trường hợp đã được đơn giản hóa.)
- Kết quả trả về được ký số bởi ai, kiểm tra tính toàn vẹn thế nào?
- Chữ ký còn hiệu lực bao lâu sau khi chứng thư hết hạn? Cần đóng dấu thời gian không?
- Lưu trữ chứng từ đã ký: định dạng gì, kiểm tra lại chữ ký sau nhiều năm có được không?

Câu hỏi cuối cùng hay bị bỏ qua và gây rắc rối lớn khi cần trích xuất hồ sơ cũ để giải quyết
khiếu nại.

## Quy trình xin kết nối

Đây là rủi ro tiến độ lớn nhất của dự án dịch vụ công, và hoàn toàn dự đoán được.

**Các bước điển hình:**
```
1. Xác định dịch vụ cần kết nối và cơ sở pháp lý
2. Gửi văn bản đề nghị tới đơn vị chủ quản dữ liệu
3. Được cấp tài liệu kỹ thuật và tài khoản môi trường thử nghiệm
4. Phát triển và thử nghiệm
5. Kiểm thử kết nối chính thức, có biên bản
6. Đánh giá an toàn thông tin
7. Cấp tài khoản môi trường chính thức, ký thỏa thuận chia sẻ dữ liệu
8. Vận hành
```

**Việc BA nên làm ngay tuần đầu dự án:**
- Liệt kê toàn bộ kết nối cần có
- Với mỗi kết nối: đơn vị chủ quản là ai, thủ tục xin thế nào, thời gian dự kiến, ai trong
  dự án chịu trách nhiệm theo đuổi
- Đưa các mốc này vào kế hoạch dự án như **đường găng**, không phải việc phụ
- Chuẩn bị phương án tạm khi chưa có kết nối: mô phỏng để phát triển song song, và luồng
  nghiệp vụ dự phòng để có thể lên chạy thật dù kết nối chậm

Một dự án đợi kết nối mới bắt đầu phát triển là một dự án đã trễ từ đầu. Thiết kế sao cho
phần lớn công việc làm được với dịch vụ mô phỏng, rồi thay bằng kết nối thật khi sẵn sàng.
