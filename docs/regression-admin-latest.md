# Admin Regression — Latest Evidence Report

- Date: 2026-10-11
- Target base URL: `http://127.0.0.1:8080`
- Tester: foreground HTTP API session (PowerShell `Invoke-WebRequest`, UTF-8 `byte[]` bodies)
- Server: not stopped or restarted; application source not edited
- Roles under test: `admin` / `Admin123` (ADMIN), `user` / `User123` (PASSENGER)
- Session stamp: `261011184856`

All requests sent `Content-Type: application/json; charset=utf-8` with UTF-8 encoded bytes.
All responses decoded with `UTF8.GetString(RawContentStream)` (byte-exact, no console-codepage reliance).
`code` below is the JSON body `code` field; `http` is the HTTP status line.

---

## 1. Admin login

| Check | Result |
|---|---|
| `POST /api/auth/login` admin/Admin123 | http=200, code=0, token length 192 |
| returned user role | `ADMIN` |

**PASS** — admin authentication works and role is ADMIN.

## 2. Statistics field integrity + independent revenue reconciliation

`GET /api/admin/statistics` → http=200, code=0.

| API field | API value |
|---|---|
| revenue | 7342.00 |
| orderCount | 18 |
| todayRevenue | 7342.00 |
| todayOrderCount | 18 |
| hotRoutes | 5 entries (all `北京 → 上海` routes) |

Independent reconciliation: paginated **all** `GET /api/admin/orders` (size=50, total=26),
summed `order.paidAmount` for rows whose `order.status` is `PAID` or `REFUNDED`.

| Independent value | Computed |
|---|---|
| ordersTotal | 26 |
| paidAmount sum over PAID+REFUNDED | 7342 |
| count over PAID+REFUNDED | 18 |
| paidAt=today paidAmount sum | 7342 |

Observed status distribution across all 26 orders: `PAID=6`, `REFUNDED=12`, `CANCELLED=7`, `PENDING_PAYMENT=1`.

| Assertion | Result |
|---|---|
| `revenue` == independent paidAmount sum | PASS (diff 0.00) |
| `orderCount` == PAID+REFUNDED count | PASS (18 == 18) |
| `todayRevenue` == independent today sum | PASS (diff 0.00) |
| `todayOrderCount` == today paid count | PASS |

Note: `revenue` semantics = paidAmount across PAID+REFUNDED orders (matches `OrderService.statistics()`).
All documented fields (`revenue`, `orderCount`, `todayRevenue`, `todayOrderCount`, `hotRoutes`) are present
and internally consistent.

## 3. Users list / search / pagination

`GET /api/admin/users?page=1&size=5` → http=200, code=0.

| Check | Result |
|---|---|
| total | 17 |
| page1 size5 count | 5 (ids 17,16,15,14,13) |
| `GET /api/admin/users?keyword=admin` | http=200, code=0, total=1 (admin) |
| `GET /api/admin/users?page=2&size=5` | http=200, code=0, total=17, count=5 |

**PASS** — keyword search works, pagination total is stable, page 2 returns a full page.

## 4. Isolated tester account lifecycle

Created isolated passenger `tester_261011184856` / `Tester123` via `POST /api/auth/register`.

| Step | Request | Observed |
|---|---|---|
| register | POST /api/auth/register | http=200, code=0, id=18, realName byte-exact "回归测试员" (True) |
| disable | PUT /api/admin/users/18/status `{enabled:false}` | http=200, code=0, message "ok" |
| disabled login | POST /api/auth/login | http=200, **code=400**, message byte-exact "账号已被禁用" (True) |
| re-enable | PUT /api/admin/users/18/status `{enabled:true}` | http=200, code=0 |
| login again | POST /api/auth/login | http=200, code=0, token issued |
| disable admin (guard) | PUT /api/admin/users/1/status `{enabled:false}` | http=200, **code=400**, message "不能禁用管理员账号" |

**PASS** for the full lifecycle. Note the disabled-login business error is HTTP 200 with body `code=400`
(as warned). `user` demo account (`id=2`) and `admin` (`id=1`) were never disabled or modified.

## 5. Flights list / pagination

| Check | Result |
|---|---|
| `GET /api/admin/flights?page=1&size=5` | http=200, code=0, count=5 |
| `GET /api/admin/flights?page=1&size=100` | http=200, code=0, total=67 |
| `GET /api/admin/flights?page=2&size=5` | http=200, code=0, total=67, count=5 |

**PASS** — list and pagination correct.

## 6. Create flight — Chinese fields, space datetime + ISO datetime

**6a. Space-separated datetime** `POST /api/admin/flights`:
departureTime `2027-08-07 08:30:00` (space). http=200, code=0, created id=95.

Byte-exact Chinese round-trip (`chineseExact=True`):
`airline="测试航空"`, `departureCity="测试出发城"`, `arrivalCity="测试到达城"`,
`departureAirport="测试出发航站楼"`, `arrivalAirport="测试到达航站楼"`, `aircraftType="测试机型A320"`.
Economy 666.66/88, Business 1666.66/8. Status SCHEDULED. **PASS**

**6b. ISO datetime** departureTime `2027-08-08T09:15:00`. http=200, code=0, created id=96.
ISO string accepted and serialized back as `2027-08-08 09:15:00`. **PASS**

## 7. Full update (Chinese)

`PUT /api/admin/flights/95` all fields changed. http=200, code=0.
`chineseExact=True`: airline "测试航空改", depCity "测试出发城改", arrCity "测试到达城改",
depAirport "C测试出发改T3", arrAirport "C测试到达改T4", aircraft "测试机型改B787".
Economy 700.00/90, Business 1700.00/10. **PASS**

## 8. Price-only partial update (preserve fields + seats)

`PUT /api/admin/flights/95` body `{economyPrice:555.55}` only.

| Field | After |
|---|---|
| economyPrice | 555.55 |
| flightNo / airline / cities / airports / aircraft | unchanged |
| economyTotal / businessTotal | 90 / 10 (unchanged) |
| businessPrice | 1700.00 (unchanged) |

`preserved=True`. **PASS**

## 9. Seats-only partial update (preserve price)

`PUT /api/admin/flights/95` body `{economySeats:95, businessSeats:12}` only.

| Field | After |
|---|---|
| economyPrice / businessPrice | 555.55 / 1700.00 (unchanged) |
| economyTotal / economyAvailable | 95 / 95 |
| businessTotal / businessAvailable | 12 / 12 |

`priceKept=True`, `seatsOk=True`. **PASS**

## 10. Status → CANCELLED and verification

`PUT /api/admin/flights/95/status` `{status:"CANCELLED"}` → http=200, code=0.
Re-fetched via `GET /api/admin/flights?page=1&size=100`: id 95 status = `CANCELLED`, flightNo unchanged.
**PASS**

### Diagnostic (UNSUPPORTED — not a documented endpoint)
`GET /api/admin/flights/95` is **not** in `docs/API.md` (Admin section lists only list/create/update/status).
Requested for diagnostics only: **http=500, code=500, "服务器内部错误"**.
No such mapping exists, so it falls through to the generic error handler.
Marked **UNSUPPORTED / diagnostic** — NOT counted as a supported-test failure.

## 11. Orders list / status filtering

| Request | Observed |
|---|---|
| `GET /api/admin/orders?page=1&size=5` | http=200, code=0, total=26, returned 5 |
| `GET /api/admin/orders?status=PAID&page=1&size=100` | http=200, code=0, total=6, all rows PAID=True |
| `GET /api/admin/orders?status=REFUNDED&page=1&size=100` | http=200, code=0, total=12, all rows REFUNDED=True |
| `GET /api/admin/orders?status=PENDING_PAYMENT&page=1&size=100` | http=200, code=0, total=1 |

**PASS** — status filter returns only matching rows; totals match the independently paginated set
(`6 + 12 + 7 + 1 = 26`).

## 12. Passenger denied admin functions

Logged in as `user` / `User123` (http=200, code=0, role PASSENGER). Token used against admin endpoints:

| Endpoint | Result |
|---|---|
| GET /api/admin/statistics | http=403 |
| GET /api/admin/users | http=403 |
| GET /api/admin/flights | http=403 |
| GET /api/admin/orders | http=403 |
| GET /api/admin/statistics (no token) | http=401 |
| GET /api/admin/users (no token) | http=401 |

**PASS** — passengers are denied (403); anonymous access is 401.

## 13. Cleanup

Flights created this session, all cancelled via `PUT status {CANCELLED}` → http=200, code=0:

- id=95 (TST261011184856) — cancelled
- id=96 (ISO261011184856) — cancelled

All other flights created by earlier runs of this same tester harness (ids 84,85,93,94) were already CANCELLED.
Two orphan `TST*` SCHEDULED flights from a prior harness run (ids 71,72) were also cancelled during cleanup.

No orders/tickets were created against real demo data; no destructive deletes performed.
Isolated tester accounts left in place, enabled (per instructions: never destructive).

---

## Summary

- Supported admin functions tested: **all passed** (login, statistics integrity + reconciliation, users
  list/search/pagination, tester disable/enable lifecycle, flights list/pagination, create Chinese+space/ISO,
  full update, price-only, seats-only, status CANCELLED + verification, orders list/status filter, passenger denied).
- Character encoding: all Chinese values matched source by exact codepoint comparison (not console eyeballing).
- Business errors correctly surfaced as HTTP 200 with body `code=400` (disabled login, admin-disable guard).
- Unsupported check: `GET /api/admin/flights/{id}` returned http=500/code=500 and is expected to be absent;
  flagged as **UNSUPPORTED diagnostic**, not a supported failure.
- Assertion failures: **none**.

### Limitations
- Single-run snapshot at 2026-10-11; live DB shared with other demo data (order/user/flight totals drift between runs).
- Revenue reconciliation counts only orders returned by `GET /api/admin/orders` (same source as statistics);
  it validates internal consistency, not independently sourced ledger data.
