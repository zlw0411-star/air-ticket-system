# Frontend Passenger Regression — Untested Page Clicks

- Date: 2026-10-11
- Frontend under test: `http://localhost:5173` (IPv6-bound, not `127.0.0.1:5173`)
- Backend: `http://127.0.0.1:8080`
- Browser: real headless Microsoft Edge via already-installed Python Playwright
  (`E:\workspace\cs-study-assistant\.venv`, `channel="msedge"`)
- Isolation: a separate `browser.new_context()` per flow; isolated UI-registered passenger
  `uitp1011191722` was created through the register page and used for every order/password step.
- Authoritative run id: `1011191722`; own flights `UITP1011191722A` (id=116) and
  `UITP1011191722B` (id=117), both 北京→上海 and both `SCHEDULED` during testing.
- Demo accounts `user/User123` and `admin/Admin123` were used **only for admin login** to create
  own test flights; no demo account or seeded flight was written.
- Servers were **not stopped or restarted**; application source was **not edited**; no installs/downloads.
- Own test flights were cancelled after the run: all `UITP*` flights observed in DB are `CANCELLED`.

Book.vue auto-pays on submit (`src/views/passenger/Book.vue` calls `POST /orders/{id}/pay`
immediately), so the cancel test created a `PENDING_PAYMENT` order through `POST /api/orders`
with the isolated passenger’s token, then opened that order in the already-logged-in browser and
clicked `取消订单`.

## Result Summary

| Metric | Value |
|---|---|
| Named functional checks | **11** |
| PASS | **11** |
| FAIL | **0** |

## 1. Cancel unpaid order button — PASS

Setup: `POST /api/orders` as `uitp1011191722` on `UITP1011191722A`, ECONOMY, 1 passenger.
Observed before opening the order in the browser: order id=49 `PENDING_PAYMENT`, ticket id=57
`PENDING`, flight A economyAvailable=119.

Browser actions: logged in as the isolated passenger, opened `/orders/49`, clicked `取消订单`,
waited for the reload.

Observed exact UI text after clicking:

```
订单 AT1791717451889BDDB
状态：已取消　金额：¥520　已付：¥0
票号 TN179171745189050D8
乘机人 测试乘客
航班 UITP1011191722A 北京→上海
舱位 经济舱
票价 520
状态 已取消
```

API/DB verification after click:
- order `status = CANCELLED`
- ticket `status = CANCELLED`
- flight inventory `ECONOMY available 119 -> 120` (seat restored)

**PASS** — `取消订单` button works and seats are restored.

## 2. Reschedule button — PASS

Setup: `POST /api/orders` + `POST /orders/{id}/pay` as the isolated passenger on flight A
(id=116), ECONOMY. Before browser: order id=50 `PAID`, ticket id=58 `ISSUED`, flight A ECONOMY
available=119.

Browser actions: logged in as the isolated passenger, opened `/orders/50`, clicked `改签`. The
dialog opened with title `选择改签航班`; the same-route search already returned `UITP1011191722B` because
both own flights depart the same future date (A 09:00, B 17:00), so no date change was needed.
Clicked `改签到此航班` in B’s row.

Observed:
- Dialog title: `选择改签航班`
- Toast after submit: `改签成功`
- Order detail after reload shows ticket status: `已改签`
- Ticket detail flight shown: `UITP1011191722B 北京→上海`

API/DB verification after click:
- ticket id=58 `status = RESCHEDULED`, `flight_id = 117`
- reschedule_record: `from_flight_id=116`, `to_flight_id=117`, `from_cabin=ECONOMY`, `to_cabin=ECONOMY`, `price_diff=0`
- Flight A ECONOMY was restored to available=120; flight B ECONOMY was locked from 120 to 119

**PASS** — `改签` button, dialog search, and `改签到此航班` all work.

## 3. Refund button — PASS

Setup: `POST /api/orders` + `POST /orders/{id}/pay` as the isolated passenger on flight B
(id=117), ECONOMY. Flight B departs in >24h. Before browser: order id=51 `PAID`, paidAmount=520,
ticket id=59 `ISSUED`.

Browser actions: opened `/orders/51`, clicked `退票`. Element Plus message box appeared with
exact text `确认退票？将按退票规则退款`. Clicked confirm (`确定`).

Observed:
- Toast: `退票成功`
- Ticket status after reload: `已退票`
- Order status: `已退票`
- Paid amount: `¥520 -> ¥0`

API/DB verification:
- ticket id=59 `status = REFUNDED`
- order id=51 `paid_amount = 0.00`, `status = REFUNDED`
- refund_record id=22 `refund_amount = 520.00`, reason `乘客申请退票`
- flight B ECONOMY inventory restored from 119 to 120 at refund time (later rescheduled ticket 58
  holds the one seat shown as 119 in the final DB snapshot; ticket 58 is intentionally left
  RESCHEDULED on the now-cancelled own flight)

**PASS** — `退票` button and confirmation flow work, with full refund for >24h and paidAmount
dropped.

## 4. Change password page — PASS

All through `/profile` as isolated passenger `uitp1011191722`.

| Check | Observed | Result |
|---|---|---|
| 4.1 wrong old password | toast `原密码不正确` | PASS |
| 4.2 correct old → new `UitpTest2` (>=6 chars) | toast `密码已修改` | PASS |
| 4.3 logout, login with new password | redirect to `/`, Pinia `isLogin=true` | PASS |
| 4.4 restore via same page to `UitpTest1` | toast `密码已修改`; API login with original works | PASS |

Final state: isolated passenger remains enabled and can log in with the original password.

**PASS** — password validation and change/restore work end to end.

## Result by Feature

| Feature | Result |
|---|---|
| Cancel unpaid order button → `已取消`, seat restored | PASS |
| Reschedule dialog → B flight, `已改签` | PASS |
| Refund confirm → `已退票`, paid dropped | PASS |
| Wrong old password error | PASS |
| Password change / new-password login / restore | PASS |
| Cleanup of own test flights | PASS |

## HTTP console audit

No unexpected application errors were recorded during the browser flows. The order/pay API calls
made for setup are the normal lifecycle calls. Toasts are the intended success/error messages above.

## Honest Limitations

1. Browser engine: Microsoft Edge (Chromium) only; Firefox/WebKit were not run.
2. The reschedule test used two own future flights on the **same date** (A 09:00, B 17:00), as the
   task allows, so the in-flight-route search from the opened dialog already contained B and no
   manual date-picker change was exercised.
3. Book.vue auto-pays, so the cancel flow necessarily used an API-created `PENDING_PAYMENT` order
   rather than a form-created order; the cancel button itself was clicked in the real browser.
4. Both successful setup bookings were created through `POST /api/orders` + pay instead of filling
   the Book.vue form, because the existing Book.vue form was already covered by prior regression
   docs and this run targets the untested page action buttons (cancel/reschedule/refund/password).
5. One final DB snapshot shows `uitp1011191722B` ECONOMY available=119 because the rescheduled
   ticket id=58 remains RESCHEDULED on that now-cancelled own flight; it was deliberately not
   refunded because the reschedule scenario only requires `已改签`. It is isolated to own test data.
6. Test artifacts remain in the shared DB (isolated passenger, cancelled own flights, order ids
   49/50/51 and tickets 57/58/59) and are identified rather than destructively deleted.