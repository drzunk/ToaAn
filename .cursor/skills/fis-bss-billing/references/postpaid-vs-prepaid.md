# Postpaid vs prepaid — choosing the model

## Postpaid

Customer uses the service first, pays at end of cycle (monthly invoice).

- **Subscriber state on network:** ACTIVE based on credit standing.
- **Credit risk:** carrier; mitigated by deposit / spend cap / credit check.
- **Core BSS components:** rating engine, billing engine, dunning.
- **Customer profile:** mid-to-high ARPU, contracted plans, often bundled (voice + SMS + data + extras).

## Prepaid

Customer pays first, uses until balance depleted, tops up to continue.

- **Subscriber state:** ACTIVE while balance > 0 and within validity period.
- **Credit risk:** none for carrier; balance acts as collateral.
- **Core BSS components:** real-time charging (OCS), top-up gateway.
- **Customer profile:** mass-market low ARPU, no contract, easy churn.

## Convergent (hybrid)

One subscriber identity carries both balance (prepaid bucket) and bill cycle (postpaid component).

- **Use case:** corporate customer with prepaid pool for international roaming + postpaid base plan.
- **Complexity:** OCS + billing engine must reconcile; account hierarchy non-trivial.
- **Worth it when:** ≥ 10% of customer base genuinely needs the hybrid.

## Decision matrix

| Criterion | Postpaid | Prepaid | Convergent |
|---|---|---|---|
| Carrier credit risk | High — needs credit check | None | Mixed |
| Activation friction | KYC + credit check | KYC + first top-up | Both |
| Real-time charging required | Yes for usage cap; No for billing | **Yes** | Yes |
| Mid-cycle plan change | Standard | Restricted | Complex |
| Roaming complexity | Standard | Geographic blocking | Complex |
| Suitable for VN mass market | Mid-high ARPU customers | **Most subscribers** | Enterprise |

## Anti-patterns

- Forcing convergent when 95% are prepaid → over-engineered.
- Prepaid without OCS → balance leakage; charging happens after the fact.
- Postpaid with weak dunning → bad debt mounts.
