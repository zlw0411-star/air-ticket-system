# Passenger Regression — Latest Evidence Report

- Date: 2026-10-11 (run id `1011185019`, local)
- Target base URL: `http://127.0.0.1:8080`
- Method: PowerShell 5.1 `Invoke-WebRequest`, request bodies as UTF-8 `byte[]`
  (`Content-Type: application/json; charset=utf-8`), responses decoded with
  `UTF8.GetString(RawContentStream)` (byte-exact; no console-codepage reliance).
- Server: **not stopped or restarted**; application source **not edited**.
- Conventions confirmed live: business errors are **HTTP 200 + non-zero body `code`**
  (400/401/403); only the security layer emits HTTP 401/403 status.
- Test harness file is a copy under `%TEMP%`; no project source or seed data was modified
  except one authorized focused `UPDATE ticket_order.pay_deadline` for the scheduler test.

## Scope / isolation

- Isolated tester created for this run only: username `ptester_1011185019`, realName `测试乘客张三`.
- Demo accounts `user/User123` and `admin/Admin123` were **read-only** (login/logout); never written.
- Own test flights created via admin login and **cancelled at the end**:
  ids `104`–`110` (routes 北京→上海 / 广州→深圳 / 成都→杭州), all final status `CANCELLED` (verified in DB).
- No seed flight, seed user, or pre-existing order was modified.
- Only DB mutation: backdating `pay_deadline` of the single newly-created order id=43 for the scheduler test.

## Result summary

| Metric | Value |
|---|---|
| Named functional assertions | **50** |
| PASS | **50** |
| FAIL | **0** |

**50 / 50 PASS — 0 failures.** All steps below were actually executed against the live server.

> Harness note: two earlier drafts produced false FAILs from test-harness bugs (a PowerShell
> operator-precedence error `(BodyCode $x -eq 0)` and a stale pre-lock seat baseline for O10),
> **not app defects**. Both were corrected; the single authoritative run reported here is **50/50**.
> Details in "Limitations".

---

## 1. Public cities — exact Chinese, no `???` (C1)

`GET /api/flights/cities` → http=200, code=0, count=14.

Core cities present and byte-exact (Unicode codepoint comparison, not eyeballed):
`上海, 北京, 广州, 成都, 杭州, 深圳, 西安, 重庆`.
`missing=0`, `hasQuestionMark=False`.

**PASS** — public cities return valid Chinese with no `???`.
(Extra entries such as `测试出发城/到达城` are leftovers from prior admin flight-creation tests, not mojibake.)

## 2. Public search / filter / date / pagination / detail (C2–C6)

| Case | Request | Observed | Result |
|---|---|---|---|
| C2 | `GET /api/flights?page=1&size=3` | http=200, code=0, total=76, count=3 | PASS |
| C3 | `GET /api/flights?page=2&size=3` | page1 first id=80 ≠ page2 first id=81 | PASS |
| C4 | `GET /api/flights?departureCity=北京&page=1&size=50` | code=0, total=30, all `departureCity=北京` | PASS |
| C5 | `GET /api/flights?date=2026-10-12&page=1&size=50` | code=0, total=17, all `departureTime` start 2026-10-12 | PASS |
| C6 | `GET /api/flights/80` | code=0, id=80, flightNo=PST1011182531 | PASS |

**PASS** — query, city filter, date filter, page-2 distinctness, and detail all correct.

## 3. Login — user and admin, read-only (A1, A2)

| Case | Request | Observed |
|---|---|---|
| A1 | `POST /api/auth/login` `user/User123` | http=200, code=0, role=PASSENGER |
| A2 | `POST /api/auth/login` `admin/Admin123` | http=200, code=0, role=ADMIN |

**PASS** — both demo logins work; demo accounts left untouched.

## 4. Negative auth / registration (N1, WP2, N2, N3)

| Case | Request | Observed |
|---|---|---|
| N1 | login `user/WrongPwd999` | http=200, **code=401**, `用户名或密码错误` |
| WP2 | login `admin/WrongPwd999` | http=200, **code=401**, `用户名或密码错误` |
| N2 | register blank username/password/realName | http=200, **code=400**, `不能为空` |
| N3 | register duplicate username | http=200, **code=400**, `用户名已存在` |

**PASS** — wrong password (both demo accounts), blank registration, duplicate registration all rejected.

## 5. Isolated tester register with Chinese realName + login (R1, R2)

| Case | Observed |
|---|---|
| R1 register | http=200, code=0, id=22, realName byte-exact `测试乘客张三` (True) |
| R2 login | http=200, code=0, token issued (len 216) |

**PASS** — new Chinese passenger account created and can log in.

## 6. Profile get / update Chinese (P1, P2)

| Case | Observed |
|---|---|
| P1 `GET /api/users/profile` | code=0, realName byte-exact `测试乘客张三` (True) |
| P2 `PUT /api/users/profile` `{realName:李四, phone:13877778888, email:...}` | code=0, realName byte-exact `李四` (True), phone=13877778888 |

**PASS** — Chinese profile roundtrips exactly and update persists.

## 7. Password: wrong-old / change / restore (PW1–PW3)

| Case | Observed |
|---|---|
| PW1 wrong old password | http=200, **code=400**, `原密码不正确` |
| PW2 change `Passw0rdA1`→`Passw0rdB2` | code=0; login with new password → code=0 |
| PW3 restore `Passw0rdB2`→`Passw0rdA1` | code=0; login with original → code=0 |

**PASS** — wrong-old rejected, new works, original restored and verified.

## 8. Token / authorization boundaries (S1–S4)

| Case | Request | Observed |
|---|---|---|
| S1 | `GET /api/orders` no token | **http=401** |
| S2 | `GET /api/orders` junk token `junk.token.value` | **http=401** |
| S3 | `GET /api/flights/cities` junk token | http=200, code=0 (public still works) |
| S4 | `GET /api/admin/statistics` passenger token | **http=403** |

**PASS** — protected orders require a real token; junk token → 401; junk token does not break public
endpoints; passenger is forbidden (403) from admin endpoints.

## 9. Order lifecycle on own flights (O1–O8)

Fixture flight 104 (北京→上海, ECONOMY 500, BUSINESS 1500).

| Case | Operation | Observed | Result |
|---|---|---|---|
| O0 | admin create 7 own test flights | code=0, ids 104–110 | PASS |
| O1 | create order 1 pax ECONOMY | code=0, order `PENDING_PAYMENT`, ticket `PENDING` (orderId=36) | PASS |
| O2 | seat lock on create | economy 20→19 (−1) | PASS |
| O3 | pay order | code=0, order `PAID`, ticket `ISSUED`, paidAmount=500.00 | PASS |
| O4 | seats after pay | still 19 (not restored) | PASS |
| O5 | create then cancel unpaid | code=0, order+ticket `CANCELLED`; seats 19→18 (−1) →19 (+1) | PASS |
| O6 | `GET /api/orders` | code=0, total=2, both orders present | PASS |
| O7 | `GET /api/orders/36` | code=0, id=36, tickets=1 | PASS |
| O8 | 3 passengers | code=0, tickets=3, total=1500.00 (3×500) | PASS |

**PASS** — create/pay/ISSUED/seat-lock, unpaid-cancel with seat restore, list/detail, multi-pax totals correct.

## 10. Invalid passenger input + max-5 (N4–N6)

| Case | Request | Observed |
|---|---|---|
| N4 | blank passenger name | code=400, `乘机人姓名不能为空` |
| N5 | idCard `123` | code=400, `乘机人身份证号必须为18位有效格式` |
| N6 | 6 passengers | code=400, `单次最多购买 5 张机票` |

**PASS** — invalid name, invalid idCard, and max-5 all enforced.

## 11. Reschedule BUSINESS same-route, money + inventory (O9, O10)

| Case | Operation | Observed | Result |
|---|---|---|---|
| O9 | create+pay BUSINESS on f104 | code=0, ticket `ISSUED`, paidAmount=1500.00 (orderId=39) | PASS |
| O10 | reschedule → f105 (北京→上海, BUSINESS 1800) | code=0, ticket `RESCHEDULED`, ticket.flightId=105; f105 BUSINESS 6→5 (−1); f104 BUSINESS 4→5 (+1, releases the O9 lock); order total=1800.00, paid=1800.00 | PASS |

**PASS** — reschedule moves the ticket to a future same-route target, decrements the target inventory by 1,
restores the old flight inventory by 1, and updates money (total 1500→1800, paid 1500→1800).

## 12. Refund money + seats + policy (O11, PF4, PF3)

| Case | Scenario | Observed | Result |
|---|---|---|---|
| O11 | refund ticket 47 (departs 2027, >24h, price 1800) | code=0, ticket `REFUNDED`; f105 BUSINESS 5→6 (+1); order paidAmount 0.00; order `REFUNDED` | PASS |
| PF4 | 2–24h flight (own fM, economy 400) | code=0; refund=320 (80%), order paidAmount=80.00; economy 19→20 (+1) | PASS |
| PF3 | <2h flight (own fN, economy 300) | code=0; refund=150 (50%), order paidAmount=150.00; economy 19→20 (+1) | PASS |

**PASS** — refund amount equals the policy for each window (100% / 80% / 50%), paidAmount adjusted to the
remainder, order transitions to REFUNDED when all tickets refunded, and seats return to inventory.

## 13. Past / near-departure guards (PF1, PF2)

Own flights: f108 departed 3h ago; f109 departs in ~60 min.

| Case | Operation | Observed | Result |
|---|---|---|---|
| PF1 | book past flight (f108) | code=400, `航班已起飞，无法购票` | PASS |
| PF2 | reschedule ISSUED ticket on near flight (f109) | code=400, `起飞前 2 小时内不可改签` | PASS |

**PASS** — past-flight booking blocked; <2h reschedule denied.

## 14. Repeat-operation rejection (N7–N10)

| Case | Request | Observed |
|---|---|---|
| N7 | pay an already `PAID` order | code=400, `当前订单不可支付` |
| N8 | refund an already `REFUNDED` ticket | code=400, `当前机票不可退票` |
| N9 | cancel a `PAID` order | code=400, `仅待支付订单可取消` |
| N10 | reschedule a `REFUNDED` ticket | code=400, `当前机票不可改签` |

**PASS** — repeat pay/refund/cancel/reschedule safely rejected (no duplicate effect, no state corruption).

## 15. Cross-user order access (S5)

`GET /api/orders/36` with the demo `user` token → http=200, **code=403**, `不能操作他人订单`.

**PASS** — another user cannot read a tester's order.

## 16. Auto-expiry scheduler (SC1, SC2)

Method: created a fresh unpaid order (id=43, ticket=48) on own future flight f107; backdated **only that row's**
`pay_deadline` with
`E:\MySQL\mysql-8.0.39-winx64\bin\mysql.exe -uroot airline_ticket -e "UPDATE ticket_order SET pay_deadline = NOW() - INTERVAL 5 MINUTE WHERE id = 43;"`.
The application scheduler was allowed to run naturally; **no cancel endpoint was called**.

| poll | order status | ticket status |
|---|---|---|
| t+5s | PENDING_PAYMENT | PENDING |
| t+10s | PENDING_PAYMENT | PENDING |
| t+15s | PENDING_PAYMENT | PENDING |
| t+20s | PENDING_PAYMENT | PENDING |
| t+25s | PENDING_PAYMENT | PENDING |
| t+30s | PENDING_PAYMENT | PENDING |
| t+35s | PENDING_PAYMENT | PENDING |
| t+40s | PENDING_PAYMENT | PENDING |
| t+45s | **CANCELLED** | **CANCELLED** |

| Case | Result |
|---|---|
| SC1 order+ticket auto-CANCELLED, seats restored | **PASS** — order=43 CANCELLED, ticket=48 CANCELLED, elapsed≈45s, economy 10→9 (lock) →10 (restored) |
| SC2 `POST /api/orders/43/pay` after expiry | **PASS** — code=400, `当前订单不可支付` |

**PASS** — scheduler auto-cancel works end-to-end (fired within the 60s fixed-delay window; no explicit cancel API).

## 17. Cleanup (CL1)

- Own test flights 104–110: `PUT /api/admin/flights/{id}/status {status:"CANCELLED"}` → each code=0.
- Final DB status verified: 104–110 all `CANCELLED`.
- Test order/ticket artifacts remain in the DB (orders 36,37,39,40,41,42,43; ticket 47 REFUNDED) — identified,
  not deleted (no destructive cleanup requested). Isolated tester `ptester_1011185019` left present with original
  password restored.
- Demo seeds `user` (id=2) and `admin` (id=1) intact — both `enabled=1`, roles unchanged.

---

## Pass/fail count by feature

| Feature | Cases | Pass | Fail |
|---|---|---|---|
| Public cities exact Chinese no `???` | C1 | 1 | 0 |
| Public search/filter/date/pagination/detail | C2–C6 | 5 | 0 |
| Login user/admin (read-only) | A1–A2 | 2 | 0 |
| Wrong password (user/admin) | N1, WP2 | 2 | 0 |
| Blank / duplicate register | N2–N3 | 2 | 0 |
| Tester register/login | R1–R2 | 2 | 0 |
| Profile get/update Chinese | P1–P2 | 2 | 0 |
| Password wrong-old/change/restore | PW1–PW3 | 3 | 0 |
| Token/authorization boundaries | S1–S4 | 4 | 0 |
| Order fixture | O0 | 1 | 0 |
| Order create/pay/cancel/list/detail/3-pax | O1–O8 | 8 | 0 |
| Invalid passenger + max-5 | N4–N6 | 3 | 0 |
| Reschedule BUSINESS same-route | O9–O10 | 2 | 0 |
| Refund >24h / 2–24h / <2h | O11, PF4, PF3 | 3 | 0 |
| Past-flight booking / <2h reschedule | PF1–PF2 | 2 | 0 |
| Repeat-operation rejection | N7–N10 | 4 | 0 |
| Cross-user order access | S5 | 1 | 0 |
| Scheduler auto-expiry | SC1–SC2 | 2 | 0 |
| Cleanup (own flights cancelled) | CL1 | 1 | 0 |
| **Total** | | **50** | **0** |

## Assertion failures

**None.** 50 / 50 PASS, 0 FAIL.

## Limitations / notes

1. **Not browser-tested.** This is API-level regression only; no DOM/click automation across the frontend.
2. **Business errors are HTTP 200** with non-zero body `code` (400/401/403). HTTP 401/403 status is only emitted
   by the security layer (missing/invalid token, role denial).
3. **Scheduler timeout path tested** via one authorized focused `pay_deadline` backdate of a newly-created order
   (id=43); running scheduler auto-cancelled it at ≈45s with seats restored and no cancel endpoint invoked.
4. **Earlier false FAILs were harness bugs, not app defects**: (a) a PowerShell operator-precedence error
   (`(BodyCode $x -eq 0)` evaluated as `BodyCode ($x -eq 0)`), and (b) an O10 seat baseline captured before the
   O9 lock. Both corrected; the authoritative run is 50/50.
5. Single-run snapshot on a shared live DB; totals include data from prior test sessions
   (e.g. city count 14 includes leftover `测试出发城/到达城` entries).
6. Leftover test data in the shared DB from this run: tester `ptester_1011185019`; cancelled flights 104–110;
   orders 36,37,39,40,41,42,43 and ticket 47. Identified but not removed (no destructive cleanup requested).
