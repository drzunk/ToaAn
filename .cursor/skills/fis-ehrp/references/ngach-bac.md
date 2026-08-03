# ngach-bac

Hệ thống ngạch / bậc cho công chức và viên chức tại Việt Nam.

## Ngạch (rank category)

- Chuyên viên cao cấp (A3) — bậc 1-6, hệ số 6.20-8.00
- Chuyên viên chính (A2) — bậc 1-8, hệ số 4.40-6.78
- Chuyên viên (A1) — bậc 1-9, hệ số 2.34-4.98
- Cán sự (B) — bậc 1-12
- Nhân viên (C) — bậc 1-12

Reference: NĐ 204/2004/NĐ-CP Phụ lục 1.

## Bậc (step within rank)

- Mỗi ngạch có N bậc; mỗi bậc có hệ số lương riêng.
- Nâng bậc thường xuyên: 3 năm (A0/A1) hoặc 2 năm (B/C) nếu hoàn thành nhiệm vụ.
- Nâng bậc trước hạn: thành tích đặc biệt, max 12 tháng.

## Chuyển ngạch

- Yêu cầu thi/xét chuyển ngạch (Sở Nội vụ tổ chức).
- Hồ sơ: bằng cấp, chứng chỉ, đánh giá 2 năm gần nhất.

## Anti-patterns

- Hardcode hệ số trong code → mỗi lần NĐ điều chỉnh phải sửa code.
- Không version-control bảng ngạch/bậc → mất audit trail khi cải cách.
