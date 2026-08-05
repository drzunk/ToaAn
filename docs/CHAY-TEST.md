# Cách chạy test

## Cách dễ nhất

```bat
.\scripts\chay.cmd
```

Dùng **↑ / ↓** để chọn, **Enter** để vào (hoặc bấm phím số 1–9). Chọn xong — **tự mở Chrome và chạy**.

| Số | Việc |
|----|------|
| **1** | **Chạy theo Google Sheet** — danh sách case lấy từ sheet (chỉ hỏi số Chrome) |
| 2 | Cấu hình case tại chỗ — loại đơn / loại việc / CN\|TC / dừng bước (1–3 Chrome) |
| 3 | Smoke nhanh — 1 Chrome |
| 4 | Smoke nhanh — 3 Chrome |
| 5 | Chỉ đăng nhập |
| 6 | Mid regression — 3 Chrome |
| 7 | Full coverage — 3 Chrome (lâu) |
| 8 | Xem cấu hình (không chạy) |
| **9** | **Mở Dashboard web** (`http://localhost:8787`) — báo cáo / Test case / **Sinh test case theo màn** / locator |

### Menu 1 — chạy theo Google Sheet

Mỗi dòng trên sheet là một case. Cột:

```
Chạy | Loại đơn | Loại việc | Chủ thể | Tư cách | Tòa án | Số bị đơn
     | Đồng NĐ | Đại diện | Liên quan | TL bổ sung | Đến bước | Gửi đơn | Ghi chú
```

- Cột **Chạy**: gõ `x` cho dòng muốn chạy — **để trống là bỏ qua dòng đó**.
- Ô trống ở các cột nhánh = để automation tự chọn.
- **Đến bước** `0`–`6` (`6` = điền đủ tới màn Xem lại). Chỉ khi `Đến bước = 6` thì cột
  **Gửi đơn** = `x` mới bấm Gửi đơn thật.
- Sheet phải chia sẻ **"Bất kỳ ai có đường liên kết — Người xem"**.

Link sheet nằm ở khoá `run.casesSheet` trong `src/test/resources/run-flow.properties`.
Số case trên sheet có thể nhiều hơn số Chrome — các case còn lại xếp hàng.

### Menu 2 — ví dụ

- Case 1: Dân sự → Hợp đồng → Cá nhân → dừng bước 3  
- Case 2: Hành chính → Quyết định → Tổ chức → điền đủ + gửi đơn  

→ ghi `run.cases=...`, mở đúng số Chrome đã chọn.

### Menu 9 — Dashboard web + Sinh test case

1. `.\scripts\chay.cmd` → chọn **9**.
2. Tab **Sinh test case** → «Sinh / làm mới đề xuất» (hoặc tự tải lần đầu vào tab).
3. Chọn case theo từng màn (Login → Dashboard → Bước 1…6) → **Thêm case đã chọn vào danh sách**.
4. Tab **Test case** → **Lưu tất cả xuống file** → **Chạy**.

Đề xuất lấy từ `master-data.properties` + whitelist ca âm. Nếu đã chạy `mvn -Pdiscovery test`,
thông báo mong đợi được gắn từ CSV mới nhất trong `test-output/discovery-sweep/`.

---

## File cấu hình (nếu sửa tay)

`src/test/resources/run-flow.properties`

Rồi: `.\scripts\run-flow.cmd`

---

## Lần đầu

1. Copy `config.example.properties` → `config.properties`
2. Điền username / password / baseUrl
3. `.\scripts\chay.cmd`

Báo cáo: `test-output/index.html` (tự mở sau khi chạy xong)
