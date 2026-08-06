# Schema case cấu hình — ToaAn v1

Nguồn chạy master: `local-cases.json` (Dashboard / `CaseFileSource`) hoặc Google Sheet (`CaseSheetSource`).  
Hợp đồng Java: `CaseFileSource.CaseRow` → `RunFlowConfig.CaseProfile` → `DataGenerator.generateConfiguredCases`.

## 1. Field JSON / CaseRow ↔ cột Sheet

| JSON / CaseRow | Sheet (tên gần đúng) | Ý nghĩa |
|---|---|---|
| `chay` | Chạy (`x` = true) | `false` = không đưa vào lượt master |
| `loaiDon` | Loại đơn | Bắt buộc khi `chay` (khớp catalog) |
| `loaiViec` | Loại việc | Rỗng = tự chọn / Phá sản thường trống |
| `chuThe` | Chủ thể | CN / TC (chuẩn hoá trong config) |
| `tuCachNopDon` | Tư cách | Chỉ Phá sản |
| `toaAn` | Tòa án | Rỗng = tự chọn trong whitelist automation |
| `soLuongBiDon` | Số bị đơn | `0` = tự chọn; `1` hoặc `2` |
| `coDongNguyenDon` | Đồng NĐ | `null` = tự chọn |
| `coNguoiDaiDien` | Đại diện | `null` = tự chọn |
| `coNguoiLienQuan` | Liên quan | `null` = tự chọn |
| `coTaiLieuBoSung` | TL bổ sung | `null` = tự chọn |
| `ghiChu` | Ghi chú | Id dễ nhớ; generator dùng prefix `GEN_…` |
| `truongLoi` | Trường lỗi | Ca âm — whitelist `DataGenerator.TRUONG_LOI_HOP_LE` |
| `giaTriLoi` | Giá trị lỗi | Giá trị ép vào field (rỗng = để trống) |
| `thongBaoMongDoi` | Thông báo mong đợi | Chuỗi con trong message chặn; rỗng = mọi thông báo |
| `untilStep` | Đến bước | `0` = chỉ login; `1`…`6` = dừng sau bước đó |
| `submit` | Gửi đơn | Chỉ có hiệu lực khi `untilStep >= 6` |

Validate nghiêm lúc **Lưu** / nạp file (`CaseFileSource`) — sai catalog hoặc sai tên trường lỗi → không lưu.

## 2. `untilStep` và `submit`

- Wizard không nhảy bước: case bước N vẫn login → điền hợp lệ 1…N−1 → dừng ở N.
- `untilStep = 6` + `submit = false` → dừng Xem lại (an toàn).
- `untilStep = 6` + `submit = true` → bấm Gửi đơn (tạo đơn trên UAT).
- Ca âm thường đặt `untilStep` = bước chứa field; `submit = false`.

## 3. Case từ `TestCaseGenerator`

- API: `GET /api/generate-cases` (tab **Sinh test case**).
- `ghiChu` dạng `GEN_Login_Smoke`, `GEN_B2_CaNhan`, `GEN_AM_B2_…`.
- `untilStep` theo màn đề xuất; ca âm mặc định `chay = false` (tránh chạy ồ ạt).
- Merge: tick → **Thêm vào danh sách** (bỏ trùng `ghiChu`) → **Lưu tất cả**; hoặc **Chạy riêng màn này** (ghi đè file bằng đúng nhóm đã tick).
- Nếu có CSV `test-output/discovery-sweep/field-discovery_*.csv`, generator gắn `thongBaoMongDoi` khi discovery từng thấy hệ thống chặn.

### Ca đăng nhập (màn Login trên tab Sinh test case)

- **1 ca dương** (`GEN_Login_Smoke`, `untilStep=0`): có `CaseRow`, thêm vào `local-cases` và chạy qua master như case wizard.
- **3 ca âm** (sai mật khẩu, sai captcha, bỏ trống mật khẩu): `engine=login` trên API — **không** có `CaseRow`, **không** dùng `truongLoi`; hiển thị trên Dashboard để tra cứu, chạy bằng **«Chạy suite login»** → `POST /api/run-login` → `mvn -Plogin test` (`LoginTest`).

## 4. Ví dụ

**Positive** (đến Xem lại, không gửi):

```json
{
  "chay": true,
  "loaiDon": "Dân sự",
  "loaiViec": "Hợp đồng dân sự",
  "chuThe": "Cá nhân",
  "tuCachNopDon": "",
  "toaAn": "Tòa án nhân dân tỉnh Sơn La",
  "soLuongBiDon": 1,
  "coDongNguyenDon": false,
  "coNguoiDaiDien": false,
  "coNguoiLienQuan": false,
  "coTaiLieuBoSung": false,
  "ghiChu": "GEN_B6_XemLai",
  "truongLoi": "",
  "giaTriLoi": "",
  "thongBaoMongDoi": "",
  "untilStep": 6,
  "submit": false
}
```

**Negative** (ép SĐT nguyên đơn sai, dừng bước 2):

```json
{
  "chay": true,
  "loaiDon": "Dân sự",
  "loaiViec": "Hợp đồng dân sự",
  "chuThe": "Cá nhân",
  "tuCachNopDon": "",
  "toaAn": "Tòa án nhân dân tỉnh Sơn La",
  "soLuongBiDon": 1,
  "coDongNguyenDon": false,
  "coNguoiDaiDien": false,
  "coNguoiLienQuan": false,
  "coTaiLieuBoSung": null,
  "ghiChu": "GEN_AM_B2_So_dien_thoai",
  "truongLoi": "Số điện thoại",
  "giaTriLoi": "abc",
  "thongBaoMongDoi": "",
  "untilStep": 2,
  "submit": false
}
```

PASS ca âm khi `StepBlockedException` và (nếu có) message chứa `thongBaoMongDoi`. Không bị chặn → FAIL (nghi lỗ hổng validation).
