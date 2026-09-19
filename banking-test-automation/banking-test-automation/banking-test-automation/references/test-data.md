# Test data cho hệ thống ngân hàng

Mục lục: [Nguyên tắc](#nguyên-tắc-tuyệt-đối) · [Sinh dữ liệu hợp lệ](#sinh-dữ-liệu-hợp-lệ-nhưng-giả) · [Factory](#factory-pattern) · [Che dữ liệu](#che-dữ-liệu-nếu-buộc-phải-lấy-từ-production) · [Cô lập](#cô-lập-dữ-liệu-giữa-các-test) · [Rà quét](#rà-quét-repo)

## Nguyên tắc tuyệt đối

1. **Không mang dữ liệu khách hàng thật sang môi trường test.** Kể cả đã "ẩn bớt". Nếu quy
   trình hiện tại của đội là restore bản backup production xuống UAT, đó là rủi ro pháp lý
   cần nêu ra, không phải chuyện đã rồi. Xem `compliance-vn.md`.
2. **Không dùng số thẻ thật, kể cả thẻ của chính bạn.** Dùng dải thẻ test do tổ chức thẻ
   hoặc PSP công bố.
3. **Không hard-code bí mật thật** (khóa API, mật khẩu người dùng thật, token) trong repo.
4. **Dữ liệu test phải sinh được lại**: cùng seed → cùng dữ liệu, để lỗi tái hiện được.

## Sinh dữ liệu hợp lệ nhưng giả

Dữ liệu test phải **qua được validation** của hệ thống nhưng **không trỏ tới người thật**.

### Số thẻ — thuật toán Luhn

Số thẻ có chữ số kiểm tra. Sinh ngẫu nhiên sẽ bị chặn ở bước validate, nên phải sinh đúng:

```python
def luhn_check_digit(prefix: str) -> str:
    digits = [int(c) for c in prefix]
    # nhân đôi từ phải sang, bắt đầu ở vị trí ngay trước check digit
    total = 0
    for i, d in enumerate(reversed(digits)):
        if i % 2 == 0:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return str((10 - total % 10) % 10)

def make_test_pan(bin_prefix: str, length: int = 16, rng=None) -> str:
    body = "".join(str(rng.randint(0, 9)) for _ in range(length - len(bin_prefix) - 1))
    partial = bin_prefix + body
    return partial + luhn_check_digit(partial)
```

Chỉ dùng BIN test do PSP/tổ chức thẻ cấp cho môi trường sandbox, không tự chọn BIN của một
ngân hàng thật. Với Stripe sandbox, dùng thẳng dải thẻ test công bố (ví dụ `4242…4242` cho
thành công, các số khác cho từng mã từ chối cụ thể) — chi tiết ở `cards-3ds.md`.

### Số CCCD Việt Nam

12 chữ số: 3 số mã tỉnh + 1 số giới tính/thế kỷ + 2 số năm sinh + 6 số ngẫu nhiên. Với test,
**dùng dải mã tỉnh không tồn tại hoặc dải dành riêng cho test** nếu hệ thống cho phép, để
không vô tình trùng với người thật. Nếu buộc phải dùng mã tỉnh thật, ghi rõ trong tài liệu
rằng đây là dữ liệu tổng hợp và đăng ký dải số này với đội vận hành để loại khỏi mọi báo cáo.

### Số tài khoản và mã ngân hàng

- Số tài khoản nội bộ: theo quy tắc sinh của core, thường có check digit riêng. Hỏi đội core
  lấy hàm sinh, đừng tự chế.
- Mã ngân hàng: NAPAS dùng mã BIN 6 số cho mỗi ngân hàng thành viên; CITAD dùng mã ngân hàng
  8 số. Dùng đúng mã của môi trường sandbox.
- BIC/SWIFT: 8 hoặc 11 ký tự (`BANKVNVXXXX`). Với test dùng các BIC sandbox do đối tác cấp.
- IBAN (nếu có nghiệp vụ quốc tế): có check digit mod-97, phải sinh đúng mới qua validation.

### Tên, địa chỉ, số điện thoại

- Tên: dùng bộ tên tổng hợp, kèm cả trường hợp khó — tên có dấu tiếng Việt, tên dài trên 35
  ký tự (giới hạn của nhiều trường message), tên chỉ 2 ký tự.
- Số điện thoại: dùng dải dành riêng cho test của nhà mạng hoặc số nội bộ của đội, không
  bao giờ sinh ngẫu nhiên số 09x thật vì sẽ gửi SMS tới người lạ.
- Email: dùng domain bắt được như Mailpit/Mailosaur/MailSlurp để test luồng OTP email.

## Factory pattern

Mỗi test tự tạo dữ liệu của mình. Đây là cách duy nhất để test chạy song song mà không đá nhau.

```python
@dataclass
class TestAccount:
    cif: str
    account_no: str
    currency: str = "VND"
    balance: int = 0          # đơn vị đồng, số nguyên
    status: str = "ACTIVE"

class AccountFactory:
    def __init__(self, api, seed: int):
        self.api = api
        self.rng = random.Random(seed)

    def create(self, balance=0, status="ACTIVE", currency="VND") -> TestAccount:
        cif = self.api.create_customer(**synthetic_person(self.rng))
        acc = self.api.open_account(cif, currency=currency)
        if balance:
            self.api.credit_for_test(acc, balance)   # API nội bộ chỉ có ở môi trường test
        if status != "ACTIVE":
            self.api.set_status(acc, status)
        return TestAccount(cif, acc, currency, balance, status)
```

Nguyên tắc dùng factory:
- Mặc định hợp lệ, cho ghi đè từng trường. Test chỉ khai báo điều nó quan tâm:
  `factory.create(balance=0)` cho case số dư không đủ.
- Trả về đối tượng có đủ thông tin để test tự dọn dẹp.
- Số dư nạp qua API test chuyên dụng, không qua chuyển khoản từ tài khoản dùng chung.

**Fixture tĩnh** (file JSON/SQL cố định) chỉ dùng cho dữ liệu tham chiếu không đổi: danh mục
ngân hàng, biểu phí, mã lỗi, lịch nghỉ lễ. Không dùng fixture tĩnh cho tài khoản.

## Che dữ liệu nếu buộc phải lấy từ production

Trường hợp duy nhất chấp nhận được là khi cần bộ dữ liệu quy mô lớn để test hiệu năng hoặc
migration, và phải qua phê duyệt của bộ phận tuân thủ. Khi đó:

| Trường | Cách xử lý | Lý do |
|---|---|---|
| Họ tên | Thay bằng tên tổng hợp | Định danh trực tiếp |
| CCCD/CMND | Thay mới, giữ độ dài và định dạng | Định danh trực tiếp |
| Số điện thoại, email | Thay bằng dải test | Tránh gửi thông báo tới người thật |
| Số tài khoản | Thay bằng số sinh mới, **giữ nguyên ánh xạ** để quan hệ dữ liệu không vỡ | Cần nhất quán giữa các bảng |
| Số thẻ (PAN) | Thay hoàn toàn, không giữ 6 số đầu + 4 số cuối | 6+4 vẫn đủ để suy ra chủ thẻ khi ghép nguồn khác |
| Số dư, số tiền | Có thể nhiễu nhẹ nhưng phải giữ tổng để đối soát còn ý nghĩa | Giữ đặc tính phân phối |
| Sinh trắc học | **Xóa hẳn, không che** | Không có cách ẩn danh an toàn |
| Ngày sinh | Giữ năm, ngẫu nhiên ngày/tháng | Giữ phân bố độ tuổi |

Che dữ liệu phải làm **trong vùng an toàn của production rồi mới chuyển ra**, không phải
chuyển ra rồi mới che — vì lúc đã chuyển thì rò rỉ đã xảy ra.

## Cô lập dữ liệu giữa các test

Nguyên nhân flaky số một ở test ngân hàng: hai test dùng chung một tài khoản, test A tiêu
hết tiền, test B fail ngẫu nhiên.

- Mỗi test tạo tài khoản riêng, nạp đủ số dư nó cần.
- Nếu không thể tạo mới (core chậm), dùng pool tài khoản có khóa: test mượn một tài khoản
  khỏi pool, trả lại sau khi xong, và **luôn nạp lại số dư về mốc chuẩn trước khi dùng**.
- Dọn dẹp ở `finally`, không ở cuối hàm test — test fail vẫn phải dọn.
- Với DB test có thể rollback transaction sau mỗi test; nhưng cách này không dùng được khi
  giao dịch đi qua nhiều hệ thống.

## Rà quét repo

Thêm bước này vào CI, chạy trên mọi pull request:

```bash
# Số thẻ 13-19 chữ số qua được Luhn nằm trong code
grep -rEn '\b[0-9]{13,19}\b' --include='*.{js,ts,py,java,json,yaml}' . | head

# Khóa thật
grep -rEn '(sk_live_|pk_live_|-----BEGIN (RSA|PRIVATE))' .

# CCCD 12 số
grep -rEn '\b[0-9]{12}\b' --include='*.{json,csv,sql}' .
```

Tốt hơn nữa là dùng công cụ quét bí mật (gitleaks, trufflehog) chạy như một job bắt buộc.
Phát hiện sau khi đã merge thì đã phải xoay khóa và xử lý sự cố — chặn trước rẻ hơn nhiều.
