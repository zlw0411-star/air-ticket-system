# Frontend Browser Regression — Latest Evidence Report (stale-token Pinia fix)

- Date: 2026-10-11 (latest local run)
- Frontend under test: **`http://localhost:5173`** (frontend is IPv6-bound; `127.0.0.1:5173`
  intentionally NOT used).
- Backend: `http://127.0.0.1:8080`.
- Tester: REAL headless browser automation — the already-installed **Python Playwright** in
  `E:\workspace\cs-study-assistant\.venv`, driving OS-installed **Microsoft Edge**
  (`channel="msedge"`). Isolated `browser.new_context()` per flow.
- Servers: **not stopped or restarted**; application source **not edited**; nothing installed.
- Demo data: `user` / `User123` and `admin` / `Admin123` used **login-only**. No seeded flight
  created/edited/deleted. Admin dialogs were opened and inspected but **left unsaved**.

## Method

Per isolated context I recorded `console` errors and every HTTP response with `status >= 400`.
Live Vue/Pinia state was read from
`document.querySelector('#app').__vue_app__.config.globalProperties.$pinia.state.value.auth`.
After the 401 interceptor fired I waited **600 ms** for the `import('../stores/auth').then(...)`
microtask to run `logout()` before reading Pinia.

---

## PRIMARY — Stale namespaced token → 401 interceptor clears live Pinia — **PASS**

Fresh isolated context. Injected the namespaced keys
`air_ticket_token = "stale.invalid.token"` and
`air_ticket_user = {id:999, username:"ghost", role:"PASSENGER"}`, then loaded `/orders`.
The protected request returned backend **401**.

| Check | Observed | Result |
|---|---|---|
| URL after interceptor | `http://localhost:5173/login` | **PASS** |
| `localStorage['air_ticket_token']` | `None` (cleared) | **PASS** |
| `localStorage['air_ticket_user']` | `None` (cleared) | **PASS** |
| UI logged-out (login form visible) | `password_inputs = 1` | **PASS** |
| UI no `退出` button | `quit_count = 0` | **PASS** |
| **Live Pinia `auth.token`** | **`""` (falsy)** | **PASS** |
| **Live Pinia `auth.user`** | **`None`** | **PASS** |
| **Live Pinia `auth.isLogin`** | **`false`** | **PASS** |

Raw live Pinia read after the flow:

```json
{"token": "", "user": null, "isLogin": false, "keys": ["token", "user"]}
```

**BUG FIXED.** Previously (`docs/regression-frontend-latest.md` S8) Pinia retained
`"stale.invalid.token"` / `{id:999,...}`. The current `src/api/http.js:15-21` `clearSession()`
performs `import('../stores/auth').then(({ useAuthStore }) => useAuthStore().logout())`, which
resets the reactive store. Verified live: token empty, user null, `isLogin` false.

### Router guard after logout (client-side nav) — **PASS**

| Nav | Resulting URL | Result |
|---|---|---|
| `$router.push('/orders')` | `http://localhost:5173/login` | **PASS** |
| `$router.push('/profile')` | `http://localhost:5173/login` | **PASS** |

The guard sees `isLogin === false` and bounces to `/login`.

## Normal login after stale-token flow (NEW context) — **PASS**

| Check | Observed | Result |
|---|---|---|
| `user` / `User123` login redirect | `http://localhost:5173/` | **PASS** |
| UI shows `退出` | `quit_count = 1` | **PASS** |
| Pinia `isLogin` | `true` (token + user = id 2 `user`) | **PASS** |

## Admin Flights.vue dialog recheck — **PASS**

### Edit — seat pre-fill matches API totals

Opened 编辑 on the first two rows; seat fields non-empty and equal the admin API's
`economyTotal` / `businessTotal` (source binds `row.economySeats ?? row.economyTotal`;
admin API exposes only totals, so fallback applies).

| Flight edited | 经济舱座 shown | API economyTotal | 商务舱座 shown | API businessTotal | Result |
|---|---|---|---|---|---|
| `ISO261011184827` | `100` | 100 | `10` | 10 | **PASS** |
| `ISO261011101819` | `99` | 99 | `9` | 9 | **PASS** |

### Edit → 新增航班 (create)

Sequence without saving (no seeded flight touched):

| Check | Observed | Result |
|---|---|---|
| Edit dialog title | `编辑航班` | **PASS** |
| After edit→新增航班, dialog title | `新增航班` | **PASS** |
| `航班号` field after edit→新增航班 | empty (`''`) | **PASS** |
| `form.id` falsy | implied by title `新增航班` | **PASS** |
| Left unsaved | dialog closed via Escape, no save clicked | **PASS** |

## Console / HTTP audit (primary flow)

| Signal | Value |
|---|---|
| Console errors | `favicon.ico` 404 (cosmetic) + the expected `401` from the injected stale token |
| API responses ≥ 400 | `401 http://localhost:5173/api/orders?page=1&size=50` (the stale-token request) |

No unexpected application errors.

## Assertion summary

| Area | Result |
|---|---|
| PRIMARY stale token → 401 → `/login`, localStorage null, UI logged-out | **PASS** |
| PRIMARY **live Pinia cleared** (token `""`, user `null`, isLogin `false`) | **PASS (bug fixed)** |
| Router guard client-nav `/orders`,`/profile` → stay `/login` | **PASS** |
| Normal login `user`/`User123` in new context | **PASS** |
| Admin edit seat pre-fill == API totals (2 flights) | **PASS** |
| Admin edit→新增航班: title `新增航班`, 航班号 empty, id falsy, unsaved | **PASS** |

**22/22 checks PASS, 0 FAIL.**

## Coverage limitations (honest)

1. **Browser engine:** Microsoft Edge (Chromium) only; Firefox/WebKit not run.
2. **Favicon 404** is reported but is not an application defect.
3. **Admin CRUD writes were NOT executed** — dialogs inspected and left unsaved.
4. Wait of 600 ms used for the dynamic-import microtask; a slower machine should increase it.
5. PowerShell console renders CJK as mojibake in raw stdout; all assertions were on in-browser
   DOM/JSON (UTF-8) values.
