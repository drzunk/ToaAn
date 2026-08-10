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
| `truongLoi` | Trường lỗi | **Negative** — whitelist `DataGenerator.TRUONG_LOI_HOP_LE` |
| `giaTriLoi` | Giá trị lỗi | Giá trị ép vào field (rỗng = để trống) |
| `thongBaoMongDoi` | Thông báo mong đợi | Chuỗi con trong message chặn; rỗng = mọi thông báo |
| `untilStep` | Đến bước | `0` = chỉ login; `1`…`6` = dừng sau bước đó |
| `submit` | Gửi đơn | Chỉ có hiệu lực khi `untilStep >= 6` |

Validate nghiêm lúc **Lưu** / nạp file (`CaseFileSource`) — sai catalog hoặc sai tên trường lỗi → không lưu.

## 2. `untilStep` và `submit`

- Wizard không nhảy bước: case bước N vẫn login → điền hợp lệ 1…N−1 → dừng ở N.
- `untilStep = 6` + `submit = false` → dừng Xem lại (an toàn).
- `untilStep = 6` + `submit = true` → bấm Gửi đơn (tạo đơn trên UAT).
- **Negative** thường đặt `untilStep` = bước chứa field; `submit = false`.

## 3. Case từ `TestCaseGenerator`

- API: `GET /api/generate-cases` (tab **1. Chọn case** — filter UI: **Negative / Positive / All**).
- `ghiChu` dạng `GEN_Login_Smoke`, `GEN_B2_CaNhan`, `GEN_AM_B2_…`.
- `untilStep` theo màn đề xuất; Negative mặc định `chay = false` (unchecked by default — safe).
- Merge: tick → **Thêm vào danh sách** (bỏ trùng `ghiChu`) → **Lưu tất cả** → **Chạy case đã lưu**.
- Nếu có CSV `test-output/discovery-sweep/field-discovery_*.csv`, generator gắn `thongBaoMongDoi` khi discovery từng thấy hệ thống chặn.

### Field Negative lấy từ đâu (`FieldCoverageCatalog`)

Generator không còn giữ danh sách field cứng trong từng màn. Field Negative (= ca âm kỹ thuật) là giao của ba nguồn:

1. whitelist `DataGenerator.TRUONG_LOI_HOP_LE` — tên field hợp lệ của schema `CaseRow`;
2. `DataGenerator.tryFieldOverride` — framework thật sự ép được giá trị lỗi vào kịch bản (ép không được thì field bị bỏ, nêu trong `boQua`);
3. CSV discovery mới nhất nếu có — chỉ để gắn `thongBaoMongDoi` và đánh dấu field từng bị chặn, **không** thu hẹp danh sách.

Mỗi field gắn một biến thể form để case sinh ra đúng ngữ cảnh:

| Bước | Biến thể | Field đặc trưng |
| --- | --- | --- |
| 2 | `B2_CA_NHAN` | CCCD, Họ tên, Ngày sinh, Ngày cấp, Giới tính, SĐT, Email, Địa chỉ thường trú |
| 2 | `B2_TO_CHUC` | Mã số thuế |
| 3 | `B3_CA_NHAN` | CCCD/Họ tên/Nghề nghiệp/Nơi ở hiện tại (Bị đơn) |
| 3 | `B3_TO_CHUC_PHA_SAN` | Mã số thuế (Bị đơn) — luồng Phá sản là chỗ UI bảo đảm bị đơn là tổ chức |
| 4 | `B4_TEXTAREA` | Thời điểm phát sinh, Giá trị tranh chấp, Tóm tắt quá trình, Yêu cầu cụ thể, Căn cứ pháp lý |
| 4 | `B4_EFORM` | (trống) — eform có schema động trong iframe, không gắn override Java |

Vì vậy Negative bước 4 chỉ sinh cho loại việc dùng textarea; loại việc eform chỉ có Positive.

`GET /api/generate-cases` trả thêm `fieldCoverage` (`tongFieldUngVien`, `fieldDaCoCaAm`, `phanTramPhu`, `fieldDiscoveryDaThay`, `fieldChuaPhu`); tab Chọn case hiện dòng `phủ field Negative: x/y (z%)`. Muốn có `thongBaoMongDoi` thật thì chạy `mvn -Pdiscovery test` trước khi Sinh.

### Login suite (màn Đăng nhập trên tab Chọn case)

- **1 Positive** (`GEN_Login_Smoke`, `untilStep=0`): có `CaseRow`, thêm vào `local-cases` và chạy qua master như case wizard.
- **3 Negative login** (sai mật khẩu, sai captcha, bỏ trống mật khẩu): `engine=login` trên API — **không** có `CaseRow`, **không** dùng `truongLoi`; hiển thị trên Dashboard để tra cứu, chạy bằng **«Chạy Login suite»** → `POST /api/run-login` → `mvn -Plogin test` (`LoginTest`).

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

PASS Negative khi `StepBlockedException` và (nếu có) message chứa `thongBaoMongDoi`. Không bị chặn → FAIL (nghi lỗ hổng validation).

> Thuật ngữ UI Dashboard: **Positive** / **Negative** / **All**. Trong JSON/API vẫn dùng `loai: "duong"|"am"` và field `truongLoi`.
