# Phase 11 - Discovery & Search

Baseline: `dbb7b48 phase-10-final-validation` (branch `main`).
Target commit message: `phase-11-discovery-search`.

**Commit status: NOT COMMITTED.** Implementation and partial verification were done in a
sandbox that had no Gradle, no Android SDK, no Maven access and could not run the Spring Boot
application. The full verification sequence in "Verification still required on the
development machine" must be run there before committing.

## Objective

Replace the authenticated Search placeholder with a real matrimonial profile discovery flow:
authenticated search API, discovery-safe DTOs, database-level pagination, authorized photo
access, an Android Search screen with filters, and a profile detail screen.

## Implementation summary

- New backend package `com.matrimony.discovery` (controller, service, repository, validator, DTOs).
- Search, profile detail and discovery photo endpoints, all behind the existing
  `AuthAuthenticationFilter` (no second authentication mechanism).
- Android: `ProfileSearchApi`, DTOs, `ProfileSearchRepository`, a pure-Kotlin filter validator and
  pagination state machine, `SearchScreen`, `ProfileDetailScreen`, bounded photo decoding.
- No Flyway migration was added (see "Migration changes").

## Changed files

Modified (Phase 11 changes only):

- `backend/src/main/java/com/matrimony/auth/ApiExceptionHandler.java` - map Spring MVC client errors (405, 404, malformed body/argument) to 4xx instead of the catch-all HTTP 500.
- `backend/src/main/java/com/matrimony/profile/ProfilePhotoService.java` - extracted `readStored(storageKey)`, strict storage-key format, no symlink following.
- `android/.../data/remote/ApiClient.kt` - extracted the existing auth interceptor into `internal fun authInterceptor` (behaviour unchanged) and added `createProfileSearchApi`.
- `android/.../MatrimonyApplication.kt`, `android/.../MainActivity.kt` - wiring.
- `android/.../ui/auth/AuthenticatedScreen.kt` - Search placeholder replaced by `SearchScreen`.

Added:

- Backend: `discovery/DiscoveryController.java`, `DiscoveryService.java`, `DiscoveryRepository.java`, `DiscoveryValidator.java`, `DiscoveryModels.java`.
- Backend tests: `DiscoveryValidatorTest`, `DiscoveryServiceTest`, `DiscoveryControllerTest`, `DiscoveryPhotoAccessTest`, `DiscoveryRepositoryIntegrationTest`, `TestDatabase` (helper).
- Android: `data/remote/ProfileSearchApi.kt`, `ProfileSearchDtos.kt`, `data/discovery/ProfileSearch.kt`, `ProfileSearchRepository.kt`, `ui/search/SearchScreen.kt`, `ProfileDetailScreen.kt`, `SearchScreenModel.kt`, `PhotoDecoding.kt`, `PhotoSampling.kt`.
- Android tests: `ProfileSearchRepositoryTest`, `SearchFiltersTest`, `SearchStateTest`, `ProfileSearchDtosTest`, `ApiClientAuthInterceptorTest`, `PhotoSamplingTest`.
- Scripts: `scripts/verify-discovery.ps1`, `scripts/verify-discovery-runtime.ps1`.
- This document.

## Backend endpoints

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/v1/profiles/search` | `ProfileSearchResponse` |
| GET | `/api/v1/profiles/{profileId}` | `ProfileDetailResponse` |
| GET | `/api/v1/profiles/{profileId}/photo` | image bytes, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff` |

**Deviation from the task text:** the task names `GET /api/profiles/search`. Every existing
endpoint in this project lives under `/api/v1/...` and the authentication filter protects all of
`/api/**` except health/register/login, so the endpoint was placed at `/api/v1/profiles/search`
to follow the established convention. Change the `@RequestMapping` in `DiscoveryController` (and
the path in `ProfileSearchApi.kt`) if the unversioned path is really required.

All three require a valid Bearer session. Missing, malformed, expired or revoked sessions return
HTTP 401 from the existing filter.

## Query parameters

`country`, `state`, `city`, `minAge`, `maxAge`, `page` (default 0), `pageSize` (default 20).

- Only these parameters are accepted. Anything else (for example `sort`, `orderBy`) returns 400.
- A parameter supplied more than once returns 400.
- Blank values are treated as "not supplied".
- Text filters: max 100 chars (city 120), no control characters.
- Ages: whole numbers 18-100; `minAge > maxAge` returns 400.
- `page`: 0-10000, otherwise 400. `pageSize`: below 1 returns 400; above 50 is **clamped** to 50 and
  the response reports the effective `pageSize`.
- Error body: existing `{ "code": "SEARCH_VALIDATION_ERROR", "message": "..." }`.
- Parameters are bound as strings and validated by `DiscoveryValidator`, so a malformed number
  produces 400 rather than a framework conversion error.

## Search semantics

- Supplied filters combine with AND; unspecified filters do not restrict the query.
- Country/state/city are **case-insensitive exact matches** (`lower(col) = lower(?)`), trimmed.
  There is no substring or wildcard matching, so `%` and `_` are literal.
- Age is derived from `date_of_birth` at query time and is never stored:
  `age >= N` becomes `date_of_birth <= today - N years`; `age <= M` becomes
  `date_of_birth > today - (M + 1) years`. Bounds are inclusive and birthday-aware.
- Ordering is `created_at DESC, id ASC`. `created_at` never changes on update, so editing a
  profile cannot reshuffle pages, and `id` breaks ties.

## Visibility rules

The schema (V1-V4) has no visibility column, and none was invented. Minimum discovery rule,
enforced in SQL for search, detail and photo alike:

1. the profile belongs to a different user than the authenticated viewer, and
2. `country` and `state_province` are both non-null (excludes legacy profiles that never
   supplied a location).

Consequence: any member with a complete location is discoverable. There is no opt-out yet
(see known limitations).

Search and detail expose only: `profileId`, `displayName`, `age`, `country`, `stateProvince`,
`city`, `photoAvailable`, `photoUrl` (a relative authorized route, or `null`). Never exposed:
phone number, date of birth, user id, email, bio, gender, storage key, filesystem path,
credentials or session data. Search and detail use dedicated records; persistence and owner
models are not reused.

## Pagination behavior

`COUNT(*)` plus `LIMIT ? OFFSET ?` in SQL. Nothing is paginated in Java. A page beyond the last
one returns HTTP 200 with an empty list and correct totals, and the page query is skipped.
`totalPages = ceil(totalItems / pageSize)`.

## Photo authorization behavior

- The photo route takes only a profile UUID (parsed strictly; anything else is 400).
- The storage key is read from the database only, with the same eligibility check as search
  (other user, complete location). Own profile, incomplete profiles and unknown ids return 404.
- `ProfilePhotoService.readStored` now requires the key to match
  `<lowercase-uuid>.(jpg|png)`, resolves it against the storage directory, requires the parent
  to equal the storage directory, and reads with `NOFOLLOW_LINKS`.
- The discovery package contains no `java.io`/`java.nio.file` usage (enforced by
  `verify-discovery.ps1`). Paths and storage keys are never returned to clients.

## Android UI behavior

- Search tab: Country, State / Province, City, Min age, Max age, **Search**, **Clear filters**.
- States: initial, loading, results, empty (not an error), error with **Retry**, and a
  load-more footer (Load more / spinner / inline Retry / end-of-list).
- Client-side validation mirrors the server; the server remains authoritative.
- Results are cards showing photo (when available), name, age and location. Tapping opens the
  detail screen (photo, name, age, country, state/province, city; no phone; no ids). The detail
  screen has loading and error-with-retry states. System Back returns to the results.
- Filters, results and the photo cache are hoisted above the tab, so opening a profile and
  returning keeps the search.
- Pagination: profiles are de-duplicated by id across pages; a `requestId` guard discards stale
  responses (slow earlier search, Clear filters, or a late load-more).
- HTTP 401 from search, detail or photo calls goes to the existing `onAuthenticationLost` flow.
  Token clearing remains solely in `ApiClient`'s interceptor; no mapping was duplicated.
- Photos of other members are untrusted: they are fetched through the authenticated API only,
  size-checked with `inJustDecodeBounds`, refused above 40 megapixels, down-sampled to about
  512 px, and cached in memory only (never on disk).
- No Compose file contains networking code.

## Tests

Backend (JUnit 5), 81 test methods in 5 classes: Validator 18, Service 17, PhotoAccess 10
(one is parameterized with 13 cases), RepositoryIntegration 22, Controller 14.
Requirement coverage: authenticated search, 401 unauthenticated, 401 expired/revoked (filter
level with a faked session lookup, and real SQL for `SessionRepository`), own-profile exclusion,
country/state/city/min/max filters, AND semantics, empty result, pagination, pageSize bound,
private phone absent, authentication fields absent (reflection over DTO components and rendered
JSON), invalid filters rejected, photo containment and traversal.

The repository integration test runs the real V1-V4 migration files in a throwaway schema
(`discovery_it_<random>`) of the database named by `TEST_DB_URL` / `TEST_DB_USERNAME` /
`TEST_DB_PASSWORD` (falling back to `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`, then localhost)
and drops it afterwards. Development data is not touched. It requires a reachable PostgreSQL,
like the existing `ApplicationContextTest`.

Android (JUnit 4), 56 tests in 6 classes: DTO parsing, repository success / 401 / empty /
error mapping / network failure / malformed body / cancellation, filter validation, pagination
state (append, de-dup, stale responses, load-more failure and retry, reset), shared 401
interceptor clears the token (and only on 401), photo sampling limits.

## Verification results

Executed in the sandbox (Linux, JDK 21, PostgreSQL 16, PowerShell 7.4, kotlinc 2.1):

| Check | Result |
| --- | --- |
| `verify.ps1` | PASS (26 PASS lines) |
| `verify-security.ps1` | PASS (9) |
| `verify-migrations.ps1` | PASS (13) |
| `verify-android.ps1` | PASS (35) - see caveat below |
| `verify-discovery.ps1` | PASS (196) |
| `verify-discovery.ps1` negative tests | 17 injected violations (phone in DTO, auth-filter exemption, own-profile exclusion removed, in-memory pagination, page cap removed, SQL concatenation, filesystem access, OTP in code, global cleartext, unapproved cleartext host, networking in Compose, phone on detail, placeholder restored, newline-escape corruption, V4 edited, duplicate migration version) were each detected; an OTP mention in a comment correctly did not fail |
| Backend tests, executed subset | 79/79 invocations pass: Validator, Service, PhotoAccess, RepositoryIntegration against real PostgreSQL 16 with the real V1-V4 schema |
| Mutation checks on integration tests | removing own-profile exclusion, making the age bound inclusive, and making city case-sensitive were each caught; removing the `id` tiebreaker was not detectable at runtime on a tiny table (covered by the static script) |
| Android unit tests, executed subset | 56/56 pass (kotlinc 2.1 + JUnit 4, against hand-written Retrofit/OkHttp/coroutines stubs and real Gson) |
| Compile of `DiscoveryController` and `ApiExceptionHandler` | compiled against thin shims only (names/types, not Spring behavior) |
| `verify-discovery-runtime.ps1` | executed only against a throwaway Python mock API to check the script itself (44/44). **This says nothing about the real backend.** |

Caveat for `verify-android.ps1`: it passed because a Phase 10 debug APK already exists in the
project tree. No APK was built for Phase 11.

**Not executed (must be run on the development machine):**

- `..\android\gradlew.bat test` from `backend` (Gradle was unavailable).
- `DiscoveryControllerTest` (14 tests, standalone MockMvc through the real auth filter and
  exception handler) - needs Spring Framework 7; the sandbox only had Spring 3.x.
- `:app:testDebugUnitTest` and `:app:assembleDebug` (no Android SDK / Compose toolchain).
- Compilation of the Compose files (`SearchScreen.kt`, `ProfileDetailScreen.kt`,
  `SearchScreenModel.kt`, `PhotoDecoding.kt`, `AuthenticatedScreen.kt`). They were checked with a
  mechanical import scan and by reusing only APIs already used elsewhere in the project, not compiled.
- Any test of the real Spring Boot application over HTTP.
- APK existence/size check for a Phase 11 build.
- Any on-device or emulator run of the new screens.

## Runtime HTTP results

**None for the real backend.** After starting the backend, run:

```powershell
.\scripts\verify-discovery-runtime.ps1 -BaseUrl http://localhost:8080
# Optional, for the expiry check: start a second backend (same database) with ~2s session TTL
.\scripts\verify-discovery-runtime.ps1 -BaseUrl http://localhost:8080 -ShortTtlBaseUrl http://localhost:8081
```

It covers authenticated search, each filter, combined filters, pagination, page-size bound,
empty result, invalid input and 405, own-profile exclusion, private-phone absence, detail and
photo authorization, revoked session (logout) and, with `-ShortTtlBaseUrl`, expired session.
It prints only pass/fail labels. It creates `@example.test` users that cannot be deleted through
the API; they are isolated by a per-run marker in their country/state names.
Photo checks require PowerShell 7 (`-Form`); otherwise they are reported as SKIP, not PASS.

## Migration changes

None. No V5 was created; V1-V4 are unmodified. Reasons: the search needs no new columns, and
adding an index without query-plan evidence was explicitly discouraged. Note that
`lower(country) = lower(?)` predicates cannot use the V4 btree index on
`(country, state_province, city)`; at small to moderate data volumes this is a filtered scan.
When volume justifies it, add a `V5__...` functional index on
`lower(country), lower(state_province), lower(city)` **and update `verify.ps1` and
`verify-migrations.ps1`, which currently assert exactly V1-V4.**

## Security evidence

- SQL: every client value, the viewer id, profile id, LIMIT and OFFSET are bound parameters; SQL
  fragments are fixed literals. Verified by tests (a SQL-injection string and `%`/`_` are literals)
  and by `verify-discovery.ps1`.
- Authentication: no new mechanism; discovery paths are not exempted from the shared filter.
- Private data: DTO records contain no phone/credential/storage/user-id/email/DOB fields (reflection
  test, JSON scan test, static script). Phone absence verified against real PostgreSQL rows that
  contain a phone.
- Filesystem: strict key format, containment check, no symlink following, database-resolved keys only.
- Logging: no logging in the discovery package or Android discovery code (static script).
- Android networking: release manifest unchanged; debug cleartext config unchanged and restricted to
  the two approved hosts (static script).
- Client-side robustness: bounded decoding of other members' photos (see UI section).

## Existing issues found in the repository (reported, not all changed)

Fixed in this phase:

1. **Catch-all 500 for client errors.** `ApiExceptionHandler` mapped every non-`AuthException`
   to HTTP 500 with a logged stack trace, including wrong HTTP method, unknown path, malformed JSON
   and argument type mismatches. Added explicit 405/404/400 handlers.

Reported only (not changed, because they are outside Phase 11 or touch validated Phase 10 code):

2. **Unbounded rate-limiter memory** (`InMemoryRateLimiter`): entries are never evicted and the keys
   are attacker-influenced (email, IP), so unique keys grow the map indefinitely.
3. **Login timing side channel** (`AuthService.login`): an unknown email returns before any BCrypt
   work, so response time can reveal whether an email is registered. Registration also returns an
   explicit "already exists" 409.
4. **Startup logs the user out on transient failures** (`AuthRepository.restoreSession`): a network
   error at launch clears the stored token, so being offline at app start forces a fresh login.
5. **Photo upload validates only the file signature.** A tiny file can declare enormous dimensions.
   Discovery now shows other members' photos on viewers' devices, so the Android side decodes with a
   pixel budget. Server-side dimension validation was not added because existing tests upload
   non-decodable placeholder bytes.
6. **Hard-coded LAN IP** `http://<PC-LAN-IP>:8080/` in `ApiClient` main source (works only with the
   debug cleartext configuration; release builds cannot reach it). A build-config field would be cleaner.
7. **`application-test.yml` lives in `src/main/resources`** and sets `flyway.clean-disabled: false`, so
   it ships in the production jar; activating the `test` profile there would permit Flyway clean.
8. **Password length mismatch**: Android accepts up to 128 characters, the server rejects above 72.
9. **Pre-existing uncommitted changes in the working tree** (present before Phase 11 work): 14 tracked
   files differ from `dbb7b48` (line endings/BOM: `CLAUDE.md`, `docker-compose.yml`, `gradlew.bat`,
   `scripts/verify.ps1`, `scripts/verify-migrations.ps1`, and others) and make `git diff --check`
   fail in the sandbox copy. They were left untouched. Review them before committing, and stage only
   Phase 11 files. `CLAUDE.md` also contains escaped Markdown and still says the current phase is 10.

## Known limitations

- No visibility/opt-out setting: every member with a complete location is discoverable.
- No rate limiting on search; an authenticated user can page through all profiles at up to 50 per request.
- Location matching is exact (case-insensitive); there is no fuzzy or partial matching and no
  functional index (see Migration changes).
- Total count and page are separate queries, so totals can shift slightly under concurrent writes; the
  client de-duplicates.
- Search state and photo cache are lost on process death and configuration change (screen rotation).
- The id tiebreaker in ordering is enforced by the static script but cannot be shown to matter by
  a runtime test on a tiny table.
- Bio and gender are not shown in discovery (not requested); adding them is a product decision.

## Intentionally excluded features

OTP, SMS authentication, social login, MFA, password reset, chat, likes, matches, subscriptions,
payments, push notifications, AI recommendations. Phase 12 work was not started.

## Verification still required on the development machine

```powershell
cd V:\Projects\MatrimonyApp
.\scripts\verify.ps1
.\scripts\verify-security.ps1
.\scripts\verify-migrations.ps1
.\scripts\verify-android.ps1
.\scripts\verify-discovery.ps1

cd .\backend
..\android\gradlew.bat test          # needs a reachable PostgreSQL (TEST_DB_URL or DB_URL)

cd ..\android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug

# start the backend, then:
cd ..
.\scripts\verify-discovery-runtime.ps1
```

If a Gradle compile error appears, the most likely places are the Compose files and
`DiscoveryControllerTest` (both unverified by compilation here). When everything passes:

```powershell
git status --short
git diff --check
git diff --cached --check
git add <Phase 11 files only>
git commit -m "phase-11-discovery-search"
```

Do not stage `APPLY_PHASE_07_UI_POLISH.md`, `APPLY_PHASE_08.md`, build directories, APKs,
`backend/data/`, or the unrelated pre-existing modifications listed above.

## Final Git commit

Not created. Baseline remains `dbb7b48`.

