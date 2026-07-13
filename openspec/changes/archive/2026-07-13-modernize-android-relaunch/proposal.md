## Why

Fl-Mane hasn't been touched since 2019: the build depends on a dead package repository (`jcenter()`), an EOL Android Gradle Plugin, deprecated `com.android.support.*` libraries, an abandoned HTTP client, and a Google Sign-In API Google is actively sunsetting. The app currently cannot be reliably built from a clean checkout, and its `targetSdkVersion` is far below what Google Play requires for new/relaunched apps in 2026. The app is being relaunched on the Play Store, which forces the SDK/toolchain floor to be fixed regardless; this is the moment to also close out the other overdue debt (language, networking, image loading) in one pass rather than re-opening the same files again later.

## What Changes

- **BREAKING**: Bump `minSdkVersion` 21 → 24, `compileSdkVersion`/`targetSdkVersion` 26 → 36. Devices below Android 7.0 can no longer install the app.
- **BREAKING**: Replace Gradle 4.10.1/AGP 3.3.2 with Gradle 8.11+/AGP 9.2.0; replace the `jcenter()` repository with `mavenCentral()` + `google()`.
- **BREAKING**: Migrate all `com.android.support.*` dependencies and code references to AndroidX + Material 3 components.
- **BREAKING**: Replace `GoogleSignInClient` (`play-services-auth`, deprecated by Google) with Credential Manager's "Sign in with Google" for authentication only (no `AuthorizationClient`/extra scopes). The Firebase idToken exchange with the backend is preserved.
- Replace `android-async-http` (abandoned since 2016) with OkHttp + Kotlin coroutines for the app's 3 REST calls (`criarSessaoVisitante`, `renovarSessaoVisitante`, `criarSessaoGoogle`); JSON responses continue to be parsed manually (no Retrofit).
- Replace Picasso + `picasso-transformations` with Coil for avatar loading (`ImageAdapter`, `LoginActivity`, `PerfilActivity`).
- Convert all 7 existing Java classes to Kotlin 2.4.0; nested `AsyncHttpResponseHandler` callbacks become `suspend` functions.
- Add response validation to backend host discovery (`LoginActivity.carregaHost()`): a blank or malformed response no longer overwrites the previously cached host. (Note: host discovery already used HTTPS before this change — an earlier draft of this proposal incorrectly claimed it was plaintext HTTP; that was a misreading of the code, not an actual finding. Corrected here; see `specs/resilient-host-discovery/spec.md`.)

## Capabilities

### New Capabilities
- `google-sign-in`: Authenticating with a Google account via Credential Manager (bottom-sheet UI, auto sign-in, idToken handoff to the backend), replacing the legacy `GoogleSignInClient` intent-based flow. New capability spec because the observable sign-in flow changes, not just its implementation.
- `resilient-host-discovery`: How the app validates the backend host URL it fetches before trusting it. New capability spec because response validation (reject blank/malformed values, keep the last known-good host) is new behavior — transport was already HTTPS beforehand, so this is not a security fix, just a resilience one.

### Modified Capabilities
- None. Guest/anonymous login, profile editing (name + preset avatar), and the WebView↔native JS bridge (`WebAppInterface`) are being re-implemented (Kotlin, new libraries) but their observable behavior is unchanged, so no existing spec requirements change.

## Impact

- **Build system**: `build.gradle`, `app/build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties` (Kotlin plugin, JVM toolchain).
- **All 7 source files** in `app/src/main/java/com/firebaseapp/sowbreira_26fe1/fl_mane/` (converted to `.kt`), plus their layouts under `app/src/main/res/layout/` and `values/styles.xml` (Material 3 theme).
- **Dependencies added**: AndroidX (`androidx.appcompat`, `androidx.constraintlayout`, Material Components), `androidx.credentials` + `credentials-play-services-auth` + `googleid`, `com.squareup.okhttp3:okhttp`, `io.coil-kt.coil3:coil` + `coil-network-okhttp`, `androidx.lifecycle:lifecycle-runtime-ktx` (for `lifecycleScope`), Kotlin stdlib/coroutines, updated Firebase BoM.
- **Dependencies removed**: `com.android.support:*`, `com.google.android.gms:play-services-auth` (GoogleSignInClient usage), `com.loopj.android:android-async-http`, `com.squareup.picasso:*`, `jp.wasabeef:picasso-transformations`.
- **Play Store listing**: minSdk/targetSdk bump changes the device install base; this is expected/accepted as part of the relaunch.
- **No backend changes**: REST endpoints and their contracts (`/f1mane/rest/letsRace/...`) are unchanged; only the client HTTP stack changes. Host discovery already talks to its endpoint over HTTPS; no server-side change needed for `resilient-host-discovery`.
- **No automated tests exist today**; this change does not introduce a test suite (out of scope) but should not make the app harder to test later.
