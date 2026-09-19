# Test case, test plan và báo cáo

Mục lục: [Mẫu test case](#mẫu-test-case) · [Đặt tên](#quy-ước-đặt-tên) · [Kỹ thuật thiết kế](#kỹ-thuật-thiết-kế-case) · [Test plan](#mẫu-test-plan) · [Truy vết](#ma-trận-truy-vết) · [Báo cáo lỗi](#báo-cáo-lỗi) · [Báo cáo kết quả](#báo-cáo-kết-quả)

## Mẫu test case

Dùng mẫu này cho case thủ công và làm phần mô tả cho case tự động.

```
Mã case:        TRF-INT-012
Tiêu đề:        Chuyển khoản nội bộ với số tiền bằng đúng số dư khả dụng trừ phí
Nghiệp vụ:      Chuyển tiền nội bộ
Mức ưu tiên:    Cao
Tham chiếu:     BRD-2.3.1; TT50 Đ.8 (nếu có yếu tố tuân thủ)
Loại:           Chức năng — biên

Tiền điều kiện:
  - Tài khoản nguồn A: ACTIVE, số dư khả dụng 1.011.000đ, không có khoản giữ
  - Tài khoản đích B: ACTIVE, cùng ngân hàng, khác chủ
  - Biểu phí chuyển nội bộ: 11.000đ (gồm VAT)
  - Người dùng đã đăng nhập, thiết bị đã tin cậy

Các bước:
  1. Vào chức năng Chuyển tiền trong cùng ngân hàng
  2. Nhập số tài khoản B, số tiền 1.000.000
  3. Xác nhận thông tin, nhập OTP hợp lệ

Kết quả mong đợi:
  1. Giao dịch thành công, trạng thái COMPLETED
  2. Số dư A = 0đ (hoặc bằng số dư tối thiểu nếu sản phẩm yêu cầu)
  3. Số dư B tăng đúng 1.000.000đ
  4. Bút toán gồm 4 dòng: nợ A 1.011.000; có B 1.000.000; có GL phí 10.000; có GL VAT 1.000
  5. Tổng nợ = tổng có
  6. Sao kê A hiển thị đúng số tiền, phí, ngày và nội dung
  7. Thông báo gửi tới A nêu đúng số tiền và số dư còn lại

Cách kiểm chứng:
  - UI: màn hình kết quả, màn hình số dư
  - API: GET /accounts/{id}/balance
  - DB: SELECT * FROM journal WHERE txn_id = ...
  - Log: bản ghi audit có đủ trường

Hậu điều kiện / dọn dẹp:
  - Hoàn trả số dư A về mốc chuẩn, hoặc hủy tài khoản test
```

Ba phần mà test case ngân hàng hay thiếu và phải luôn có: **bút toán mong đợi**, **cách kiểm
chứng ở nhiều tầng**, và **dọn dẹp**.

## Quy ước đặt tên

```
<MODULE>-<NHÓM>-<SỐ>
TRF-INT-012   Chuyển tiền — nội bộ — case 12
TRF-NAP-007   Chuyển tiền — NAPAS 247
CRD-3DS-003   Thẻ — 3DS
AUTH-BIO-015  Xác thực — sinh trắc học
EOD-REC-004   Cuối ngày — đối soát
SEC-IDOR-021  Bảo mật — truy cập trái phép đối tượng
CMP-TT50-009  Tuân thủ — Thông tư 50
```

Tên hàm test tự động nên đọc được như một câu khẳng định, để khi CI đỏ người ta biết ngay
hỏng cái gì mà không cần mở code:

```python
def test_chuyen_tien_vuot_han_muc_ngay_bi_tu_choi_va_khong_sinh_but_toan(): ...
def test_gui_lai_cung_idem_key_khong_tao_giao_dich_moi(): ...
def test_token_khach_a_khong_xem_duoc_so_du_khach_b(): ...
```

## Kỹ thuật thiết kế case

**Phân lớp tương đương + phân tích giá trị biên** — công cụ chính cho mọi trường số tiền:

Với hạn mức chuyển tiền 0 < x ≤ 500.000.000:

| Giá trị | Lớp | Kỳ vọng |
|---|---|---|
| −1 | Không hợp lệ | Từ chối ở cả client và server |
| 0 | Biên dưới ngoài | Từ chối |
| 1 | Biên dưới trong | Chấp nhận (nếu không có mức tối thiểu) |
| 250.000.000 | Giữa | Chấp nhận |
| 500.000.000 | Biên trên trong | Chấp nhận |
| 500.000.001 | Biên trên ngoài | Từ chối |
| Số có phần thập phân với VND | Không hợp lệ | Từ chối |
| Chuỗi chữ, ký tự đặc biệt | Không hợp lệ | Từ chối, không gây lỗi hệ thống |

**Bảng quyết định** — dùng khi kết quả phụ thuộc nhiều điều kiện, ví dụ có cần sinh trắc học
hay không:

| Số tiền > 10tr | Dồn ngày > 20tr | Thiết bị mới | → Sinh trắc học |
|---|---|---|---|
| Không | Không | Không | Không |
| Có | Không | Không | Có |
| Không | Có | Không | Có |
| Không | Không | Có | Có |
| Có | Có | Có | Có |

Bảng quyết định giúp thấy ngay là cần 8 tổ hợp chứ không phải 3 case — và chính các tổ hợp
hỗn hợp là chỗ lỗi hay nằm.

**Chuyển đổi trạng thái** — dùng cho vòng đời giao dịch, thẻ, tài khoản. Vẽ ma trận
trạng thái × sự kiện và test cả các ô "không hợp lệ" để chắc chắn chúng bị chặn.

**Đoán lỗi dựa trên kinh nghiệm** — ở ngân hàng, những chỗ đáng ngờ theo thứ tự: làm tròn,
biên ngày/giờ, giao dịch đồng thời, hoàn/hủy, và bất kỳ chỗ nào có phép nhân/chia.

## Mẫu test plan

```markdown
# Test Plan — [Tên dự án/phát hành]

## 1. Phạm vi
Trong phạm vi: [module, luồng nghiệp vụ, kênh]
Ngoài phạm vi: [nêu rõ, kèm lý do và ai chịu trách nhiệm phần đó]

## 2. Cơ sở kiểm thử
BRD/SRS, đặc tả API, tài liệu tích hợp đối tác, văn bản pháp quy liên quan

## 3. Chiến lược
Tỉ trọng theo tầng, kỹ thuật thiết kế case, mức độ tự động hóa mục tiêu

## 4. Môi trường và dữ liệu
Môi trường nào, đối tác thật hay giả lập, dữ liệu lấy từ đâu, lịch reset

## 5. Rủi ro và mức ưu tiên
Bảng rủi ro: mô tả — khả năng xảy ra — mức thiệt hại (tiền/uy tín/pháp lý) — biện pháp

## 6. Tiêu chí vào / tiêu chí ra
Vào: môi trường sẵn sàng, bản build đã qua smoke, tài liệu đã chốt
Ra: 100% case ưu tiên Cao đã chạy và đạt; không còn lỗi Nghiêm trọng/Cao mở;
    cân sổ đạt; bộ case tuân thủ đạt toàn bộ

## 7. Lịch và nguồn lực

## 8. Sản phẩm bàn giao
Bộ case, kết quả chạy, báo cáo lỗi, ma trận truy vết, báo cáo tổng kết
```

Phần rủi ro là phần quan trọng nhất và hay bị viết cho có. Ở ngân hàng, hãy xếp hạng rủi ro
theo **số tiền có thể mất** và **khả năng bị cơ quan quản lý xử lý**, không theo cảm tính về
độ phức tạp kỹ thuật.

## Ma trận truy vết

Một bảng, ba chiều: yêu cầu → case → kết quả.

| Yêu cầu | Mô tả | Case | Tự động | Kết quả gần nhất |
|---|---|---|---|---|
| BRD-2.3.1 | Chuyển tiền nội bộ | TRF-INT-001…015 | 13/15 | 15/15 đạt |
| BRD-2.3.4 | Hạn mức theo ngày | TRF-LMT-001…006 | 6/6 | 6/6 đạt |
| TT50 Đ.8.6 | Sinh trắc học thiết bị mới | AUTH-BIO-007 | Có | Đạt |

Giá trị của ma trận này là trả lời được hai câu hỏi trong 10 giây: "yêu cầu này đã được test
chưa?" và "case này tồn tại vì yêu cầu nào?". Yêu cầu không có case nào là lỗ hổng phủ; case
không gắn yêu cầu nào thường là case thừa hoặc yêu cầu chưa được ghi lại.

## Báo cáo lỗi

```
Tiêu đề:  [Module] Hành vi sai — điều kiện ngắn gọn
          Ví dụ: [Chuyển tiền] Phí không được hoàn khi giao dịch bị đối tác từ chối

Mức nghiêm trọng:
  Nghiêm trọng — mất tiền, sai số dư, lộ dữ liệu, chặn toàn bộ nghiệp vụ
  Cao          — nghiệp vụ chính sai, có đường vòng nhưng tốn kém
  Trung bình   — nghiệp vụ phụ sai, có đường vòng dễ
  Thấp         — hiển thị, chính tả

Môi trường:   SIT, build 2.14.3, core T24 R22, ngày 19/09/2026 14:30 (GMT+7)
Dữ liệu:      Tài khoản test 1900xxxx (số giả), số tiền 1.000.000đ
Các bước:     [đánh số, đủ để người khác làm lại được mà không hỏi thêm]
Thực tế:      Số dư sau giao dịch = 988.989.000 (đã trừ phí 11.000)
Mong đợi:     Số dư = 989.000.000, phí phải được hoàn cùng giao dịch
Bằng chứng:   Ảnh màn hình, bản ghi request/response, txn_id, các dòng bút toán, log
Tác động:     Mỗi giao dịch bị từ chối làm khách mất 11.000đ; ước tính N giao dịch/ngày
```

Trường **tác động** là trường quyết định lỗi được sửa nhanh hay nằm trong hàng chờ. Nêu con
số tiền và số lượng khách hàng ảnh hưởng nếu có thể ước lượng.

Với lỗi liên quan tiền, luôn đính kèm `txn_id` và các dòng bút toán — đó là thứ đội phát
triển cần đầu tiên và nếu thiếu thì phiếu lỗi sẽ quay lại hỏi.

## Báo cáo kết quả

Một trang, dành cho người ra quyết định phát hành:

```markdown
# Báo cáo kết thúc kiểm thử — [Phát hành X] — [ngày]

## Khuyến nghị
Đủ điều kiện phát hành / Đủ điều kiện có điều kiện / Chưa đủ điều kiện
[Một đoạn giải thích, nêu rõ rủi ro còn lại nếu khuyến nghị phát hành]

## Số liệu
Case đã chạy: 412/415 (3 case bị chặn do sandbox đối tác bảo trì)
Đạt: 405 | Không đạt: 7
Lỗi mở: 0 Nghiêm trọng | 2 Cao | 5 Trung bình
Vùng phủ yêu cầu: 98% (2 yêu cầu chưa test — nêu tên và lý do)
Cân sổ sau EOD: Đạt | Đối soát NAPAS: Đạt
Bộ tuân thủ: 24/24 đạt

## Rủi ro còn lại
[Mỗi rủi ro: mô tả — ảnh hưởng — biện pháp giảm thiểu — ai chấp nhận rủi ro]

## Việc chưa xong
[Case bị chặn, nợ kỹ thuật của bộ test, kế hoạch xử lý]
```

Điều làm nên một báo cáo tốt không phải con số đẹp mà là **sự trung thực về những gì chưa
test được**. Người đọc cần biết rủi ro nào họ đang chấp nhận khi ký duyệt phát hành — che
giấu vùng chưa phủ là cách nhanh nhất để mất uy tín của cả đội kiểm thử.
