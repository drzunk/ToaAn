# EOD, batch và đối soát

Mục lục: [Chu kỳ ngày](#chu-kỳ-một-ngày-làm-việc) · [Test batch](#test-batch) · [Giả lập thời gian](#giả-lập-thời-gian) · [Đối soát](#đối-soát) · [Cân sổ](#cân-sổ-gl) · [Báo cáo](#báo-cáo)

## Chu kỳ một ngày làm việc

```
SOD (mở ngày) → Giao dịch trong ngày → Cut-off → EOD (khóa ngày)
                                                    ├─ Tính lãi, thu phí định kỳ
                                                    ├─ Hạch toán giao dịch chờ
                                                    ├─ Sinh file đối soát
                                                    ├─ Sinh báo cáo, sao kê
                                                    └─ Cân sổ cái
```

Mỗi bước là một điểm hỏng. Test EOD thường bị bỏ vì khó dựng môi trường, nhưng lỗi EOD là
loại lỗi phát hiện muộn nhất và tốn kém nhất để sửa.

## Test batch

**Nhóm case bắt buộc cho mọi job batch:**

| Nhóm | Case |
|---|---|
| Đúng đắn | Chạy với dữ liệu chuẩn → kết quả khớp tính toán độc lập |
| Rỗng | Ngày không có giao dịch nào → job vẫn chạy xong, không lỗi, sinh file rỗng hợp lệ |
| Lớn | Khối lượng bằng ngày cao điểm → chạy xong trong cửa sổ thời gian cho phép |
| Chạy lại | Chạy lại job đã chạy thành công → **không** nhân đôi bút toán |
| Dừng giữa chừng | Kill job ở giữa rồi chạy lại → dữ liệu không ở trạng thái nửa vời |
| Dữ liệu bẩn | Giao dịch treo, bản ghi thiếu trường, số tiền bất thường → job ghi nhận lỗi và tiếp tục, không chết cả batch |
| Thứ tự | Job B phụ thuộc job A → chạy B trước A phải bị chặn |
| Đồng thời | Hai tiến trình cùng chạy một job → chỉ một được chạy (khóa) |

Case "chạy lại" là quan trọng nhất. Trong vận hành thật, batch **sẽ** phải chạy lại sau sự
cố. Nếu job không idempotent, lần chạy lại sẽ tính lãi hai lần cho toàn bộ khách hàng.

```python
def test_batch_tinh_lai_chay_lai_khong_nhan_doi():
    snapshot_truoc = tong_lai_da_nhap()
    run_batch("interest_accrual", business_date="2026-03-31")
    sau_lan_1 = tong_lai_da_nhap()

    run_batch("interest_accrual", business_date="2026-03-31")   # chay lai
    sau_lan_2 = tong_lai_da_nhap()

    assert sau_lan_2 == sau_lan_1, "Batch tinh lai khong idempotent"
    assert sau_lan_1 > snapshot_truoc
```

**Case quanh cut-off:**
- Giao dịch đúng 1 giây trước và sau cut-off → rơi vào ngày nào.
- Giao dịch khởi tạo trước cut-off nhưng duyệt sau cut-off.
- Giao dịch đang xử lý dở khi EOD bắt đầu → phải bị chặn hoặc xếp sang ngày mới, không được
  hạch toán vào ngày đang khóa.
- Kênh số vẫn nhận lệnh trong lúc EOD chạy → lệnh vào hàng chờ, khách nhận thông báo phù hợp.

## Giả lập thời gian

Không đổi giờ máy chạy test. Các cách đúng, theo thứ tự ưu tiên:

1. **Business date override ở hệ thống** — hầu hết core banking có khái niệm "ngày làm việc
   hệ thống" tách rời giờ máy. Đây là cách chuẩn.
2. **Trigger batch thủ công với tham số ngày** — chạy job EOD cho ngày chỉ định.
3. **Test clock ở phía đối tác** — các PSP hiện đại cung cấp cơ chế tua thời gian phía server
   cho môi trường sandbox, dùng để test chu kỳ thanh toán định kỳ nhiều tháng mà không phải
   chờ. Giả lập đồng hồ trong tiến trình test không có tác dụng, vì việc tính toán chạy trên
   máy chủ của đối tác.
4. **Container có giờ riêng** — chỉ dùng khi ba cách trên không khả thi, và phải cô lập hoàn
   toàn, vì đổi giờ làm hỏng chứng chỉ TLS và làm các dịch vụ khác cư xử kỳ lạ.

**Các mốc thời gian đáng test:** cuối tháng, cuối quý, cuối năm, 29/2 năm nhuận, ngày chuyển
từ 31 sang 1, và chuỗi ngày nghỉ Tết Nguyên đán (nhiều ngày nghỉ liên tiếp làm lộ bug tính
ngày làm việc kế tiếp).

## Đối soát

Đối soát là việc so khớp dữ liệu giữa hai hệ thống độc lập. Nếu QA không test phần này, sai
lệch sẽ được phát hiện bởi kế toán hoặc khách hàng.

**Các cặp đối soát điển hình:**
- Hệ thống kênh ↔ core banking
- Ngân hàng ↔ NAPAS (file đối soát cuối ngày)
- Ngân hàng ↔ tổ chức thẻ
- Ngân hàng ↔ đối tác thanh toán hóa đơn
- Sổ cái ↔ sổ chi tiết tài khoản

**Case cần có:**

| Kịch bản | Kỳ vọng |
|---|---|
| Hai bên khớp hoàn toàn | Báo cáo đối soát sạch, không có mục chênh |
| Có ở ta, không có ở đối tác | Đánh dấu chênh lệch, đưa vào danh sách tra soát, **không** tự xóa |
| Có ở đối tác, không có ở ta | Đánh dấu, cảnh báo — đây có thể là giao dịch ta chưa ghi nhận |
| Khớp giao dịch nhưng lệch số tiền | Đánh dấu rõ chênh bao nhiêu |
| Trùng bản ghi trong file đối tác | Chỉ đối chiếu một lần, ghi nhận bản trùng |
| File đối soát đến muộn / không đến | Job phải chờ hoặc cảnh báo, không âm thầm coi là không có giao dịch |
| File sai định dạng, thiếu dòng tổng | Từ chối xử lý, cảnh báo |
| Giao dịch xuyên cut-off của hai bên | Xử lý theo quy ước đã thống nhất, thường là đối chiếu bù sang ngày sau |

**Kiểm tra dòng tổng (control total)**: hầu hết file đối soát có dòng tổng số bản ghi và tổng
số tiền. Test phải khẳng định tổng tính từ các dòng chi tiết bằng đúng dòng tổng. File mà
tổng không khớp phải bị từ chối ngay, không xử lý từng phần.

```python
def test_file_doi_soat_khop_dong_tong(file_path):
    rows, footer = parse_recon_file(file_path)
    assert len(rows) == footer.record_count, (
        f"So dong {len(rows)} khac dong tong {footer.record_count}"
    )
    assert sum(r.amount for r in rows) == footer.total_amount
```

## Cân sổ GL

Phép kiểm cuối cùng của một ngày. Chạy sau EOD:

```sql
-- 1. Toan bo but toan trong ngay phai can
SELECT business_date, SUM(debit) - SUM(credit) AS lech
FROM journal
WHERE business_date = :d
GROUP BY business_date
HAVING SUM(debit) <> SUM(credit);
-- Ky vong: khong tra ve dong nao

-- 2. So du cuoi ky = so du dau ky + phat sinh
SELECT a.account_no
FROM account_balance a
JOIN (SELECT account_no, SUM(credit) - SUM(debit) AS movement
      FROM journal WHERE business_date = :d GROUP BY account_no) m
  ON m.account_no = a.account_no
WHERE a.closing_balance <> a.opening_balance + m.movement;
-- Ky vong: khong tra ve dong nao

-- 3. Khong co tai khoan khach hang bi am ngoai han muc thau chi
SELECT account_no, closing_balance FROM account_balance
WHERE closing_balance < 0 AND overdraft_limit = 0;
```

Ba truy vấn này nên chạy như một test tự động sau mỗi lần EOD ở môi trường test, và lý tưởng
là cả trên môi trường thật như một kiểm soát vận hành.

## Báo cáo

- **Sao kê khách hàng**: số dư đầu kỳ + phát sinh = số dư cuối kỳ. Kiểm tra cả bản PDF/Excel
  xuất ra, không chỉ dữ liệu API — lỗi định dạng số và ngày rất hay xảy ra ở bước xuất file.
- **Sao kê theo khoảng ngày tùy chọn**, gồm khoảng rỗng và khoảng rất dài.
- **Báo cáo gửi cơ quan quản lý**: đúng mẫu, đúng hạn, đúng cách làm tròn. Sai mẫu là vấn đề
  tuân thủ chứ không chỉ là bug.
- **Số liệu giữa các báo cáo phải nhất quán**: tổng giao dịch trên dashboard, trên báo cáo
  ngày, và trên file đối soát phải bằng nhau. Lệch giữa các báo cáo là dấu hiệu có đường dữ
  liệu bị bỏ sót.
