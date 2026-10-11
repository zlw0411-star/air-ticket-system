# Frontend Regression — Previously Untested Admin PAGE Writes

- Date: 2026-10-11
- Frontend under test: **`http://localhost:5173`** (IPv6-bound; per instructions `127.0.0.1:5173` was NOT used)
- Backend: `http://127.0.0.1:8080`
- Tester: REAL headless browser automation — Python Playwright in `E:\workspace\cs-study-assistant\.venv`,
  driving the OS-installed **Microsoft Edge** via `channel="msedge"`, isolated `browser.new_context()` per flow.
- Servers: **not stopped or restarted**; application source **not edited**; nothing installed.
- Demo/protected data: `admin` / `Admin123` used **login-only**. `user` / `User123` and `admin` were **never**
  disabled or otherwise written. No seeded (`CA*`, `RF*`, existing demo) flight was edited or stopped.
- Scope: **UI-driven admin page writes** that were previously untested through the actual page buttons
  (prior runs exercised create/edit/status only via raw API and dialog prefill via DOM).
- Session stamp: `1791716871` (Unix seconds). Created flight: `UITA1791716871` (id 111).
  Created user: `uitauser_1791716871`.

## Method

Playwright drove the real Element-Plus UI: typed into the create/edit dialogs, clicked the actual
**保存 / 编辑 / 停飞 / 禁用 / 启用** buttons, and filled the `/register` form. Every write was
cross-checked against the live backend JSON (`GET /api/admin/flights`, `GET /api/admin/users`) using the
admin token read from `localStorage['air_ticket_token']`. Dialog field values were read from the DOM
(`.el-dialog .el-form-item` inputs matched by label text).

---

## 1) Admin create flight — page SAVE (click 保存) — **PASS**

Logged in as `admin` via the real `/login` page → `/admin/flights` → clicked **新增航班**.
Filled unique Chinese fields and a 2027 future datetime, then clicked **保存**.

Form inputs before save (DOM):

| Field | Value entered |
|---|---|
| 航班号 | `UITA1791716871` |
| 航司 | `UI测试航空` |
| 出发城市 / 到达城市 | `北京` / `上海` |
| 出发机场 / 到达机场 | `首都机场T3` / `虹桥机场T2` |
| 起飞时间 / 到达时间 | `2027-06-15 08:30:00` / `2027-06-15 11:45:00` |
| 机型 | `A330` |
| 经济舱价 / 经济舱座 | `888.88` / `111` |
| 商务舱价 / 商务舱座 | `2888.88` / `22` |

Observed:

| Check | Expected | Observed | Result |
|---|---|---|---|
| Dialog closes after 保存 | closed | **closed (`dialog_closed=True`)** | PASS |
| New row `UITA1791716871` in list | present | **present** | PASS |
| Row status | `计划中` | **`计划中`** | PASS |
| Row departure | `2027-06-15 08:30:00` | **`2027-06-15 08:30:00`** | PASS |
| API `GET /api/admin/flights` contains flightNo | present | **present, id=111** | PASS |

API record after create (byte-exact Chinese round-trip):

```json
{"id":111,"flightNo":"UITA1791716871","airline":"UI测试航空","departureCity":"北京",
 "arrivalCity":"上海","departureAirport":"首都机场T3","arrivalAirport":"虹桥机场T2",
 "departureTime":"2027-06-15 08:30:00","arrivalTime":"2027-06-15 11:45:00",
 "aircraftType":"A330","status":"SCHEDULED",
 "economyPrice":888.88,"economyAvailable":111,"economyTotal":111,
 "businessPrice":2888.88,"businessAvailable":22,"businessTotal":22}
```

**PASS** — page-level create SAVE persists the entered Chinese fields, 2027 future datetime, prices and seats;
the row appears in the list and the API agrees.

## 2) Admin edit SAVE of the new flight (seat prefill + persistence) — **PASS**

Opened **编辑** on `UITA1791716871`. The dialog prefilled from API totals
(`economySeats ?? economyTotal`, `businessSeats ?? businessTotal`). Changed price and seats, clicked **保存**,
then re-opened the dialog.

Prefilled fields on first edit open (DOM, read directly from the dialog inputs):

| Field | Prefilled | API total | Result |
|---|---|---|---|
| 经济舱座 | `111` | economyTotal `111` | PASS (prefilled from total) |
| 商务舱座 | `22` | businessTotal `22` | PASS (prefilled from total) |
| 经济舱价 | `888.88` | economyPrice `888.88` | PASS |

Edits applied: 经济舱价 `888.88 → 999.99`, 经济舱座 `111 → 123`, 商务舱座 `22 → 33`; clicked **保存**.

Re-opened 编辑 on the same flight (DOM values persisted):

```
航班号 UITA1791716871 | 航司 UI测试航空 | 北京 → 上海 | 首都机场T3 / 虹桥机场T2
起飞 2027-06-15 08:30:00 | 到达 2027-06-15 11:45:00 | 机型 A330
经济舱价 999.99 | 经济舱座 123 | 商务舱价 2888.88 | 商务舱座 33
```

API after edit: `economyPrice=999.99, economyTotal=123, economyAvailable=123,
businessTotal=33, businessAvailable=33` (businessPrice unchanged `2888.88`).

| Check | Expected | Observed | Result |
|---|---|---|---|
| Seats prefilled from totals (visual) | non-empty, = totals | `111` / `22` | PASS |
| Re-open shows new price | `999.99` | `999.99` | PASS |
| Re-open shows new seats | `123` / `33` | `123` / `33` | PASS |
| API economyTotal / businessTotal | `123` / `33` | `123` / `33` | PASS |
| API economyPrice | `999.99` | `999.99` | PASS |
| Other fields preserved | unchanged | unchanged | PASS |

**PASS** — dialog seat prefill from totals confirmed at the DOM level; edited price/seats persist across a
re-open and match the API totals.

## 3) Admin 停飞 of the new flight — **PASS**

Clicked **停飞** on `UITA1791716871` **only** (no seeded flight touched), then reloaded the list.

| Check | Expected | Observed | Result |
|---|---|---|---|
| Row status text after 停飞 | `已停飞` | **`已停飞`** | PASS |
| API status | `CANCELLED` | **`CANCELLED`** | PASS |
| flightNo unchanged | `UITA1791716871` | `UITA1791716871` | PASS |

**PASS** — status becomes 已停飞 in the list and `CANCELLED` in the API for the created flight only.

## 4) Disable / enable isolated user via Users page — **PASS**

Registered a unique passenger `uitauser_1791716871` / `Uitapass123` (realName `界面测试员`) through the
real `/register` page in a separate isolated context (redirected to `/` → register success).

Admin flow on `/admin/users`:

| Step (page action) | Expected | Observed | Result |
|---|---|---|---|
| Search `uitauser_1791716871` | row shown, status `启用` | row shown, `启用` | PASS |
| Click **禁用** | status `禁用` | status `禁用` | PASS |
| Login as that user (fresh context) | blocked | URL stayed `/login`; error `账号已被禁用` present in page | PASS |
| Click **启用** | status `启用` | status `启用` | PASS |
| Login again | succeeds | URL `http://localhost:5173/` | PASS |

The disabled-login failure surfaced as the business error `账号已被禁用` (backend returns HTTP 200 with body
`code=400`; the axios interceptor routes it to `ElMessage.error`). The user was left **enabled** afterwards.

**PASS** — page-driven disable blocks login with 账号已被禁用, page-driven enable restores login.
Neither `admin` nor the demo `user` was disabled; only the run-created user was toggled.

## 5) Firefox / WebKit — **BLOCKED** (not FAIL)

- Requested: check whether Firefox/WebKit is already installed; do not download; try a Playwright
  Firefox **channel** only if a binary exists.

| Probe | Observed |
|---|---|
| OS Firefox (`C:\Program Files*`, `%LOCALAPPDATA%`, `where firefox`, Mozilla registry) | **absent** |
| Playwright bundled Firefox (`%LOCALAPPDATA%\ms-playwright\firefox-1543\...\firefox.exe`) | **absent** |
| Playwright bundled WebKit (`...\webkit-2359\Playwright.exe`) | **absent** |
| `ms-playwright` cache directory | **does not exist** |
| `p.firefox.launch()` | fails: *Executable doesn't exist at ...\firefox-1543\firefox\firefox.exe* |
| `p.webkit.launch()` | fails: *Executable doesn't exist at ...\webkit-2359\Playwright.exe* |
| Chromium/Edge (`msedge`) | present and used |

No Firefox binary exists on the machine, so a Playwright Firefox channel launch was not attempted
(unlaunchable) and no browser was downloaded (per instructions). **Firefox and WebKit are BLOCKED**,
not FAIL. Only Microsoft Edge (Chromium) was exercised.

---

## Summary

| # | Admin PAGE write | Result | Key observed values |
|---|---|---|---|
| 1 | Create flight + SAVE | **PASS** | `UITA1791716871` id=111 created, status SCHEDULED, row 计划中, API matches |
| 2 | Edit flight + SAVE (prefill + persist) | **PASS** | prefill 111/22 (= totals); re-open 999.99 / 123 / 33; API totals match |
| 3 | 停飞 own flight | **PASS** | list `已停飞`; API status `CANCELLED` |
| 4 | Disable/enable isolated user | **PASS** | 禁用 → login blocked (`账号已被禁用`); 启用 → login OK |
| 5 | Firefox / WebKit | **BLOCKED** | no OS Firefox; Playwright firefox/webkit executables absent; no download |

Test data created by this run: flight `UITA1791716871` (id 111, left `CANCELLED` via the tested 停飞 flow),
user `uitauser_1791716871` (left **enabled**). No seeded flight/user/order was created, modified, or cancelled.
No application source was edited; no dependency was installed; servers were not stopped.

### Limitations (honest)

1. Browser engine: Microsoft Edge (Chromium) only; Firefox/WebKit BLOCKED (no binary present).
2. Frontend reached via `localhost:5173` (IPv6 binding), not the literal `127.0.0.1:5173` — same server process.
3. Single-run snapshot against a shared live DB; rows are identified by unique `flightNo`/`username`, not row index.
4. Dialog field values were read from the DOM inputs (label-matched); the Vue setup-state introspection used in
   earlier reports returned no node this run, so DOM + API were used as the two independent sources instead.
