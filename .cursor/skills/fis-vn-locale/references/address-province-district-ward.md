# VN address — Province / District / Ward

## Hierarchy

```
Country (Việt Nam)
  └── Province / Centrally-governed city (Tỉnh / Thành phố trực thuộc TW) — 63 entities
        └── District (Quận / Huyện / Thị xã / Thành phố thuộc tỉnh) — ~700
              └── Ward (Phường / Xã / Thị trấn) — ~10,500
                    └── Street + house number (Số nhà + đường)
```

## Storage

Three-level FK chain:

```sql
CREATE TABLE province (
  code VARCHAR(2) PRIMARY KEY,    -- "01" = Hà Nội
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20)                -- "Tỉnh" | "Thành phố trực thuộc TW"
);

CREATE TABLE district (
  code VARCHAR(3) PRIMARY KEY,    -- "001" = Quận Ba Đình
  province_code VARCHAR(2) REFERENCES province(code),
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20)                -- "Quận" | "Huyện" | ...
);

CREATE TABLE ward (
  code VARCHAR(5) PRIMARY KEY,    -- "00001"
  district_code VARCHAR(3) REFERENCES district(code),
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20)                -- "Phường" | "Xã" | "Thị trấn"
);
```

Codes per Tổng cục Thống kê (https://danhmuchanhchinh.gso.gov.vn).

## Display

```
{house_number} {street}
{ward.type} {ward.name}, {district.type} {district.name}
{province.name}, Việt Nam
```

Example:
```
Số 12 Đường Lê Lai
Phường Bến Thành, Quận 1
Thành phố Hồ Chí Minh, Việt Nam
```

## Migration concerns

- Province / district / ward codes change occasionally (mergers, splits — e.g. NĐ 1211/2016 of UBTVQH).
- Track history: keep old codes valid; flag as `deprecated_at` when merged.
- Periodic refresh: subscribe to GSO updates; reconcile yearly.

## Anti-patterns

- Storing address as free-text only → can't query by district / province; bad for tax reports.
- Hardcoding province list → goes stale on every administrative change.
- Single string field for the entire address → impossible to validate ward exists in district.
