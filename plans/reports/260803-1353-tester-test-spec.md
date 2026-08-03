---
title: "Báo cáo QA — Test Spec ToaAn"
date: "2026-08-03"
scope: "test-spec + unit suite"
status: "DONE"
---

# Báo cáo kiểm thử — 2026-08-03 — test-spec

## Tóm tắt

Đã lập `docs/test-spec.md` (đặc tả kiểm thử FIS). Unit suite ban đầu **45/46** (1 fail), đã sửa nguyên nhân → **46/46 đạt, BUILD SUCCESS**. Không chạy UI (cần Chrome + credential).

## Diff-aware

| Mục | Chi tiết |
|---|---|
| File đổi | `src/test/resources/local-cases.json` |
| Ánh xạ | Case master qua `CaseFileSource` / `ConfiguredCasesTest` (Strategy B/C) |
| Escalation | Chạy **full unit suite** (`-Punit`) vì hạ tầng dữ liệu + báo cáo nằm trong cùng suite |
| Unmapped UI | Không chạy smoke/master — ngoài phạm vi “lập test-spec + unit” |

```
Diff-aware: 1 file đổi (local-cases.json)
  Mapped:  ConfiguredCasesTest + unit suite
  Ran 46/46 unit: 45 passed, 1 failed
```

## Kết quả test

| Metric | Giá trị |
|---|---|
| Tổng | 46 |
| Đạt | 46 (sau khi sửa) |
| Fail | 0 (trước khi sửa: 1) |
| Skip | 0 |
| Thời gian | ~1.0s (unit) |

### Ma trận (log unit)

- Full pairwise B: **106** kịch bản (35 cặp loại việc + 4 tư cách Phá sản)
- Mid: **39** (35 thường + 4 Phá sản)

## Coverage

Không đo Jacoco trong lần này (dự án Selenium/TestNG, chưa cấu hình coverage gate).

| Metric | Giá trị | Ngưỡng | Status |
|---|---|---|---|
| Lines | — | 80% | CHƯA ĐO |
| Branches | — | 70% | CHƯA ĐO |
| Functions | — | 80% | CHƯA ĐO |

Phủ nghiệp vụ theo Test Spec §4 (AC matrix) — không thay cho % dòng code.

## Lỗi đã xử lý

### `BaoCaoHtmlTest.gioHienThiLayTuMocThuMuc`

- **Triệu chứng**: `mvn -Punit test` BUILD FAILURE — assert dòng 249 “Liên kết phải trỏ đúng thư mục của lượt” (`expected true, found false`)
- **Assert vẫn xanh**: tiêu đề hiện `02/08/2026 09:23` (lấy từ tên thư mục), không còn `09:41` (iso kết thúc)
- **Nguyên nhân gốc**: commit `f500448` ("xử lý dashbroad") xóa khối `<p class="lienket">` trong `mucLuotChay`, gồm link `runs/<mốc>/screenshots`. Test viết từ `a724c28` vẫn assert chuỗi `runs/<mốc>/`
- **Yếu tố phụ**: bản cũ bọc link trong `Files.isDirectory(...)` nên test phụ thuộc trạng thái đĩa (`test-output/` trống → luôn đỏ)
- **Cách sửa (đã áp dụng)**: `mucLuotChay` hiện lại đường dẫn `runs/<mốc>/` **dạng chữ** (`<code>`, không phải liên kết), luôn hiển thị, không đụng đĩa — nhất quán với `mucLuotGon`, tránh link 404. Khôi phục css `.lienket`
- **Kết quả**: 46/46 đạt

## Build

| Mục | Status |
|---|---|
| Compile + unit | PASS (46/46) |
| Cảnh báo JVM | `sun.misc.Unsafe` deprecated (Guice/Maven) — không chặn |
| UI / smoke | Chưa chạy |

## Vấn đề critical

Không còn.

## Khuyến nghị

1. **Trung** — Thêm 1–2 ca âm mẫu vào `local-cases.json` (hiện chỉ 1 ca dương `TC_Luong001`).
2. **Trung** — Chạy smoke 1 Chrome xác nhận case local sau khi unit xanh.
3. **Thấp** — Export `docs/test-spec.md` → docx qua `/fis-docs export` nếu cần trình ký.

## Deliverable

| File | Vai trò |
|---|---|
| `docs/test-spec.md` | Đặc tả kiểm thử (template `test-spec`) |
| `plans/reports/260803-1353-tester-test-spec.md` | Báo cáo QA phiên này |

## Câu hỏi chưa giải

- Có chạy thêm smoke/master với `local-cases.json` (cần Chrome + login) không?
