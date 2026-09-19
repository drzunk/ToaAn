# Tuân thủ: pháp lý Việt Nam, PCI DSS, KYC/AML

> **Lưu ý**: đây là tài liệu định hướng test, không phải tư vấn pháp lý. Văn bản pháp luật
> thay đổi thường xuyên. Luôn đối chiếu bản hiện hành trên cổng thông tin chính thức và làm
> việc với bộ phận Pháp chế/Tuân thủ của ngân hàng trước khi chốt tiêu chí nghiệm thu.

Mục lục: [Khung pháp lý VN](#khung-pháp-lý-việt-nam) · [Dữ liệu cá nhân](#bảo-vệ-dữ-liệu-cá-nhân) · [PCI DSS](#pci-dss) · [KYC/AML](#kycaml) · [Biến quy định thành test](#biến-quy-định-thành-test-case)

## Khung pháp lý Việt Nam

| Văn bản | Phạm vi | Ảnh hưởng tới test |
|---|---|---|
| Thông tư 50/2024/TT-NHNN (31/10/2024, hiệu lực 01/01/2025) | An toàn, bảo mật cho dịch vụ trực tuyến ngành ngân hàng; thay thế Thông tư 35/2016 | Ngưỡng xác thực, sinh trắc học, quản lý thiết bị, bảo mật dữ liệu khách hàng — xem `auth-security.md` |
| Quyết định 2345/QĐ-NHNN | Xác thực sinh trắc học theo ngưỡng giao dịch | Nội dung đã được đưa vào Thông tư 50 |
| Luật Bảo vệ dữ liệu cá nhân 2025 (Luật 91/2025/QH15, hiệu lực 01/01/2026) | Quyền chủ thể dữ liệu, nghĩa vụ của tổ chức xử lý dữ liệu | Test data, quyền truy cập/xóa dữ liệu, chuyển dữ liệu ra nước ngoài |
| Nghị định 356/2025/NĐ-CP (hiệu lực 01/01/2026, thay thế Nghị định 13/2023) | Hướng dẫn chi tiết Luật Bảo vệ dữ liệu cá nhân | Phân loại dữ liệu cơ bản/nhạy cảm, yêu cầu về sự đồng ý, điều kiện chuyển dữ liệu |
| PCI DSS | Bảo mật dữ liệu thẻ | Không lưu dữ liệu xác thực nhạy cảm, che PAN, phân vùng mạng |

Đối tượng áp dụng của Thông tư 50 bao gồm tổ chức tín dụng, chi nhánh ngân hàng nước ngoài,
tổ chức cung ứng dịch vụ trung gian thanh toán và công ty thông tin tín dụng — nên nếu dự án
là ví điện tử hay cổng thanh toán thì vẫn nằm trong phạm vi, không chỉ ngân hàng.

## Bảo vệ dữ liệu cá nhân

Nghị định 356/2025/NĐ-CP điều chỉnh danh mục dữ liệu cá nhân cơ bản và nhạy cảm, siết chặt
yêu cầu về sự đồng ý, làm rõ cơ chế và thời hạn thực hiện quyền của chủ thể dữ liệu, và quy
định cụ thể điều kiện chuyển dữ liệu cá nhân ra nước ngoài.

**Dịch thành test case:**

| Yêu cầu | Test |
|---|---|
| Sự đồng ý phải rõ ràng, tách bạch | Không có ô đồng ý nào được tick sẵn; từ chối đồng ý marketing không chặn được việc mở tài khoản |
| Quyền truy cập dữ liệu | Khách yêu cầu bản sao dữ liệu → hệ thống xuất được, trong thời hạn quy định, đúng phạm vi |
| Quyền rút lại đồng ý | Rút đồng ý → dừng đúng hoạt động xử lý tương ứng, và ghi nhận thời điểm rút |
| Quyền xóa | Xóa dữ liệu nhưng vẫn phải giữ bản ghi giao dịch theo luật kế toán → test ranh giới này rất kỹ, đây là mâu thuẫn thật giữa hai luật |
| Chuyển dữ liệu ra nước ngoài | Kiểm tra cấu hình lưu trữ và dịch vụ bên thứ ba; log/analytics gửi ra nước ngoài là điểm hay bị bỏ sót |
| Dữ liệu nhạy cảm (sinh trắc học, tài chính) | Mã hóa khi lưu, phân quyền chặt, mỗi lần truy cập có giám sát |
| Thông báo sự cố | Có quy trình và có kênh thông báo hàng loạt; nên diễn tập |

**Kiểm tra dòng dữ liệu ra ngoài** là case hay bị bỏ qua nhất: SDK phân tích hành vi, công cụ
theo dõi lỗi, dịch vụ bản đồ, quảng cáo — tất cả đều có thể gửi dữ liệu khách hàng ra máy chủ
nước ngoài. Test bằng cách bắt toàn bộ lưu lượng mạng của ứng dụng trong một phiên đầy đủ và
soát từng đích đến.

```python
DOMAIN_CHO_PHEP = {"api.bank.vn", "cdn.bank.vn", ...}

def test_khong_gui_du_lieu_ra_ngoai_ngoai_danh_sach(har_file):
    lac = {u.host for u in parse_har(har_file) if u.host not in DOMAIN_CHO_PHEP}
    assert not lac, f"Ung dung goi ra cac dich vu ngoai danh sach: {lac}"
```

## PCI DSS

Áp dụng khi hệ thống chạm tới dữ liệu chủ thẻ. Những điều quan trọng nhất với đội test:

**Tuyệt đối không được lưu sau khi cấp phép:** dữ liệu dải từ đầy đủ, mã xác minh thẻ
(CVV/CVC), và mã PIN. Không ở DB, không ở log, không ở file tạm, không ở bộ nhớ đệm, không ở
ảnh chụp màn hình trong báo cáo lỗi.

```sql
-- Chay sau bo test the, ky vong khong tra ve dong nao
SELECT table_name, column_name FROM information_schema.columns
WHERE column_name ~* '(cvv|cvc|cav2|cid|track[12]|pin_block)';
```

**Che PAN khi hiển thị**: tối đa 6 số đầu và 4 số cuối. Test ở mọi nơi PAN có thể xuất hiện:
màn hình, email, SMS, PDF sao kê, log ứng dụng, log truy cập web, thông điệp lỗi, và phản hồi
API. Case hay sót: PAN đầy đủ nằm trong phản hồi JSON nhưng giao diện chỉ hiện 4 số cuối —
người dùng mở công cụ phát triển là thấy hết.

**Phạm vi (scope)**: nếu môi trường test có dữ liệu thẻ thật, toàn bộ môi trường đó rơi vào
phạm vi kiểm toán PCI. Đây là lý do thực dụng, ngoài lý do pháp lý, để chỉ dùng thẻ test.

**Dấu vết kiểm toán**: mọi truy cập vào dữ liệu chủ thẻ phải được ghi lại. Test như ở phần
audit trail trong `maker-checker-rbac.md`.

## KYC/AML

**Định danh khách hàng (KYC/eKYC)** — case cần có:
- Giấy tờ hợp lệ, hết hạn, giả mạo rõ ràng, ảnh mờ, ảnh chụp màn hình giấy tờ.
- Đối chiếu khuôn mặt với ảnh giấy tờ: đúng người, khác người, ảnh in, video phát lại.
- Thông tin trên giấy tờ không khớp thông tin khách khai.
- Khách hàng dưới tuổi mở tài khoản.
- Người nước ngoài: hộ chiếu, thị thực, giấy tờ cư trú.
- Trường hợp cần nâng cấp thẩm định (khách hàng rủi ro cao).

**Phòng chống rửa tiền (AML)** — hệ thống giám sát phải sinh cảnh báo, và QA test được cơ chế
sinh cảnh báo dù không quyết định chính sách:
- Giao dịch giá trị lớn vượt ngưỡng báo cáo.
- Chia nhỏ giao dịch để né ngưỡng (nhiều lệnh sát dưới ngưỡng trong thời gian ngắn).
- Giao dịch với quốc gia trong danh sách hạn chế.
- Khách hàng trùng danh sách đen/danh sách cảnh báo — test cả trùng chính xác và trùng gần
  đúng (tên có dấu, thiếu dấu, đảo thứ tự họ tên, viết tắt).
- Mẫu hành vi bất thường: tài khoản mới mở nhận tiền lớn rồi rút hết ngay.
- **Âm tính giả và dương tính giả**: bộ dữ liệu test phải có cả hai, và phải đo được tỉ lệ.

Trùng tên trong tiếng Việt là bài toán khó riêng: "Nguyễn Văn Đức", "NGUYEN VAN DUC",
"Nguyen Van Duc" phải được nhận diện là một; ngược lại "Đức" và "Dức" là khác nhau. Dựng bộ
dữ liệu test chuyên cho việc này.

## Biến quy định thành test case

Quy trình làm việc với một văn bản pháp quy:

1. **Đọc và trích điều khoản có tính kiểm tra được.** Bỏ qua phần nguyên tắc chung, tập trung
   vào câu có con số, ngưỡng, thời hạn, hoặc động từ "phải/không được".
2. **Mỗi điều khoản → một hoặc nhiều test case**, ghi rõ số điều khoản trong mô tả case.
3. **Phân loại**: tự động hóa được (ngưỡng, che dữ liệu, chặn chức năng) hay phải kiểm tra thủ
   công/bằng tài liệu (quy trình, chứng nhận bên thứ ba, đào tạo nhân sự).
4. **Dựng ma trận truy vết**: điều khoản → case → lần chạy gần nhất → kết quả. Đây chính là
   thứ kiểm toán nội bộ và đoàn thanh tra yêu cầu, và việc có sẵn nó tiết kiệm hàng tuần.

Mẫu ma trận:

| Điều khoản | Yêu cầu tóm tắt | Case | Tự động | Lần chạy | Kết quả |
|---|---|---|---|---|---|
| TT50 Đ.19.2 | PIN/sinh trắc học lưu phải mã hóa hoặc che giấu | SEC-021 | Có | 2026-09-15 | Đạt |
| TT50 Đ.8.6 | Khớp sinh trắc học khi dùng Mobile Banking trên thiết bị mới | AUTH-007 | Có | 2026-09-15 | Đạt |
| PCI 3.2 | Không lưu CVV sau cấp phép | PCI-003 | Có | 2026-09-18 | Đạt |

**Nguyên tắc quan trọng**: tuân thủ là nhị phân. Một case tuân thủ "gần đạt" là không đạt.
Đừng để kết quả loại này nằm trong vùng xám của báo cáo.
