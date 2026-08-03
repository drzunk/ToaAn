# Account management

Self-care actions on the customer account (the parent of one or more services).

## Operations

| Operation | API | Pre-condition | Post-condition |
|---|---|---|---|
| View profile | GET /me | authenticated | returns name, phone, email, address |
| Update email | POST /me/email | OTP verify new email | email updated; old sessions revoked |
| Update phone (secondary contact) | POST /me/phone | OTP verify | secondary phone updated |
| Change PIN | POST /me/pin | knows old PIN OR OTP | new PIN set |
| Enable MFA | POST /me/mfa | enrolled | MFA required on subsequent login |
| Close account | POST /me/close | not within billing dispute | scheduled close in N days |

## Authentication

- Login: phone + OTP (most common at VN telcos) OR phone + password.
- Step-up auth required for: change email, change PIN, view CDR detail, change tariff.

## Anti-patterns

- Email change without OTP → account takeover via leaked email.
- Closing account without prorated refund of remaining balance → regulator complaint.
- Allowing PIN change without old PIN AND no OTP → social-engineering attack vector.
