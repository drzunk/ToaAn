# Cách chạy test

## Cách dễ nhất

```bat
.\scripts\chay.cmd
```

Dùng **↑ / ↓** để chọn, **Enter** để vào (hoặc bấm phím số 1–9). Chọn xong — **tự mở Chrome và chạy**.

| Số | Việc |
|----|------|
| **1** | **Cấu hình case** — loại đơn / loại việc / CN\|TC / dừng bước (1–3 Chrome) |
| 2 | Smoke nhanh — 1 Chrome |
| 3 | Smoke nhanh — 3 Chrome |
| 4 | Chỉ đăng nhập |
| 5 | Mid regression — 3 Chrome |
| 6 | Full coverage — 3 Chrome (lâu) |
| V | Xem cấu hình (không chạy) |

### Menu 1 — ví dụ

- Chrome 1: Dân sự → Hợp đồng → Cá nhân → dừng bước 3  
- Chrome 2: Hành chính → Quyết định → Tổ chức → điền đủ + gửi đơn  

→ ghi `run.cases=...`, mở đúng số Chrome đã chọn.

---

## File cấu hình (nếu sửa tay)

`src/test/resources/run-flow.properties`

Rồi: `.\scripts\run-flow.cmd`

---

## Lần đầu

1. Copy `config.example.properties` → `config.properties`
2. Điền username / password / baseUrl
3. `.\scripts\chay.cmd`

Báo cáo: `test-output/ExtentReport.html`
