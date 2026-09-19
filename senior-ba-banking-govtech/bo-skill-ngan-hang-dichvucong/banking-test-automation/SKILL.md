---
name: banking-test-automation
description: >-
  Bộ skill toàn diện để test hệ thống ngân hàng và trung gian thanh toán: core banking,
  Internet/Mobile Banking, chuyển tiền NAPAS 247/VietQR, thẻ ATM/POS/3DS, message ISO 8583
  và ISO 20022/SWIFT, luồng maker-checker, EOD batch và đối soát, xác thực sinh trắc học
  theo Thông tư 50/2024/TT-NHNN, test data giả lập an toàn và tuân thủ Luật Bảo vệ dữ liệu
  cá nhân. Bao trùm cả test case thủ công lẫn automation (Playwright, Selenium, Appium,
  RestAssured/pytest, k6) và CI/CD.
  HÃY DÙNG SKILL NÀY khi người dùng nhắc tới: test ngân hàng, core banking, T24, Flexcube,
  internet/mobile banking, chuyển khoản, NAPAS, VietQR, CITAD, thẻ tín dụng, ATM, POS, 3DS,
  OTP, sinh trắc học, hạn mức, maker-checker, đối soát, sao kê, bút toán, GL, EOD, ISO 8583,
  ISO 20022, pacs.008, MT103, KYC, AML, PCI DSS, hoặc bất kỳ test case/automation nào liên
  quan tới tiền và tài khoản — kể cả khi họ không nói chữ "ngân hàng".
  Không dùng cho: test web/app thường không dính tiền, hoặc pentest bảo mật thực thụ.
license: MIT
metadata:
  version: "1.0"
  language: vi
  jurisdiction: VN
---

# Banking Test Automation

## Mục tiêu

Test ngân hàng hỏng theo những cách mà test web thông thường không bao giờ chạm tới: một
giao dịch timeout ở tầng mạng nhưng đã ghi nợ tài khoản, một webhook đối tác retry làm ghi
có hai lần, một batch cuối ngày chạy đúng 23:59:58 rơi vào sai ngày hạch toán, một khoản
phí làm lệch bút toán 1 đồng và không ai phát hiện cho tới lúc đối soát, một lệnh chuyển
tiền 10.000.001đ đi qua mà không hỏi sinh trắc học.

Skill này tồn tại để bắt đúng những lỗi đó. Nguyên tắc xuyên suốt: **tiền phải cân, mọi
thao tác phải để lại dấu vết, và không một byte dữ liệu thật nào được rời khỏi production.**

## Bảng tra nhanh

| Bạn đang cần test… | Đọc file |
|---|---|
| Hiểu mô hình nghiệp vụ, bút toán kép, vòng đời giao dịch | `references/domain-model.md` |
| Sinh số tài khoản/CIF/CCCD/thẻ hợp lệ, che PII, quản lý test data | `references/test-data.md` |
| Chuyển tiền nội bộ, NAPAS 247, VietQR, idempotency, hoàn/hủy lệnh | `references/payments-transfer.md` |
| Kiểm message ISO 8583 (thẻ) và ISO 20022 / SWIFT MT-MX | `references/iso-messages.md` |
| Thẻ, ATM/POS, authorization–clearing–settlement, 3DS/SCA, chargeback | `references/cards-3ds.md` |
| Đăng nhập, OTP, sinh trắc học, device binding, hạn mức theo TT50/QĐ2345 | `references/auth-security.md` |
| Luồng maker–checker, phân quyền, hạn mức duyệt, audit trail | `references/maker-checker-rbac.md` |
| EOD/SOD, cut-off, batch, đối soát NAPAS, cân GL | `references/eod-reconciliation.md` |
| Hiệu năng TPS, chịu tải giờ cao điểm, chaos, DR, giả lập đối tác | `references/nonfunctional.md` |
| Pháp lý VN, PCI DSS, KYC/AML, bảo vệ dữ liệu cá nhân | `references/compliance-vn.md` |
| Chọn framework, locator, chống flaky, CI/CD, môi trường test | `references/automation-stack.md` |
| Viết test case/test plan/báo cáo theo chuẩn đưa được cho QA lead và auditor | `references/test-artifacts.md` |

## Câu hỏi khai thác trước khi viết bất cứ dòng test nào

Đừng đoán. Hệ thống ngân hàng khác nhau rất xa giữa các bank, và đoán sai ở đây tốn cả
sprint. Hỏi (hoặc tự tìm trong tài liệu dự án) những điều sau:

**Phạm vi và kiến trúc**
- Đang test tầng nào: kênh (web/mobile), middleware/API gateway, hay core banking? Test ở
  kênh mà kỳ vọng core xử lý đúng là nguồn gốc của phần lớn test sai kỳ vọng.
- Core là gì (T24, Flexcube, Symbols, core tự phát triển)? Có gọi được API core ở môi
  trường test không, hay phải verify qua DB/báo cáo?
- Có đối tác bên ngoài nào trong luồng (NAPAS, CITAD, tổ chức thẻ, eKYC, SMS/OTP provider)?
  Sandbox thật hay phải mock? Xem `references/nonfunctional.md` phần giả lập.

**Nghiệp vụ**
- Loại giao dịch nào: chuyển tiền, thanh toán hóa đơn, mở tài khoản, tiền gửi, tín dụng,
  thẻ? Mỗi loại có vòng đời trạng thái riêng.
- Có phí, thuế, tỉ giá, lãi không? Làm tròn theo quy tắc nào và ở bước nào?
- Giao dịch có qua phê duyệt (maker–checker) không? Mấy cấp?
- Cut-off time và lịch ngày làm việc (T+0/T+1) ra sao?

**Dữ liệu và môi trường**
- Test data đến từ đâu? Nếu câu trả lời là "copy từ production" thì dừng lại — đọc
  `references/test-data.md` và `references/compliance-vn.md` trước khi làm tiếp.
- Môi trường test có reset định kỳ không? Số dư tài khoản test có bị người khác tiêu mất
  giữa chừng không? (Đây là nguyên nhân flaky số 1 trong test ngân hàng.)

## Nguyên tắc cốt lõi

**1. Không bao giờ dùng dữ liệu thật.** Không số thẻ thật, không CCCD thật, không sao chép
dữ liệu khách hàng từ production sang test. Mã hóa hay che bớt không giải quyết được vấn
đề — chỉ có không mang sang mới giải quyết. Đây không phải là sở thích mà là yêu cầu pháp
lý (Điều 19 Thông tư 50/2024/TT-NHNN, Luật Bảo vệ dữ liệu cá nhân 2025) và là điều kiện để
repo của bạn không rơi vào phạm vi PCI DSS.

**2. Test phải kết thúc bằng một phép cân tiền, không phải bằng một dòng chữ trên màn hình.**
"Chuyển khoản thành công" hiện trên UI không chứng minh gì cả. Một test chuyển tiền chỉ
được tính là pass khi: số dư nguồn giảm đúng số tiền + phí, số dư đích tăng đúng số tiền,
tổng nợ bằng tổng có trong bút toán, và bản ghi giao dịch tồn tại với đúng trạng thái. Xem
`references/domain-model.md`.

**3. Mọi giao dịch đều sẽ được gửi lại ít nhất một lần.** Mạng timeout, đối tác retry, người
dùng bấm nút hai lần, batch chạy lại sau sự cố. Idempotency không phải tính năng nâng cao
mà là yêu cầu cơ bản, và phải được test tường minh: gửi lại cùng một request ID phải trả về
cùng kết quả và **không** tạo thêm bút toán.

**4. Trạng thái "không rõ" nguy hiểm hơn trạng thái "thất bại".** Lỗi tệ nhất trong thanh
toán không phải giao dịch fail, mà là giao dịch mà hệ thống không biết nó thành công hay
chưa — đã trừ tiền khách nhưng chưa nhận được phản hồi từ đối tác. Mọi luồng thanh toán
phải có test case cho timeout giữa chừng, và phải chứng minh được cơ chế tra soát/hoàn tiền
tự động hoạt động. Xem `references/payments-transfer.md`.

**5. Thời gian là dữ liệu nghiệp vụ, không phải chi tiết kỹ thuật.** Cut-off, ngày hiệu lực
(value date), ngày làm việc, múi giờ, giao dịch lúc 23:59 và 00:01 — tất cả đều phải có test
case riêng. Giả lập thời gian phải làm ở phía server, đổi giờ máy chạy test không có tác
dụng gì.

**6. Audit trail là chức năng, hãy test nó như chức năng.** Ai làm, làm lúc nào, từ IP/thiết
bị nào, giá trị trước và sau. Nếu một thao tác thay đổi tiền hoặc quyền mà không sinh log
không thể sửa được, đó là một bug nghiêm trọng, không phải một thiếu sót nhỏ.

**7. Đường xấu quan trọng hơn đường đẹp.** Ở ngân hàng, tỷ lệ bug nằm ở nhánh phụ cao hơn
nhánh chính rất nhiều: số dư không đủ, tài khoản đóng/phong tỏa, vượt hạn mức, sai người
thụ hưởng, OTP hết hạn, thẻ hết hạn, đối tác trả về mã lỗi lạ. Viết negative case trước,
happy path sau.

**8. Không tự ý chạy test lên môi trường có dữ liệu thật.** Kể cả khi có quyền. Nếu không
chắc môi trường đang trỏ vào đâu, dừng lại và hỏi.

## Quy trình làm việc

Khi nhận một yêu cầu test, đi theo thứ tự này:

1. **Xác định loại nghiệp vụ và vòng đời trạng thái.** Vẽ ra các trạng thái giao dịch và
   các chuyển đổi hợp lệ trước khi viết case. `references/domain-model.md` có các vòng đời
   chuẩn cho chuyển tiền, thẻ, mở tài khoản.
2. **Liệt kê rủi ro theo mức độ thiệt hại tiền.** Ưu tiên case có thể gây mất tiền, ghi có
   trùng, hoặc lệch sổ — trước case giao diện.
3. **Dựng test data an toàn.** Mỗi test tự tạo dữ liệu của mình, không dùng chung tài khoản
   với test khác. `references/test-data.md`.
4. **Viết case theo bộ khung 6 nhóm** (dùng cho mọi nghiệp vụ liên quan tiền):
   - Happy path đủ biến thể (nội bộ/liên ngân hàng, có phí/không phí)
   - Biên số tiền (0, 1đ, dưới/trên ngưỡng xác thực, bằng đúng số dư, bằng đúng hạn mức)
   - Trạng thái tài khoản bất thường (đóng, phong tỏa, ngủ đông, sai loại tiền tệ)
   - Xác thực và phân quyền (OTP sai/hết hạn, sinh trắc học, vượt quyền, maker tự duyệt)
   - Lỗi hệ thống và đối tác (timeout, đối tác trả lỗi, mất kết nối giữa chừng, retry)
   - Đối soát và hậu kiểm (bút toán cân, báo cáo khớp, file đối soát khớp)
5. **Tự động hóa phần lặp lại, giữ thủ công phần cần mắt người.** Automation mạnh ở API và
   kiểm tra số dư; yếu ở luồng có eKYC quay video, OTP qua SIM thật, hay thiết bị phần cứng.
6. **Chốt bằng verification.** Mỗi case phải nói rõ verify ở đâu: UI, API, DB, file đối
   soát, hay log. Case chỉ verify UI trong nghiệp vụ tiền là case yếu.

## Anti-pattern hay gặp

- **Verify bằng screenshot màn hình thành công.** Không chứng minh tiền đã chuyển. Luôn
  kiểm tra số dư và bút toán.
- **Dùng chung một tài khoản test cho cả suite.** Số dư thay đổi giữa các test, gây flaky
  không thể tái hiện. Mỗi test tự mở/nạp tài khoản riêng.
- **Hard-code số tài khoản, OTP, token trong code test.** Vừa gây lộ thông tin vừa hỏng khi
  môi trường reset.
- **Chỉ test với số tiền tròn (100.000đ).** Bug làm tròn và bug lệch 1 đồng chỉ lộ ra với
  số lẻ (123.457đ) và với phép chia phí/lãi/tỉ giá.
- **Bỏ qua trường hợp giao dịch trùng.** Không có test retry nghĩa là chưa test idempotency.
- **Assert cứng vào thông điệp lỗi tiếng Việt.** Bản dịch đổi là suite đỏ. Assert vào mã lỗi.
- **Coi đối soát là việc của kế toán.** Nếu QA không test đối soát, sai lệch sẽ được phát
  hiện bởi khách hàng.

## Hoàn thành khi

- Mọi nghiệp vụ liên quan tiền đều có case cân bút toán, không chỉ case UI.
- Có case timeout/retry và chứng minh được tính idempotency.
- Có case biên cho ngưỡng xác thực và hạn mức theo quy định hiện hành.
- Không có dữ liệu thật hay khóa bí mật thật nào trong repo test (đã grep kiểm tra).
- Test data tự sinh, tự dọn, chạy lại được nhiều lần cho cùng kết quả.
- Có truy vết từ yêu cầu nghiệp vụ → test case → kết quả chạy, đủ để đưa cho kiểm toán nội bộ.

## Tài liệu tham khảo trong skill

Đọc file tương ứng trong `references/` khi vào việc cụ thể — đừng nạp hết cùng lúc. Bảng tra
nhanh ở đầu file này chỉ ra file cần đọc cho từng tình huống.
