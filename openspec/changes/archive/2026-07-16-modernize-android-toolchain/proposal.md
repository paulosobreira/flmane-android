## Why

The project's toolchain (Gradle 4.10.1, AGP 3.3.2, Java, `com.android.support.*`, `android-async-http`,
Picasso, legacy `GoogleSignInClient`) is 2019-era and can no longer sync in Android Studio against
any currently-installed JDK (system Java 25, Android Studio's bundled JBR 21 — Gradle 4.10.1 tops
out at Java 10). Separately, `openspec/specs/google-sign-in/spec.md` and
`openspec/specs/resilient-host-discovery/spec.md` already describe target behavior (Credential
Manager sign-in, validated host discovery) that a prior, never-executed change
([openspec/changes/archive/2026-07-13-modernize-android-relaunch/](../archive/2026-07-13-modernize-android-relaunch/))
was supposed to implement but did not — the actual source tree is still on the legacy stack those
specs describe replacing. This change does the real work: fixes the Java-version build blocker and
brings the implementation in line with what's already specified, using the archived change's design
reasoning as a reference (re-verified, not trusted) rather than its fabricated completion log.

## What Changes

- **BREAKING**: Bump `minSdkVersion` 21 → 24; devices below Android 7.0 can no longer install the app.
- **BREAKING**: Replace Gradle 4.10.1/AGP 3.3.2 with Gradle 9.6.1/AGP 9.2.0; drop the dead `jcenter()`
  repository for `mavenCentral()` + `google()`.
- **BREAKING**: Migrate all `com.android.support.*` dependencies/code to AndroidX + Material 3.
- Replace `GoogleSignInClient`/`startActivityForResult` with Credential Manager's "Sign in with
  Google" in `LoginActivity` (sign-in) and `PerfilActivity` (sign-out) — implements the
  already-existing `google-sign-in` spec; no spec text changes.
- Replace `android-async-http` (abandoned since 2016) with OkHttp + Kotlin coroutines for the app's
  4 REST/network call sites, all in `LoginActivity`.
- Replace Picasso + `picasso-transformations` with Coil 3 for avatar loading.
- Convert all 7 existing Java classes to Kotlin, introducing ViewBinding in place of manual
  `findViewById` + casts.
- Add response validation to `LoginActivity.carregaHost()` so a blank/malformed discovery response
  no longer overwrites the previously cached host — implements the already-existing
  `resilient-host-discovery` spec; no spec text changes.
- Minor dead-code cleanup incidental to the full rewrite (unused imports/fields, a dead
  `findViewById(new Integer(99))` call, 5 stray unreferenced image files outside `res/`, unused
  string/dimen/color resources) — not new scope, just free cleanup while every file is touched anyway.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `google-sign-in`, `resilient-host-discovery`: no requirement text changes — both specs already
  describe the target behavior this change implements. Delta files are included only because the
  source code is currently non-conformant with them (still `GoogleSignInClient`, still unvalidated
  host persistence) and this change is what brings the implementation into compliance. Guest/
  anonymous login, profile editing, and the WebView↔native JS bridge are re-implemented in Kotlin
  with new libraries but their observable behavior is unchanged, so no spec applies/changes there.

## Impact

- **Build system**: `build.gradle`, `app/build.gradle`, `gradle/wrapper/gradle-wrapper.properties`,
  `gradle.properties`, `AndroidManifest.xml`.
- **All 7 source files** in `app/src/main/java/com/firebaseapp/sowbreira_26fe1/fl_mane/` (converted
  to `.kt`), their layouts under `app/src/main/res/layout/`, and `values/styles.xml` (Material 3 theme).
- **Dependencies added**: AndroidX (appcompat, constraintlayout, Material), `androidx.credentials` +
  `credentials-play-services-auth` + `googleid`, OkHttp, Coil 3, Kotlin stdlib/coroutines, updated
  Firebase BoM. Exact versions verified against Maven Central/`google()` at implementation time
  (see design.md) rather than copied from the archived change, which contained at least one
  confirmed-wrong version number.
- **Dependencies removed**: `com.android.support:*`, `com.google.android.gms:play-services-auth`
  (direct dep), `com.loopj.android:android-async-http`, `com.squareup.picasso:*`,
  `jp.wasabeef:picasso-transformations`.
- **Play Store listing**: minSdk/targetSdk bump changes the device install base — accepted as part
  of the relaunch this change originates from.
- **No backend changes.** REST endpoints and contracts (`/f1mane/rest/letsRace/...`) are unchanged;
  only the client HTTP stack changes. Host discovery already talks HTTPS today.
- **No automated tests exist**; this change does not introduce a test suite.
- **No emulator/device available in the implementing environment** — Phase 6 manual regression
  (guest login, Google login, profile edit, avatar picker, WebView, JS bridge, back button) must be
  run by a human in Android Studio before this change is considered complete; `tasks.md` tracks this
  explicitly rather than marking it done.
