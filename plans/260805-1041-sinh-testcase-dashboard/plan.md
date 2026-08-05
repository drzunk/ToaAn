# Sinh test case theo màn hình trên Dashboard cục bộ

**Status:** done  
**Date:** 2026-08-05

## Đã làm

- `TestCaseGenerator` — đề xuất ~41 case / 8 màn từ catalog + ca âm field (+ CSV discovery nếu có)
- API `GET /api/generate-cases`
- Tab **Sinh test case** trên Dashboard (`case-editor/index.html`)
- Unit test `TestCaseGeneratorTest` (group `unit`)
- Menu 9 + `docs/CHAY-TEST.md` cập nhật

## Cách dùng

`.\scripts\chay.cmd` → 9 → tab Sinh test case → chọn → thêm vào Test case → Lưu → Chạy
