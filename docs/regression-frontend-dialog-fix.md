# Frontend Regression — Admin Flights Dialog Fix (Targeted Re-verification)

- Date: 2026-10-11
- Frontend under test: `http://localhost:5173` (see port note below)
- Backend: `http://127.0.0.1:8080`
- Tester: REAL headless browser automation — Python Playwright 1.63.0 driving the OS-installed
  **Microsoft Edge** via `channel="msedge"`, isolated `browser.new_context()`.
- Servers: **not stopped or restarted**; application source **not edited**; no dependencies installed.
- Demo account: `admin` / `Admin123` (login only). No seeded flight/order/user was created,
  updated, or cancelled. The create/edit dialogs were left **unsaved** (no POST/PUT issued).
- Scope: only the two previously failing defects in `frontend/src/views/admin/Flights.vue`
  (patched by the lead): (1) edit-dialog seat prefill, (2) create-after-edit id retention.

## Port note (honest)

The task named `http://127.0.0.1:5173`, but the Vite dev server in this environment is
**bound to IPv6 `::1` only** (`Get-NetTCPConnection` → `::1:5173`), so `127.0.0.1:5173` refuses
the connection while `http://localhost:5173` returns 200. Testing was therefore performed against
`http://localhost:5173` (same server/process, owning PID 28528). Backend `127.0.0.1:8080` resolves
fine (`::` listener). This is an environment binding detail, not an application defect.

## Method

1. Fresh isolated context; logged in as `admin` via the real `/login` UI → redirected to `/admin`.
2. Navigated to `/admin/flights`; the table rendered the seeded flight rows.
3. Captured the live admin API JSON (`GET /api/admin/flights?page=1&size=10`) from inside the page
   using the app's own token, to compare against the values shown in the dialog.
4. Read dialog fields from the DOM (`.el-dialog .el-form-item` matched by label text) **and**
   read the live Vue setup state via `#app.__vue_app__` (component-tree walk to the Flights.vue
   setup that exposes `form` / `show`) to inspect `form.id`.

## API ground truth

`GET /api/admin/flights` returns `economyTotal` / `businessTotal` (plus `economyAvailable` /
`businessAvailable`) and **does not** return `economySeats` / `businessSeats`. Sample rows:

| id | flightNo | economyTotal | businessTotal |
|---|---|---|---|
| 74 | ISO261011101819 | 99 | 9 |
| 73 | TEST261011101819 | 95 | 12 |
| 72 | TSTCN01 | 150 | 8 |

The patch maps these totals into the form fields:
`economySeats: row.economySeats ?? row.economyTotal`, `businessSeats: row.businessSeats ?? row.businessTotal`.

---

## Defect 1 — Edit dialog seat prefill

Opened `编辑` on **two different existing flights** (rows 0 and 1) and read the dialog fields.
`经济舱座` / `商务舱座` must be non-empty and equal the API totals.

| Row | Dialog title | 航班号 | 经济舱座 (DOM) | API economyTotal | 商务舱座 (DOM) | API businessTotal | Result |
|---|---|---|---|---|---|---|---|
| 0 | 编辑航班 | ISO261011101819 | **99** | 99 | **9** | 9 | PASS |
| 1 | 编辑航班 | TEST261011101819 | **95** | 95 | **12** | 12 | PASS |

Vue setup state for row 0 edit (live read via `#app.__vue_app__`):

```
form_id      = 74
has_id       = true
flightNo     = ISO261011101819
economySeats = 99
businessSeats= 9
```

- Seat fields are no longer blank; both match the API total for the matching flight number.
- `经济舱价` also rendered (`777.77`) as a cross-check.

**Defect 1: PASS.** Observed values: `99/9` (flight 74) and `95/12` (flight 73) — non-empty and
equal to API `economyTotal`/`businessTotal` in both cases.

## Defect 2 — Create-after-edit must not retain the edited flight

Executed **without saving** (no data mutated):

1. Open `编辑` on row 0 (`ISO261011101819`, id 74) → title `编辑航班`, `form.id = 74`.
2. Close with Escape.
3. Click `新增航班` and inspect the **unsaved** dialog only.

| Check | Expected | Observed | Result |
|---|---|---|---|
| Dialog title after edit → create | `新增航班` | **`新增航班`** | PASS |
| `form.id` present / truthy | absent / falsy | `has_id = false`, `form_id = null` | PASS |
| 航班号 | not the previously edited flight | **`null` (empty)** — not `ISO261011101819` | PASS |
| 经济舱座 / 商务舱座 | create defaults | `120` / `16` | (defaults) |

Live Vue setup state for the create dialog:

```
form_keys    = [airline, aircraftType, economyPrice, economySeats, businessPrice, businessSeats]
(form has NO `id` key)
has_id       = false
form_id      = null
flightNo     = null
economySeats = 120
businessSeats= 16
```

- The dialog is correctly titled `新增航班`; the previously edited `id` (74) is gone, so `save()`
  would take the `POST` branch.
- 航班号 is cleared (null), not the prior `ISO261011101819`.
- Dialog was closed without saving; the seeded flight 74 was not modified.

**Defect 2: PASS.** Observed: title `新增航班`, `form.id` absent, 航班号 empty after an edit of id 74.

---

## Console / HTTP audit

- App console errors: 1 × favicon `404` only (cosmetic; browser auto-request).
- HTTP responses with status ≥ 400 during the flows: **none**.

## Summary

| Previously failing defect | Result | Key observed values |
|---|---|---|
| 1) Edit dialog seat counts blank | **PASS** | flight 74 → 经济舱座 99 / 商务舱座 9; flight 73 → 95 / 12; both equal API totals |
| 2) Create-after-edit retained flight id | **PASS** | title `新增航班`; `form.id` absent (`has_id=false`); 航班号 empty (was ISO261011101819) |

Both targeted defects are fixed in the running frontend. No application source was edited; no
seeded flight/order/user was created, modified, cancelled, or otherwise destructively written;
both dialogs were left unsaved.

### Limitations (honest)

1. Browser engine: Microsoft Edge (Chromium) only; Firefox/WebKit not run.
2. Frontend reached via `localhost:5173` (IPv6 binding), not the literal `127.0.0.1:5173` given —
   see port note; same server process.
3. Only the two targeted dialog defects were verified; no save (POST/PUT) was executed against
   seeded flights, so create/update persistence is out of scope here.
4. Single-run snapshot against a shared live DB; table row ordering may differ between runs, so
   flights are identified by `flightNo`, not row index.
