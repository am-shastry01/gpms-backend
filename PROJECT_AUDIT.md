# Gatepass — project audit

Youth Robe gate pass management system. Two repositories:

| | |
|---|---|
| **Backend** | `D:\Projects\gpms-backend-master` — Spring Boot 3.3.5, Java 21, Maven, PostgreSQL + Flyway, MinIO, JWT |
| **Android app** | `D:\Projects\gate_pass` — Kotlin, Jetpack Compose, Retrofit + kotlinx.serialization, ServiceLocator DI |
| **Super-admin site** | Served by the backend at `/superadmin` — Thymeleaf pages, separate login |

Both need a rebuild to pick up everything below. The backend has **19 Flyway
migrations**; V11 onward are new in this work.

---

## 1. What the system does now

**One deployment, many client companies.** Each company owns its warehouses,
its people, its master data and its gate passes. Nothing in the tenant-facing
API can read across two companies — not even a company's own admin.

**A gate pass carries three signatures.** Employee, manager, admin. Whoever
raises it has already signed; the other two are outstanding. A pass is
`APPROVED` only when all three blocks are filled, and every role can open and
sign any pass at their own site.

**Three ways in:**

- **Super admin** (you) at `/superadmin` — onboards companies, sees every
  client, suspends one, adds or removes warehouses.
- **Company admin** — runs one company: all its warehouses, its people, its
  dropdowns, its letterhead.
- **Manager / employee** — posted to exactly one warehouse.

---

## 2. Database migrations added

| Migration | What it does |
|---|---|
| `V11` | `transport_name`, `invoice_number`, `invoice_date` on gate passes |
| `V12` | The employee signature block (`employee_approved_by/at/signature_id`) |
| `V13` | `must_change_password` on users |
| `V14` | `website` on the company profile |
| `V15` | **`companies` table**; `company_id` on warehouses and users; warehouse codes unique per company; `users.warehouse_id` made nullable |
| `V16` | `company_id` on vendors, drivers, vehicles, destinations, transports, vehicle types; all global unique constraints replaced with per-company ones |
| `V17` | `users.username` widened to 150 so it can hold an email |
| `V18` | `password_changed_at` on users |
| `V19` | `companies.code` (unique) and `company_document_counters` for per-company gate pass numbering |

All 19 were applied in order against a live PostgreSQL 16 and the resulting
schema was checked against what Hibernate's `ddl-auto: validate` demands at
startup.

---

## 3. Bugs found and fixed

### Critical

**A truncated migration file.** `V1__baseline_schema.sql` was missing its first
479 bytes and began mid-statement. Flyway would have failed on the very first
migration against any fresh database. Restored from a snapshot and verified by
checksum; a 193-file comparison confirmed it was the only corrupted file.

**One gate pass per consignment.** The form sent one POST per consignment, so a
truck with three consignments produced three gate pass numbers and three sets of
signatures for one journey. Now a single create with every consignment as a line
item — which is what the `gate_pass_items` table was always for.

**Cross-tenant data leak through shared master data.** Driver mobile numbers and
vehicle registrations were unique across the whole database. The second client to
send the same lorry driver through their gate would have resolved to the *first*
client's driver row and printed that client's spelling of his name on their pass.
Every master-data table now has an owner and per-company uniqueness.

**Admins could see every company's figures.** `effectiveWarehouseId` returned
null for an admin, meaning "all warehouses" — which across companies meant
everyone's. Every gate pass query and every dashboard count is now fenced by
company first, with no exception for admins.

**The bootstrap wrote into other people's companies.** It picked its target
company by *name*, alphabetically, on every startup — so onboarding a client
called Acme handed them a stray "Primary Warehouse" and three default accounts,
because A sorts before Y. It now seeds only a genuinely empty database.

**A failed login returned HTTP 500.** `BadCredentialsException` was uncaught, so
a mistyped password fell through to the generic handler. The app read that as
"the server is having trouble, try later". Now 401 with "Invalid username or
password", and a separate message for a disabled account.

**The JWT filter skipped all of `/auth/**`.** The bearer token was never read on
`/auth/me` or `/auth/change-password`, so the security context stayed empty and a
perfectly good token came back as 403. Session restore on the splash screen had
never worked for the same reason.

**Login rejected a correct email.** The sign-in field says "email" but the server
only ever looked accounts up by username. Login now accepts either.

### Significant

- **Gate passes were invisible to the people who had to sign them.** An employee
  could only open their own requests, so they could never sign an admin-raised
  pass. Both the detail guard and the list filter removed.
- **Admins could not raise a gate pass at all** — no warehouse of their own, no
  warehouse in the form. The form now shows a warehouse dropdown for accounts
  that aren't posted to a site.
- **Adding a transport company failed validation** — the server required a `code`
  the app never sent. Codes for transports and vendors are now derived from the
  name.
- **Creating a user 404'd** — `warehouseId` was mandatory server-side and the app
  never sent one.
- **An account with no warehouse saw the whole company.** A null warehouse filter
  means "all of them", so a non-admin without a site saw every warehouse's
  passes, queues and counts. Now it sees nothing until given one.
- **Report dates used the server's timezone**, so "today" drifted. Now
  `Asia/Kolkata`, configurable.
- **`APPROVED` was set on the first signature**, making a pass with two blank
  blocks read as approved everywhere it was counted.
- **Consignment photos were never uploaded** — the endpoint existed and nothing
  called it.
- Several app-side compile breaks caught and fixed along the way (missing
  imports, a masked import, an undeclared state field, a dropped static method a
  test depended on).

---

## 4. Security review

### Fixed

| Issue | Was | Now |
|---|---|---|
| **JWT signing secret** | A placeholder published in the repo *and* in `docker-compose.yml`. Anyone with the source could mint a `ROLE_ADMIN` token against a server that forgot to set it. | Refused at startup unless `APP_ALLOW_INSECURE_JWT_SECRET=true`; under 32 characters always refused. |
| **Seeded accounts** | `admin/Admin@123`, `manager/Manager@123`, `employee/Employee@123`, published in `application.yml`. | Created with must-change-password, so a known default cannot be used. |
| **DB / MinIO credentials** | Defaulted to `gpms/gpms` and `minioadmin/minioadmin`, so a missing env var ran on a published password. | No fallback: unset fails at startup naming the placeholder. |
| **Token outlived its password** | No revocation list, so a stolen phone kept working for hours after the owner changed their password. | `password_changed_at`; any token issued earlier is refused. |
| **Token lifetime** | 24 hours. | 8 hours — one shift. |
| **Swagger / `/v3/api-docs`** | Public in every profile. | Off unless `APP_DOCS_ENABLED=true`. |
| **500 responses** | Returned the raw Hibernate/Postgres message — constraint and column names, SQL fragments. | Logged server-side, generic to the client. Same for MinIO errors, which leaked the endpoint and bucket. |
| **Consignment upload** | No type or size check, and the client's content type was stored and served back — a stored-XSS path. | PNG/JPEG/WebP/PDF only, 8 MB cap, type taken from an allow-list. |
| **Super-admin brute force** | One account, one password, no lockout. | 10 failures per address buys a 15-minute wait; `X-Forwarded-For` honoured only for its first hop. |
| **App request logging** | OkHttp logged full bodies and headers — every bearer token and password into logcat. | Method and URL only in debug, nothing in release. |
| **App backups** | `allowBackup="true"` with the session token in SharedPreferences — an adb backup lifted it. | Off. |
| **App cleartext** | Permitted to *any* host. | Only the dev hosts; everything else HTTPS. |
| **Company suspension** | Nothing enforced it. | Checked at sign-in, so suspending a client stops every one of their people. |

### How the super-admin site is isolated

Its own filter chain at `/superadmin/**`, ordered ahead of the API's, with its
own session, its own CSRF protection, and credentials from the environment —
never a row in the users table. Nothing a company admin can do can produce an
account that opens it. The API's JWT filter isn't on that chain, so a bearer
token means nothing there and a session means nothing on the API.

### Still open — my recommendations

1. **Rotate the dev secrets before this goes anywhere real.** `APP_JWT_SECRET`,
   `APP_SUPERADMIN_PASSWORD`, the Postgres password and the MinIO keys are all
   development values in `docker-compose.yml`.
2. **Set `APP_BOOTSTRAP_ENABLED=false`** after the first real start, and delete
   the seeded `admin` / `manager` / `employee` accounts.
3. **Put the super-admin site behind something more than a password** — an IP
   allowlist or a VPN. It is the keys to every client.
4. **No server-side rate limiting on the app's login.** A determined attacker can
   grind at `/auth/login` as fast as the server answers.
5. **MinIO object URLs are handed to clients directly.** If that bucket is
   publicly readable, every consignment photo is public to anyone with a URL.
   Worth switching to presigned, expiring URLs.
6. **R8 / ProGuard is disabled** for release builds, so the APK ships readable.
7. **No audit trail on the super-admin site.** Onboarding, suspending and
   removing a warehouse all go unrecorded.

---

## 5. What was built

### Super-admin site (`/superadmin`)

- Company list with warehouse, people and gate pass counts.
- Onboarding form: company details, GST, contact, address, short code, a
  **warehouse table** (name + your own number per row, add/remove rows), and the
  client's admin account. All created in one transaction.
- Company detail: contact block, warehouses with staff and pass counts, people
  with their sign-in email and password status, add/remove warehouse, suspend or
  resume the company.

### Android app

- **Login** rebuilt — Help pill, wordmark, in-field icons, eye toggle, pill
  button. Accepts username or email.
- **First sign-in** forces a password of the user's own. No back, no skip.
- **One home screen for all three roles** — Today's Summary, two stat boxes, a
  quick bar (Print PDF / Drafts / Fully signed / To sign with a red badge),
  recent passes, and a floating glass dock.
- **Create Truck Request** — Driver / Truck / Destination and Dispatch /
  Consignment / Remarks boxes, black-outlined fields, required stars, `+91 │`
  prefix, multi-consignment dialog with photo capture, draft-on-back-press, and a
  warehouse dropdown for admins.
- **History** — search by truck or pass number, select up to 10, download or
  print merged PDFs, withdraw.
- **Gate pass details** — one screen for all roles, four boxes, signature status.
- **To sign** — a role-agnostic queue; alerts show only what awaits your
  signature.
- **Admin settings** — Manage people (warehouse-first), Vendors, Transport
  companies, Destinations, Vehicle types.
- **Brand** — a monochrome Gatepass wordmark, white on dark and dark grey on
  light, with Poppins bundled; applied app-wide, launcher included.
- **Splash** shows the logo and nothing else.

### Gate pass PDF

Company name centred → rule → `Gate pass :` left, `Invoice date :` right → rule
→ vehicle and dispatch → consignments by name → three signatures with no dates →
"This is a computer generated gate pass." → a boxed company address, phone,
email and website at the left foot. Numbers are per company and per year:
`YROBE-GP-2026-000001`.

---

## 6. Files removed

Each verified twice as unreferenced before deletion:

- `company/domain/CompanyProfile.java` and `CompanyProfileRepository.java` —
  superseded by `Company`; the `company_profile` table stays, unmapped, because
  `V15` reads from it.
- `pass.pdf` — a stray generated PDF in the backend root.
- `build.txt` — a Gradle log in the app root.
- Five dead `Routes` objects (`AdminDashboard`, `AdminPending`, `AdminHistory`,
  `AdminProfile`, `AdminSettings`).
- `AdminRepository.vendors()` / `addVendor()` — superseded by the vendor branch
  of the managed-list screen.
- Earlier in the work: the whole old admin dashboard, pending and history trees,
  the guard flow, and several dummy-data files.

**Left alone deliberately:** `Claude outputs/` (logo masters worth keeping,
though the 20 MB `gatepass-debug.apk` inside it is stale and safe to delete),
`gpms-master.iml` (IDE file), `scripts/seed_sample_data.sql`.

---

## 7. How this was verified

Maven Central is blocked from my sandbox and the bridge VM only has Java 11, so I
could not compile. Instead:

- **All 19 migrations run against a live PostgreSQL 16**, in order, from an empty
  database — and the schema compared against what `validate` requires.
- **Behavioural tests on the real database:** two companies both holding a WH-01,
  both recording the same driver's mobile, both with an EKART vendor — and a
  duplicate *within* one company refused by the right constraint. Per-company
  counters verified independent, resetting by year, and company codes unique.
- **Purpose-built analyzers** over backend `main` + `test` and the whole app:
  Spring Data derived-query property resolution (these fail at *startup*, not
  compile), method calls walking inheritance, static and class-qualified calls,
  record constructor arity, Java imports, Kotlin named and required arguments,
  Thymeleaf expressions against the view records, brace balance with a proper
  string/comment stripper.

All currently clean. Three findings remain and are known false positives: a
`Map<String,String>` comma miscounted as an argument, `SystemRole.values()`
(implicit enum method), and `User.withUsername()` where `User` is Spring
Security's class, not the domain one.

**What this does not cover:** type checking, generics, Compose compiler rules and
anything Gradle or Maven would catch. Your Docker build remains the real check.

---

## 8. What to do next

### Should be done soon

1. **Rotate every development secret** (see security, item 1).
2. **Wire the managed dropdowns into the request form.** Destination, vehicle type
   and transport company are still free-text, so the lists an admin curates in
   Settings aren't actually offered to the person filling the form. This is the
   biggest gap between what the admin configures and what the app does.
3. **Request logging and the traffic dashboard** — planned, not built. Per-company
   calls, error rates, slowest endpoints, recent failures.
4. **A "move to another warehouse" action for staff.** Anyone created before the
   warehouse-first flow may have no site, and there is currently no way to assign
   one except delete and re-add.
5. **Make consignments properly optional**, if you want that — it needs a nullable
   `vendor_id` and optional package fields on the backend.

### Worth considering

- **Server-side drafts.** Drafts live on one phone; losing the handset loses them.
- **Push notifications (FCM).** "To sign" only updates when the app is opened.
- **Editing a company's short code** — deliberately fixed after creation, since
  changing it splits a company's numbering history. Say if you want it editable.
- **Per-company reference data seeding.** A new client starts with empty
  dropdowns and has to type everything in.
- **Admin-configurable mandatory fields** — asked for earlier; no backend support
  exists yet.
- **Company logo upload from the super-admin site.** The column and the PDF
  support it; only the admin's own screen can set it.
- **Tests.** Two unit tests exist. The signature flow, company scoping and the
  numbering counters are the three things most worth covering, and all three are
  the kind of logic that breaks quietly.
