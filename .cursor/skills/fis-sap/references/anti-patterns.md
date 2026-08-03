# Anti-patterns at FIS-SAP

Real bugs / incidents from FIS-SAP project archive. Each entry: symptom, root cause, prevention.

## 1. BAPI without `COMMIT WORK`

**Symptom:** Custom Z-program calls BAPI in DEV → works. Goes to PROD → "data not saved".
**Root:** BAPI buffers changes; `BAPI_TRANSACTION_COMMIT` not called; user transaction's implicit commit isn't reached because Z-program ends with `COMMIT WORK` missing.
**Prevention:** lint rule — every BAPI call sequence ends with `BAPI_TRANSACTION_COMMIT EXPORTING wait = 'X'`.

## 2. IDoc partner profile created ad-hoc in PROD

**Symptom:** Partner integration works for a week, then random IDocs fail with "partner not found" after a transport.
**Root:** Partner profile (WE20) was created directly in PROD; transport from DEV → QAS overwrote / desynced the prod state.
**Prevention:** Always create partner profiles in DEV → transport. Never WE20 in PROD.

## 3. OData service exposes all CDS fields

**Symptom:** GDPR-equivalent VN Personal Data Protection Decree (NĐ 13/2023) audit flags PII leak.
**Root:** CDS view annotated `@OData.publish: true` exposes `vendor.tax_id`, `customer.cccd`, `employee.salary` without filtering.
**Prevention:** Apply `@AccessControl: #PRIVILEGED_ONLY` to PII fields; expose redacted views to OData.

## 4. T-code modification without transport

**Symptom:** Standard SAP T-code "fixed" in PROD via SCC1 hot-fix; next support pack overwrites the fix.
**Root:** No transport audit trail; SAP support pack assumed standard behavior.
**Prevention:** All T-code modifications via TR. Use enhancement points / BAdIs, not core mod.

## 5. RFC destination hardcoded

**Symptom:** Code that worked in QAS fails in PROD with "destination not found".
**Root:** ABAP code referenced RFC destination by literal name (e.g. `'PROD_PAYMENT_GW'`); transport doesn't carry destinations.
**Prevention:** Externalise destination name to a Z-table or import parameter; configure SM59 destinations per landscape.

## 6. Stock posting (MB1A) bypassing PO

**Symptom:** Audit fails: stock movement without PO reference; can't trace cost.
**Root:** User authorized `MB1A` directly; bypassed `MIGO` PO workflow.
**Prevention:** Restrict `MB1A` to physical inventory adjustments only (S_TCODE auth differentiation).

## 7. Hardcoded VAT rate

**Symptom:** Nghị quyết 43 reduced VAT 10% → 8% temporarily; custom code still posts 10%.
**Root:** VAT rate hardcoded in Z-program instead of reading config table T007A.
**Prevention:** All tax determination via standard config tables; never hardcode rate %.
