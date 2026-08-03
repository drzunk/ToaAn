# Domain Rules — Utility / Điện (EVN)

Khi BA viết PRD cho project ngành điện (FIS thường làm cho EVN, các Tổng công ty Điện lực miền).

## Phạm vi nghiệp vụ

| Phân hệ | Mô tả |
|---|---|
| Khách hàng (CIS — Customer Information System) | Đăng ký, thanh đổi hợp đồng |
| Đo đếm (Metering / AMI) | Smart meter, đọc chỉ số, MDM |
| Tính cước (Billing) | Tính tiền theo biểu giá, in hóa đơn |
| Thu hộ (Collection) | Thanh toán, nhắc nợ, cắt điện |
| Vận hành lưới (Grid Ops) | SCADA, OMS, GIS |
| Bảo trì (PM/CM) | Kế hoạch bảo trì, báo cáo sự cố |
| Hợp đồng mua bán điện (PPA) | EVN ↔ nhà máy điện |

## Terminology

- **Mã khách hàng (PE)** — số hợp đồng điện, định dạng `<area-code>-<sequence>`
- **Công tơ (meter)** — số seri thiết bị
- **Chỉ số kWh** — meter reading
- **Biểu giá điện** — tariff schedule (sinh hoạt bậc thang, sản xuất TOU, hành chính)
- **Hệ số công suất** — power factor (cosφ)
- **Thông tư 16/2014/TT-BCT** — quy định bán lẻ điện
- **TOU (Time of Use)** — giá theo giờ cao/thấp/bình thường
- **NM, CN, AS, HCSN** — phân loại khách hàng (Sinh hoạt, Sản xuất, Hành chính sự nghiệp, Kinh doanh)

## Biểu giá điện sinh hoạt 2024 (bậc thang)

| Bậc | kWh | VND/kWh |
|---|---|---|
| 1 | 0-50 | 1,806 |
| 2 | 51-100 | 1,866 |
| 3 | 101-200 | 2,167 |
| 4 | 201-300 | 2,729 |
| 5 | 301-400 | 3,050 |
| 6 | > 400 | 3,151 |

(Áp dụng VAT 10% trên tổng tiền điện.)

## Validation rules

### Hợp đồng
- Mã PE unique trong miền
- 1 địa điểm = 1 PE
- Hợp đồng phải link công tơ active
- Đổi chủ hợp đồng cần văn bản chuyển nhượng

### Đo đếm
- Chỉ số mới ≥ chỉ số cũ (trừ trường hợp công tơ reset)
- Sản lượng tháng = chỉ số mới - chỉ số cũ × hệ số nhân (CT)
- Detect anomaly: sản lượng > 200% bình quân 12 tháng → flag review

### Tính cước
- Bậc thang cộng dồn cho 1 hộ
- TOU áp giờ cao/thấp/bình thường (3 ca/ngày)
- VAT 10% (Thuế GTGT)
- Phí công suất phản kháng nếu cosφ < 0.85 (KH sản xuất)

### Cắt điện do nợ
- Quy định: nợ > 60 ngày sau hạn
- Phải gửi 2 thông báo (15 + 30 ngày)
- Sau khi thanh toán + phí đóng lại → đóng điện trong 24h

## Compliance regulatory

- **Thông tư 16/2014/TT-BCT** — bán lẻ điện
- **Thông tư 19/2014/TT-BCT** — đo đếm điện năng
- **Quyết định 24/2017/QĐ-TTg** — cơ chế giá bán lẻ
- **Luật Điện lực 2004** (sửa đổi 2012)

## Personas

| Persona | Role |
|---|---|
| Khách hàng | Đăng ký, xem hóa đơn, thanh toán online |
| Nhân viên kinh doanh | Lập hợp đồng, theo dõi tiến độ |
| Nhân viên đo đếm | Đọc chỉ số (manual hoặc auto AMI) |
| Nhân viên kỹ thuật | Bảo trì, xử lý sự cố |
| Nhân viên thu cước | Đối soát thanh toán, cắt điện |
| Quản lý (đội/phòng/công ty) | Báo cáo, KPI |
| Tổng công ty (NPC/CPC/SPC/HCMC PC) | Tổng hợp toàn quốc |

## Integration points

- SCADA (real-time grid monitoring)
- OMS (Outage Management System)
- GIS (network topology)
- AMI Head-End (smart meter data)
- Cổng DVCQG (e-payment gateway)
- Banking + ví điện tử (Vietcombank, Momo, ZaloPay, ViettelPay)

## Anti-patterns

- ❌ Hardcode biểu giá (cập nhật mỗi quyết định)
- ❌ Skip detect anomaly trong đo đếm
- ❌ Cắt điện không gửi đủ thông báo (vi phạm pháp luật)
- ❌ Không separate prepaid vs postpaid logic

## Reference
- EVN: https://evn.com.vn
- Bộ Công Thương — Cục Điều tiết Điện lực: https://erav.vn
- Tổng công ty Điện lực: NPC (Bắc), CPC (Trung), SPC (Nam), HCMC PC, HANI PC
