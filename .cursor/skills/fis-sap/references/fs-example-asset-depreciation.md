# Example FS — Tính khấu hao tài sản tự động (FI-AA)

Worked example showing the FS structure populated for a real-world use case. Use as a model when drafting your own.

## Cover

- **Project:** FIS_SAP_2026 / Customer ABC
- **Doc title:** TÀI LIỆU ĐẶC TẢ CHỨC NĂNG (FS) — Tính khấu hao tài sản tự động
- **Mã DA:** FIS-2026-001
- **Mã TL:** FS-FI-AA-001
- **Phân hệ:** FI-AA
- **Người lập:** [BA name]
- **Phiên bản:** 0.1
- **Ngày:** 2026-05-07

## §1.1 Mục đích

Tự động hoá việc tính và hạch toán khấu hao tài sản cố định hàng tháng cho 8 công ty con thuộc tập đoàn ABC, thay thế quy trình thủ công hiện tại (mỗi tháng kế toán phải chạy tay AFAB cho từng company code).

## §2.1 Yêu cầu nghiệp vụ

- Job tự động chạy ngày 25 mỗi tháng cho period đang mở.
- Output: posting depreciation document + báo cáo Excel chi tiết theo loại tài sản.
- Email báo cáo cho kế toán trưởng từng công ty.
- Dừng nếu có tài sản chưa được kích hoạt asset master, gửi cảnh báo.

## §2.2 Đối tượng sử dụng

| Đối tượng | Vai trò | Trách nhiệm |
|---|---|---|
| Kế toán viên FI-AA | Asset accountant | Kiểm tra báo cáo email; xử lý cảnh báo |
| IT operation | Job admin | Monitor scheduled job; restart on failure |

## §3.1 Thông tin chung

| Mục | Giá trị |
|---|---|
| T-code custom | ZAFAB_AUTO |
| Fiori App custom | (không) |
| Access path | SAP Easy Access → Z-Programs → ZAFAB_AUTO |
| Trigger | Scheduled (SM37 job: J_AUTO_DEPRECIATION, 25/m 02:00) |
| Phân hệ | FI-AA |

## §3.4 Luồng xử lý

1. Lấy danh sách tất cả company codes trong scope từ T001-BUKRS.
2. Cho mỗi company code:
   - Kiểm tra period đang mở (T001B).
   - Gọi BAPI `BAPI_DEPRECIATION_RUN` với company + period.
   - **[Inference]** BAPI tên đúng là `BAPI_DEPRECIATION_RUN` cho ECC; trên S/4HANA dùng class `CL_FAA_DEPR_PROCESS` — verify với khách hàng.
   - Capture return: total depreciation amount + asset count.
3. Aggregate kết quả thành Excel (ALV → save to AL11).
4. Send email qua `BCS_DOCUMENT_SEND` đến danh sách trong ZTBL_AAEMAIL.
5. Log vào ZTBL_AAJOB_LOG.

## §4.4 Test scenarios

| TC ID | Scenario | Expected |
|---|---|---|
| TC-01 | Happy path — 8 company codes, period open | Posting OK, Excel sent, log status SUCCESS |
| TC-02 | 1 công ty period đóng | Skip company, log WARNING, others continue |
| TC-03 | Tài sản chưa activate | Stop posting cho asset đó, gửi cảnh báo email IT |
| TC-04 | BAPI fail (DB lock) | Retry 3 lần, sau đó log ERROR + alarm |
| TC-05 | Authorization fail (user job thiếu quyền) | Log ERROR, không có data leak |
| TC-06 | Email vendor lỗi | Save Excel vào AL11; log WARN; tiếp tục cho công ty kế |

## §8 Phụ lục — Các điểm cần xác nhận

- [ ] **[Inference]** Trên S/4HANA Cloud Private có expose `BAPI_DEPRECIATION_RUN` không, hay phải dùng class API mới?
- [ ] **[Unverified]** Số ngày retention của `ZTBL_AAJOB_LOG` (đề xuất 365 ngày).
- [ ] **[Unverified]** Email format VN-only hay bilingual VN-EN?
- [ ] **[Inference]** Authorization `F_AVIS_BUK` cần được derived per company code cho job user.
