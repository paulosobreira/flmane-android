## Context

Fl-Mane is a small single-module Android app (7 Java classes), mostly a WebView shell around a
server-hosted HTML5 app. It hasn't been touched since 2019: Gradle 4.10.1, AGP 3.3.2,
`com.android.support.*`, `android-async-http` (abandoned 2016), Picasso, and legacy
`GoogleSignInClient`. Gradle 4.10.1 only supports up to Java 10 — it cannot run on any JDK currently
installed on the development machine (system Java 25.0.2, Android Studio's bundled JBR 21.0.10),
which is the immediate trigger for this change: Android Studio cannot sync the project at all.

A prior change, [archived at 2026-07-13-modernize-android-relaunch](../archive/2026-07-13-modernize-android-relaunch/),
already designed this exact migration and its `tasks.md` claims full completion with specific
version numbers and specific bugs hit and fixed. **That completion record is fiction** — verified
this session by reading the actual source tree, which is unchanged from the pre-migration state.
The archived `proposal.md`/`design.md` reasoning is reused here where it still holds, but every
version number is independently re-verified (one drift already caught: `androidx.credentials`
stable is 1.5.0, not the archive's claimed 1.6.0), and `tasks.md` here starts genuinely unchecked.

`openspec/specs/google-sign-in/spec.md` and `openspec/specs/resilient-host-discovery/spec.md`
already exist (written by the prior change before its implementation was fabricated) and correctly
describe the target behavior; this change's job is to make the code match them, not to change what
they say.

## Goals / Non-Goals

**Goals:**
- A clean-checkout build that works on the currently-installed JDK, with no dead repositories.
- `minSdk` 24, `compileSdk`/`targetSdk` at the latest available stable Android API level.
- Replace deprecated/abandoned dependencies with maintained equivalents.
- Full Kotlin codebase with ViewBinding.
- Implementation that actually satisfies the existing `google-sign-in` and
  `resilient-host-discovery` specs (currently it does not).

**Non-Goals:**
- No product feature work or UI redesign.
- No automated test suite (none exists today; out of scope, should not make future testing harder).
- No backend/server-side changes.
- No Retrofit or typed networking layer — 3-4 ad hoc endpoints don't justify it.
- No `AuthorizationClient` / additional Google scopes — Credential Manager for authentication only.
- No multi-module restructuring or `SharedPreferences` → DataStore migration.
- No certificate pinning.
- Trusting any version number, bug description, or "already done" claim from the archived change's
  `tasks.md` without independently re-verifying it in this environment.

## Decisions

**Big-jump toolchain upgrade, not staged.** Same rationale as the archive: for a 7-file app with no
custom Gradle logic, staged AGP-major upgrades cost more than they save. Toolchain breakage is kept
separate from library/language breakage via sequencing (see Migration Plan), even though it all
ships together.

**Every version number is re-verified via Maven Central/`google()`/official release notes in this
session, not copied from the archive.** Current verified versions (July 2026):

| Component | Version |
|---|---|
| Gradle | 9.6.1 |
| AGP | 9.2.0 |
| Kotlin | 2.4.0 |
| `com.google.gms:google-services` | 4.5.0 |
| Firebase BoM | 34.16.0 |
| `androidx.credentials` + `credentials-play-services-auth` | 1.5.0 |
| OkHttp | 5.4.0 |
| `kotlinx-coroutines-android` | 1.11.0 |
| Coil (`io.coil-kt.coil3:coil` + `coil-network-okhttp`) | 3.5.0 |
| `androidx.appcompat:appcompat` | 1.7.1 |
| `compileSdk`/`targetSdk` | 37, falling back to 36 if `platforms;android-37` isn't installed locally |
| `minSdk` | 24 |

`androidx.constraintlayout:constraintlayout`, `com.google.android.material:material`, and
`com.google.android.libraries.identity.googleid:googleid` versions were inconclusive from search and
must be checked against Maven Central immediately before use in Phase 1/2/3 tasks — do not guess.

**Repositories: `jcenter()` → `mavenCentral()` + `google()`.**

**AndroidX migration done by hand alongside the Kotlin conversion**, not via Jetifier — every file
is being rewritten anyway, so mapping `android.support.*` → `androidx.*` by hand costs nothing extra
and avoids a long-lived Jetifier flag.

**Credential Manager for authentication only.** The app only ever used the Google idToken plus
basic profile — never Drive or other scopes. `google-services.json` already contains a `client_type:
3` (Web Client ID) entry, confirmed present, so no new Google Cloud Console client is needed.

**Networking: OkHttp + coroutines, manual `org.json` parsing — no Retrofit.** 4 call sites, each
returning one ad hoc JSON object; not enough surface to justify a typed layer.

**Coil swap happens during Kotlin conversion, not before.** Coil 3's API leans on Kotlin extension
functions (`imageView.load(url) { }`) that don't call cleanly from Java. Doing the Coil swap file-by-
file as each Activity converts to Kotlin avoids rewriting the image-loading code twice.

**Kotlin conversion in one pass across all 7 files**, after toolchain/AndroidX/library-swap phases
are already green, so language conversion isn't fighting build-system issues simultaneously.

**`SharedPreferences` persistence model (`F1ManePrefs`) is unchanged** — small, stable keyspace.

**Host discovery: add response validation only, no transport change.** `carregaHost()` already
fetches over HTTPS today (re-confirmed by reading the current code this session) — the gap is that
the fetched value is persisted verbatim with no validation.

## Risks / Trade-offs

- [Risk] `minSdk` 24 drops devices below Android 7.0 → [Mitigation] Negligible remaining market
  share by 2026; consistent with the already-existing (if fabricated-as-complete) prior decision.
- [Risk] Toolchain, AndroidX, library swaps, and Kotlin conversion landing together makes a build
  failure hard to attribute → [Mitigation] Sequence tasks so each axis reaches a green
  `gradlew.bat assembleDebug` before the next starts.
- [Risk] Credential Manager depends on the Firebase project's Web Client ID / SHA-1 config matching
  `google-services.json` → [Mitigation] Web Client ID entry confirmed present this session; guest
  login remains a working fallback throughout.
- [Risk] No automated tests, and **no emulator/device is available in this implementation
  environment** → [Mitigation] Every phase gets a `gradlew.bat assembleDebug` build-level check;
  `tasks.md`'s final manual-regression section is explicitly left unchecked and flagged as requiring
  a human pass in Android Studio — this is the same gap the archive correctly identified but then
  contradicted by marking everything else done anyway.
- [Risk] Any AGP-9-specific build error (manifest requirements, ProGuard file rename, DSL syntax
  changes) that the archive's fabricated log "already fixed" may or may not actually occur, and may
  not match the archive's guess → [Mitigation] Treat every such claim as unverified; diagnose and
  fix whatever actually surfaces when the build is actually run.

## Migration Plan

1. **Toolchain**: Gradle 9.6.1, AGP 9.2.0, `mavenCentral()`+`google()`, `compileSdk`/`targetSdk`
   37/36, `minSdk` 24, `namespace` in `app/build.gradle`, `android:exported` on all activities — green
   build with existing Java + old support-lib code first.
2. **AndroidX + Material 3**: migrate `com.android.support.*` references and theme, still in Java —
   green build again.
3. **Library swaps**, one at a time, still in Java: OkHttp+coroutines replacing `android-async-http`;
   Credential Manager replacing `GoogleSignInClient`. (Coil deferred to step 4.) Manually verify each
   affected flow after its swap, to the extent possible without a device (compile-level + code review;
   full manual verification happens in step 6).
4. **Kotlin conversion**: all 7 classes in one pass, introducing ViewBinding and swapping in Coil.
5. **Host-discovery response validation** in `carregaHost()`.
6. **Manual regression pass** (human, in Android Studio with a device/emulator): guest login, Google
   login, profile name/avatar edit, WebView load with token, JS bridge, sign-out, back button.

Rollback: single relaunch build, no live traffic, no `SharedPreferences` schema or backend contract
changes — rollback is "don't ship," no data migration to reverse.

## Open Questions

None blocking — scope and sequencing were confirmed with the user before this document was written
(plan-mode approval). Remaining unknowns are version-pin verifications called out above, to be
resolved at the point each dependency is actually added, not guessed in advance.
