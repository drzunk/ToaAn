# Maker–checker, phân quyền và audit trail

Mục lục: [Nguyên lý](#nguyên-lý-tách-bạch-nhiệm-vụ) · [Luồng phê duyệt](#luồng-phê-duyệt) · [Ma trận phân quyền](#ma-trận-phân-quyền) · [Hạn mức duyệt](#hạn-mức-theo-cấp-duyệt) · [Audit trail](#audit-trail)

## Nguyên lý tách bạch nhiệm vụ

Người tạo lệnh và người duyệt lệnh phải là hai người khác nhau. Nghe hiển nhiên, nhưng cách
nó bị phá vỡ trong thực tế rất tinh vi, và đó chính là chỗ cần test:

- Maker tự duyệt lệnh của mình (kiểm tra ở cả UI và API).
- Maker có hai tài khoản, dùng tài khoản thứ hai để duyệt.
- Maker được cấp tạm quyền checker rồi duyệt lệnh cũ do chính mình tạo.
- Checker sửa nội dung lệnh rồi duyệt luôn — biến mình thành maker mà không qua kiểm soát.
- Lệnh đã bị từ chối được sửa lại và duyệt mà không tạo lệnh mới.
- Ủy quyền khi vắng mặt (delegation) bị lạm dụng để tự duyệt.

Mỗi dòng trên là một test case. Kiểm tra ở tầng API là bắt buộc, vì giao diện thường ẩn nút
duyệt còn backend vẫn chấp nhận lời gọi.

```python
def test_maker_khong_tu_duyet_duoc(api, maker):
    txn = api.create_transfer(as_user=maker, amount=50_000_000)
    r = api.approve(txn.id, as_user=maker, raw=True)
    assert r.status_code == 403
    assert api.get(txn.id).status == "PENDING_APPROVAL"
    assert count_journal_entries(txn.id) == 0   # chua sinh but toan
```

## Luồng phê duyệt

```
TẠO (maker) → CHỜ DUYỆT ──┬─→ DUYỆT (checker) → THỰC HIỆN → HOÀN TẤT
                          ├─→ TỪ CHỐI (checker) → KẾT THÚC
                          └─→ THU HỒI (maker)   → KẾT THÚC
```

**Case cho từng nhánh:**

| Tình huống | Kỳ vọng |
|---|---|
| Duyệt đủ số cấp yêu cầu | Lệnh thực hiện, bút toán sinh ra đúng lúc duyệt cuối cùng, không phải lúc tạo |
| Chưa đủ cấp duyệt | Chưa có bút toán nào, tiền chưa bị giữ hoặc chỉ bị giữ tùy đặc tả |
| Checker từ chối | Lệnh kết thúc, mọi khoản giữ được nhả, lý do từ chối lưu lại |
| Maker thu hồi lệnh chờ duyệt | Thành công; sau khi đã duyệt thì không thu hồi được nữa |
| Số dư đủ lúc tạo, không đủ lúc duyệt | Phải thất bại ở bước thực hiện với thông báo rõ, không được ghi âm tài khoản |
| Tài khoản bị phong tỏa giữa lúc tạo và duyệt | Từ chối thực hiện |
| Lệnh chờ duyệt qua đêm | Qua cut-off thì ngày hiệu lực là ngày nào; lệnh hết hạn sau bao lâu |
| Hai checker duyệt đồng thời | Chỉ tính một lần duyệt, không thực hiện lệnh hai lần |

Case cuối cùng là case đua điều kiện, hay bị bỏ qua và hay gây ra chuyển tiền hai lần trong
thực tế. Bắn hai lời gọi duyệt song song và khẳng định chỉ có một bộ bút toán.

## Ma trận phân quyền

Xây một bảng vai trò × chức năng, rồi test **toàn bộ ô**, không chỉ đường chéo:

| Chức năng | Giao dịch viên | Kiểm soát viên | Quản lý chi nhánh | Vận hành CNTT |
|---|---|---|---|---|
| Tạo lệnh chuyển tiền | ✓ | ✓ | ✓ | ✗ |
| Duyệt lệnh | ✗ | ✓ | ✓ | ✗ |
| Xem hồ sơ khách hàng | ✓ (trong chi nhánh) | ✓ (trong chi nhánh) | ✓ | ✗ |
| Sửa thông tin khách hàng | ✓ (chờ duyệt) | ✓ | ✓ | ✗ |
| Mở/khóa tài khoản | ✗ | ✓ | ✓ | ✗ |
| Xem log hệ thống | ✗ | ✗ | ✗ | ✓ |
| Cấp quyền người dùng | ✗ | ✗ | ✓ (chờ duyệt) | ✗ |

Sinh test tự động từ ma trận thay vì viết tay từng case:

```python
@pytest.mark.parametrize("role,action,allowed", flatten(PERMISSION_MATRIX))
def test_phan_quyen(api, role, action, allowed):
    user = users[role]
    r = api.call(action, as_user=user, raw=True)
    if allowed:
        assert r.status_code < 400, f"{role} phai duoc phep {action}"
    else:
        assert r.status_code == 403, f"{role} KHONG duoc phep {action}"
```

Cách này bắt được đúng loại lỗi hay xảy ra sau khi refactor: thêm endpoint mới mà quên gắn
kiểm tra quyền.

**Các chiều phân quyền khác cần kiểm tra:**
- **Theo đơn vị**: nhân viên chi nhánh A không xem được khách hàng chi nhánh B.
- **Theo dữ liệu**: xem được danh sách nhưng không xem được chi tiết tài khoản VIP.
- **Theo thời gian**: quyền tạm thời phải hết hiệu lực đúng hạn.
- **Theo kênh**: quyền ở quầy khác quyền trên kênh số.

## Hạn mức theo cấp duyệt

Số tiền càng lớn, cấp duyệt càng cao. Test tại các biên:

```
< 100 triệu        → 1 cấp duyệt
100 triệu – 1 tỷ   → 2 cấp duyệt
> 1 tỷ             → 3 cấp duyệt, trong đó có cấp quản lý
```

- Đúng 100.000.000 thuộc bậc nào — hỏi rõ BA, biên đóng hay mở.
- Lệnh 99.999.999 và 100.000.001 → khác số cấp duyệt.
- Chia nhỏ lệnh để né cấp duyệt (structuring): 10 lệnh 99 triệu trong một ngày → hệ thống có
  cảnh báo không? Đây vừa là yêu cầu kiểm soát nội bộ vừa là yêu cầu phòng chống rửa tiền.
- Sửa số tiền lệnh sau khi đã có 1 cấp duyệt → phải hủy các duyệt trước đó và làm lại.
- Cấp duyệt thứ 2 phải khác người với cấp thứ 1.

## Audit trail

Ở ngân hàng, log không phải để debug mà là bằng chứng. Test nó như một chức năng.

**Mỗi bản ghi phải có:**
- Ai (định danh người dùng, không phải tên hiển thị)
- Lúc nào (dấu thời gian có múi giờ, đến mili giây)
- Từ đâu (IP, kênh, định danh thiết bị)
- Làm gì (mã hành động, không phải câu mô tả tự do)
- Trên đối tượng nào (định danh bản ghi)
- Giá trị trước và sau (với thao tác sửa)
- Kết quả (thành công/thất bại — **thất bại cũng phải ghi**)

**Test case:**
- Thực hiện mỗi thao tác nhạy cảm rồi khẳng định có đúng một bản ghi log với đủ trường.
- Thao tác **thất bại** cũng phải sinh log — đăng nhập sai, duyệt vượt quyền, truy cập bị từ
  chối. Chỉ ghi log thành công là lỗ hổng điều tra.
- Log không được chứa dữ liệu nhạy cảm dạng rõ: mật khẩu, OTP, PAN đầy đủ, dữ liệu sinh trắc.
  Viết một test quét log sau khi chạy suite và fail nếu tìm thấy mẫu nhạy cảm.
- Không sửa/xóa được log qua bất kỳ API nào, kể cả với vai trò quản trị.
- Thời gian lưu trữ log đúng chính sách.
- Truy vấn log theo người dùng, theo đối tượng, theo khoảng thời gian — chức năng tra cứu
  phải dùng được thật, vì khi có sự cố mới là lúc cần.

```python
SENSITIVE_PATTERNS = [
    (r'"password"\s*:\s*"(?!\*+")', "mat khau dang ro"),
    (r'\b\d{13,19}\b', "co the la so the day du"),
    (r'"otp"\s*:\s*"\d{4,8}"', "OTP dang ro"),
]

def test_log_khong_chua_du_lieu_nhay_cam(log_content):
    for pattern, mo_ta in SENSITIVE_PATTERNS:
        found = re.findall(pattern, log_content)
        assert not found, f"Log chua {mo_ta}: {len(found)} vi tri"
```
