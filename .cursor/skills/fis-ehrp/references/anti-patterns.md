# anti-patterns

Real bugs / incidents from FIS-EHRP project archive.

## 1. Hardcoded hệ số lương

**Symptom:** NĐ ban hành mới điều chỉnh hệ số → must hot-fix code.
**Root:** Hệ số lương lưu thẳng trong code Java/Stored Proc.
**Prevention:** Bảng ngạch/bậc effectivity-dated trong DB; lookup theo `effective_from / effective_to`.

## 2. Salary calc dùng gross thay vì base

**Symptom:** BHXH deduction sai 5-10%.
**Root:** Confusion giữa "lương + tất cả phụ cấp" vs "lương đóng BHXH" (chỉ một số phụ cấp tính BHXH).
**Prevention:** Trong data model, mark phụ cấp `is_bhxh_base: bool`; sum chính xác.

## 3. Quên cap salary base ở 20× mức lương cơ sở

**Symptom:** Lãnh đạo cấp cao đóng BHXH trên full lương → over-pay.
**Root:** Code sum direct, không cap.
**Prevention:** Apply cap rule centrally: `min(base, 20 * muc_luong_co_so)`.

## 4. Đánh giá không lưu workflow

**Symptom:** Cán bộ khiếu nại kết quả → không có evidence chuỗi sự kiện.
**Root:** Đánh giá lưu mỗi mức cuối, không lưu intermediate (self / tập thể / lãnh đạo).
**Prevention:** Audit trail per workflow step (timestamp, actor, decision, comment).
