# luong-payroll

Công thức lương cơ bản công chức / viên chức.

## Salary base formula

```
Lương = (mức lương cơ sở) × (hệ số lương) × (1 + Σ phụ cấp %)
```

- Mức lương cơ sở 2024: 2.340.000 VND/tháng (NĐ 73/2024/NĐ-CP).
- Hệ số lương: per ngạch × bậc (NĐ 204/2004 Phụ lục).

## Phụ cấp common

| Phụ cấp | % | Cơ sở |
|---|---|---|
| Chức vụ lãnh đạo | 0.1-1.30 | NĐ 204 |
| Khu vực | 0.1-1.0 | NĐ 76/2019 |
| Công vụ | 25% | NĐ 17/2025 |
| Thâm niên vượt khung | 5% (+1%/year) | NĐ 204 |
| Trách nhiệm | 0.1-0.5 | per task |
| Độc hại | 0.1-0.4 | per occupation |

## Khấu trừ

- BHXH: 8% × (lương + phụ cấp), max base 20× mức lương cơ sở.
- BHYT: 1.5%.
- BHTN: 1%.
- Thuế TNCN: lũy tiến 5%-35% (Luật TNCN).
- Đoàn phí công đoàn: 1% lương ngạch bậc + chức vụ.

## Anti-patterns

- Tính TNCN trên gross thay vì net of insurance → sai lệch.
- Quên cap BHXH ở 20× mức lương cơ sở → over-deduct.
- Không tách phụ cấp ngoài lương vs phụ cấp trong lương → BHXH base sai.
