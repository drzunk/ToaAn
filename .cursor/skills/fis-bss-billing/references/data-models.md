# Data models — typical BSS schemas

Reference shapes for core BSS entities. Adapt per project.

## Subscriber

```sql
CREATE TABLE subscriber (
  id BIGINT PRIMARY KEY,
  msisdn VARCHAR(15) NOT NULL UNIQUE,
  imsi VARCHAR(15) UNIQUE,
  iccid VARCHAR(20),
  state VARCHAR(20) NOT NULL,         -- NEW / ACTIVE / SUSPENDED / TERMINATED
  account_id BIGINT REFERENCES account(id),
  plan_id BIGINT REFERENCES plan(id),
  activation_date TIMESTAMPTZ,
  termination_date TIMESTAMPTZ,
  kyc_id BIGINT REFERENCES kyc(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ON subscriber (account_id);
CREATE INDEX ON subscriber (state) WHERE state IN ('ACTIVE', 'SUSPENDED');
```

## Account (parent of N subscribers)

```sql
CREATE TABLE account (
  id BIGINT PRIMARY KEY,
  customer_id BIGINT REFERENCES customer(id),
  type VARCHAR(20),                   -- POSTPAID / PREPAID / CONVERGENT
  currency VARCHAR(3) DEFAULT 'VND',
  balance_vnd BIGINT DEFAULT 0,       -- prepaid balance; 0 for postpaid
  credit_limit_vnd BIGINT,            -- postpaid; NULL for prepaid
  billing_cycle_anchor SMALLINT,      -- 1-28
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

## Plan

```sql
CREATE TABLE plan (
  id BIGINT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,   -- e.g. F89, F189
  name VARCHAR(255),
  type VARCHAR(20),                   -- POSTPAID / PREPAID / CONVERGENT
  monthly_fee_vnd BIGINT,
  voice_minutes INT,                  -- bundled
  sms_count INT,
  data_mb INT,
  fup_throttle_after_mb INT,
  effective_from DATE,
  effective_to DATE
);
```

## Rated event

```sql
CREATE TABLE rated_event (
  id BIGINT PRIMARY KEY,
  cdr_id VARCHAR(64) NOT NULL UNIQUE,  -- dedup key
  subscriber_id BIGINT REFERENCES subscriber(id),
  service VARCHAR(20),                  -- VOICE / SMS / DATA
  start_time TIMESTAMPTZ,
  end_time TIMESTAMPTZ,
  duration_sec INT,
  volume_bytes BIGINT,
  amount_vnd BIGINT,
  tax_amount_vnd BIGINT,
  source_partner VARCHAR(20),           -- network element / partner
  rate_plan_id BIGINT REFERENCES plan(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
)
PARTITION BY RANGE (start_time);
```

## Invoice

```sql
CREATE TABLE invoice (
  id BIGINT PRIMARY KEY,
  invoice_number VARCHAR(50) NOT NULL UNIQUE,  -- regulator-formatted
  account_id BIGINT REFERENCES account(id),
  cycle_start DATE,
  cycle_end DATE,
  issue_date DATE,
  due_date DATE,
  subtotal_vnd BIGINT,
  tax_vnd BIGINT,
  total_vnd BIGINT,
  status VARCHAR(20),                  -- DRAFT / ISSUED / PAID / OVERDUE / WRITTEN_OFF
  einvoice_status VARCHAR(20),         -- PENDING / ACCEPTED / REJECTED
  einvoice_xml_path TEXT
);
```

## Anti-patterns

- Storing `amount_vnd` as DECIMAL/FLOAT → rounding drift.
- No partitioning on `rated_event.start_time` → query stalls on year-2 data.
- `invoice_number` as auto-increment without regulator format check → audit fail.
