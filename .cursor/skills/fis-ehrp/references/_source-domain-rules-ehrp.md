# Domain Rules — EHRP (Government Electronic HR)

EHRP = hệ thống quản lý nhân sự cấp Bộ/Ngành/Tỉnh chuẩn theo Bộ Nội Vụ Việt Nam.

## Phạm vi nghiệp vụ

| Phân hệ | Mô tả |
|---|---|
| Hồ sơ cán bộ công chức | Quản lý profile theo TT 31/2014/TT-BNV |
| Lương | Tính lương theo Nghị định 204/2004, Nghị định 76/2019 |
| Bảo hiểm | BHXH, BHYT, BHTN — luật BHXH 2014 |
| Đào tạo | Quy hoạch đào tạo, kế hoạch năm |
| Đánh giá | Đánh giá thi đua, khen thưởng cuối năm |
| Quy hoạch | Quy hoạch cán bộ, luân chuyển |
| Báo cáo | Báo cáo Bộ Nội Vụ, Ủy ban tỉnh |

## Terminology FIS gov context

- **Hệ số lương (HSL)** — coefficient × mức lương cơ sở
- **Bậc lương** — pay grade theo thang lương
- **Ngạch công chức** — civil servant grade (vd Chuyên viên, Chuyên viên chính)
- **Đơn vị sự nghiệp** — public service unit (PSU)
- **Cơ quan chủ quản** — superior authority
- **Quy hoạch cán bộ** — cadre planning
- **Đảng viên** — Party member status (separate workflow)

## Validation rules đặc thù

### Hồ sơ
- Mã CBCC: prefix theo Bộ/Ngành (vd `01-` Bộ Tài chính, `04-` Bộ GD&ĐT)
- Số hiệu cán bộ: unique trong đơn vị + cấp trên
- CMND/CCCD: 9 hoặc 12 digits với checksum CCCD
- Quyết định bổ nhiệm: phải có file đính kèm (PDF scan)

### Lương
- HSL × mức lương cơ sở (Mức 2.34 cho Đại học)
- Mức lương cơ sở: Nghị định cập nhật (2024: 2,340,000 VND; 2026 dự kiến điều chỉnh)
- Phụ cấp chức vụ: % HSL theo chức danh
- Phụ cấp khu vực: ngân hàng, vùng đặc biệt
- Truy lĩnh lương: cho phép ngược về tháng cũ với chứng từ

### Đánh giá thi đua
- 4 mức: Hoàn thành xuất sắc / Hoàn thành tốt / Hoàn thành / Không hoàn thành
- Tỷ lệ hạn chế: HTXS không quá 15% đơn vị, HTT không quá 70%
- Phải có 2 cấp đánh giá: tự đánh giá + thủ trưởng

## Compliance regulatory

- TT 31/2014/TT-BNV: Hồ sơ cán bộ công chức format
- NĐ 90/2020/NĐ-CP: Đánh giá xếp loại CBCC
- NĐ 204/2004/NĐ-CP: Chế độ tiền lương
- Luật Cán bộ Công chức 2008 (sửa đổi 2019)
- Luật Viên chức 2010
- BHXH: Luật BHXH 2014, sửa đổi 2024

## Personas EHRP context

| Persona | Role |
|---|---|
| CBCC end user | Tự cập nhật hồ sơ, xem lương, đăng ký đào tạo |
| Trưởng phòng | Đánh giá nhân viên, đề xuất quy hoạch |
| Cán bộ tổ chức | Manage hồ sơ, tính lương, báo cáo |
| Lãnh đạo | Approve quyết định, xem báo cáo tổng hợp |
| IT admin | Phân quyền, sao lưu, audit log |

## Integration points

- Cổng Dịch vụ công Quốc gia (DVCQG)
- Hệ thống quản lý văn bản (VBĐT)
- BHXH Việt Nam (web service)
- Tổng cục Thống kê
- Bộ Nội Vụ Database (báo cáo định kỳ)

## Anti-patterns

- ❌ Skip ngạch/bậc validation khi nhập lương
- ❌ Hardcode mức lương cơ sở (Nghị định cập nhật hàng năm)
- ❌ Cho edit hồ sơ không có quyết định approve
- ❌ Skip audit log cho changes (gov compliance bắt buộc)

## Reference
- Bộ Nội Vụ: https://moha.gov.vn
- BHXH VN: https://baohiemxahoi.gov.vn
- Cổng DVCQG: https://dichvucong.gov.vn
