Every checkbox here starts unchecked and is only marked `[x]` once actually verified with a real
`gradlew.bat` run in this environment — not on the basis of what the archived change's (fabricated)
tasks.md claimed.

## 1. Pre-flight checks

- [ ] 1.1 Verify current stable versions for `androidx.constraintlayout:constraintlayout`,
      `com.google.android.material:material`, and
      `com.google.android.libraries.identity.googleid:googleid` against Maven Central (search this
      session was inconclusive for these three). — Deferred to section 3, not needed yet.
- [x] 1.2 Confirm `platforms;android-37` is installed locally; fall back to `android-36` if not. —
      Neither `cmdline-tools`/`sdkmanager` nor an `android-37` platform were present locally (only
      build-tools 37.0.0, no platform). Used `android-36` — AGP 9.2.0 auto-downloaded "Android SDK
      Platform 36 (revision 2)" and "Build-Tools 36.0.0" itself during the build (license
      auto-accepted by the Gradle plugin, not manually).

## 2. Toolchain upgrade (still Java, existing support libs)

- [x] 2.1 Bumped `gradle/wrapper/gradle-wrapper.properties` to Gradle 9.6.1.
- [x] 2.2 Bumped root `build.gradle`: AGP → 9.2.0, `com.google.gms:google-services` → 4.5.0.
- [x] 2.3 Replaced `jcenter()` with `mavenCentral()` (kept `google()`) in both
      `buildscript.repositories` and `allprojects.repositories`.
- [x] 2.4 In `app/build.gradle`: added `namespace "com.firebaseapp.sowbreira_26fe1.fl_mane"`;
      bumped `compileSdkVersion`/`targetSdkVersion` to 36 (per 1.2); bumped `minSdkVersion` to 24.
- [x] 2.5 In `AndroidManifest.xml`: removed the `package` attribute; added
      `android:exported="true"` to `LoginActivity`, `android:exported="false"` to `MainActivity`,
      `NomeActivity`, `GridActivity`, `PerfilActivity`.
- [x] 2.6 Ran `gradlew.bat clean assembleDebug`. Two real errors hit and fixed (neither assumed from
      the archive):
      1. `getDefaultProguardFile('proguard-android.txt')` no longer supported by AGP 9 (`-dontoptimize`
         blocks R8 optimizations) — switched to `proguard-android-optimize.txt`.
      2. `jp.wasabeef:picasso-transformations:2.1.2` doesn't exist on `mavenCentral()`/`google()` —
         it was jcenter-only. Bumped to `2.4.0` (confirmed present on Maven Central) purely as a
         Phase-1 stopgap; this whole dependency is removed in section 5 when Coil replaces Picasso.
      **Confirmed: `BUILD SUCCESSFUL in 1m 21s`, 36 actionable tasks.**

## 3. AndroidX + Material 3 migration (still Java)

- [x] 3.1 Replaced `com.android.support:appcompat-v7`, `com.android.support:design`,
      `com.android.support.constraint:constraint-layout` in `app/build.gradle` with
      `androidx.appcompat:appcompat:1.7.1`, `com.google.android.material:material:1.14.0`,
      `androidx.constraintlayout:constraintlayout:2.2.1` (both verified as latest stable directly
      against `dl.google.com/android/maven2` metadata, resolving 1.1's open question). Also added
      `android.useAndroidX=true` to `gradle.properties` and updated the stale
      `testInstrumentationRunner` to `androidx.test.runner.AndroidJUnitRunner`.
- [x] 3.2 Updated `android.support.v7.app.AppCompatActivity` / `android.support.annotation.NonNull`
      imports across all 5 activity files to `androidx.*`; grep confirms zero `android.support`
      references remain in `app/src`.
- [x] 3.3 Updated `styles.xml`: `AppTheme` parent → `Theme.Material3.Light.NoActionBar`; the two
      overlay styles → `ThemeOverlay.Material3.*`.
- [x] 3.4 Updated `activity_main.xml`'s root to `androidx.constraintlayout.widget.ConstraintLayout`.
- [x] 3.5 First build failed for real with `checkDebugDuplicateClasses` — old
      `com.android.support:support-compat:26.1.0` pulled transitively by the 2019
      `firebase-auth:16.2.0`/`play-services-auth:16.0.1`, colliding with `androidx.core:core:1.16.0`.
      Fixed at the root (no Jetifier): Firebase moved to BoM `34.16.0` (dropped `firebase-core`,
      which nothing in the code references), `play-services-auth` bumped to `21.6.0` (both versions
      confirmed against `dl.google.com/android/maven2` metadata, not assumed).
      **Confirmed: `BUILD SUCCESSFUL in 1m 34s`, 38 actionable tasks.**
      Note: the archive predicted a similar failure — in this case its guess matched reality, but
      the fix here was derived from the actual error output, and versions were independently
      verified.

## 4. Library swaps (still Java, one at a time)

- [x] 4.1 Added `com.squareup.okhttp3:okhttp:5.4.0` to `app/build.gradle`. **Deviation**:
      `kotlinx-coroutines-android` NOT added here — it's unusable until the Kotlin plugin lands in
      section 5, so adding it now would be a dead dependency; moved to section 5.
- [x] 4.2 Replaced all 4 android-async-http/raw-`URLConnection` call sites in `LoginActivity.java`
      (`carregaHost` — was a raw `Thread`, `entrarAnonimo`, `entrarAutenticado`, photo-existence
      check in `preencheFotoNomeUsuario`) with OkHttp `enqueue` callbacks + manual `org.json`
      parsing. UI work wrapped in `runOnUiThread`; `entrarAutenticado`'s `nome`/`email`/`urlFoto`
      headers use `Headers.Builder.addUnsafeNonAscii` (accented names would be rejected by OkHttp's
      default validation). `carregaHost` keeps its persist-verbatim behavior for now — validation
      is section 6's job. Also made `entrarAutenticado` `void` (its `FirebaseUser` return was never
      used).
- [x] 4.3 Removed `com.loopj.android:android-async-http` from `app/build.gradle`; grep confirms zero
      `loopj`/`msebera`/`AsyncHttp` references remain anywhere under `app/`.
      **Confirmed: `BUILD SUCCESSFUL in 36s` after the swap.**
- [x] 4.4 Added `androidx.credentials:credentials:1.6.0`,
      `androidx.credentials:credentials-play-services-auth:1.6.0`,
      `com.google.android.libraries.identity.googleid:googleid:1.2.0`; removed
      `com.google.android.gms:play-services-auth`. **Version correction**: this file originally said
      1.5.0 based on a web search that turned out to be stale — `dl.google.com/android/maven2`
      metadata (authoritative) shows `credentials` 1.6.0 IS a published stable. (Ironically the
      fabricated archive's 1.6.0 guess was right; the lesson stands — verify against the actual
      repository, not search snippets or the archive.)
- [x] 4.5 Replaced `GoogleSignInClient`/`GoogleSignInOptions`/`startActivityForResult` in
      `LoginActivity.java` with `CredentialManager.getCredentialAsync` + `GetGoogleIdOption`
      (`setFilterByAuthorizedAccounts(false)`, server client ID from
      `R.string.default_web_client_id`). Removed `onActivityResult`/`RC_SIGN_IN`;
      `firebaseAuthWithGoogle` now takes the ID-token string. Cancellation/failure shows a toast and
      leaves the sign-in button visible (per `google-sign-in` spec). **Extra change forced by the
      dependency swap**: `activity_login.xml`'s `com.google.android.gms.common.SignInButton` widget
      replaced with a plain `<Button>` (same `button_google` id, new `sign_in_google` string) —
      after removing `play-services-auth` as a direct dep, `SignInButton` only survives via a
      runtime-scoped transitive dep, which would break ViewBinding's compile-time generated field in
      section 5.
- [x] 4.6 `PerfilActivity.java` sign-out now calls `credentialManager.clearCredentialStateAsync`
      alongside `FirebaseAuth.signOut()`; also dropped the dead
      `linear.removeView(findViewById(new Integer(99)))` call and unused `nome`/`foto` fields while
      rewriting `signOut()`.
- [x] 4.7 Green build confirmed after 4.2/4.3 (`BUILD SUCCESSFUL in 36s`) and after 4.4-4.6
      (`BUILD SUCCESSFUL in 23s`). Grep confirms zero
      `GoogleSignIn`/`SignInButton`/`startActivityForResult`/`onActivityResult`/`RC_SIGN_IN`
      references remain in `app/src`.
- [x] 4.8 Note: Picasso → Coil swap is intentionally deferred to section 5 (Kotlin conversion) — see
      design.md's rationale (Coil 3's API doesn't call cleanly from Java).

## 5. Kotlin conversion

- [x] 5.1 Resolved empirically, in two steps: (1) AGP 9.x's built-in Kotlin support is active with
      NO Kotlin plugin at all — earlier Java-only builds already showed `:app:compileDebugKotlin
      NO-SOURCE`, and the separate `org.jetbrains.kotlin.android` plugin was never applied.
      (2) First Kotlin build failed for real: AGP's embedded Kotlin compiler is **2.2.0**, while
      Coil 3.5.0/coroutines 1.11.0 pull `kotlin-stdlib:2.4.0` whose metadata a 2.2.0 compiler can't
      read ("can read versions up to 2.3.0"). Fixed by adding
      `classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0'` to root `build.gradle` — this
      pins the effective compiler and the error cleared. (The archive claimed the same mechanism;
      here it's confirmed by an actual before/after failing→passing build, not assumed.)
- [x] 5.2 Enabled `buildFeatures { viewBinding true }` in `app/build.gradle`.
- [x] 5.3 Converted `WebAppInterface.java` → `WebAppInterface.kt` (unused `Context` import gone).
- [x] 5.4 Converted `ImageAdapter.java` → `ImageAdapter.kt` with Coil 3
      (`imageView.load(url) { placeholder(...); transformations(CircleCropTransformation()) }`).
      Added `io.coil-kt.coil3:coil:3.5.0` + `coil-network-okhttp:3.5.0`,
      `kotlinx-coroutines-android:1.11.0`, `androidx.lifecycle:lifecycle-runtime-ktx:2.11.0`
      (all verified against Maven Central/Google Maven metadata this session).
- [x] 5.5 Converted `NomeActivity.java` → `NomeActivity.kt` with ViewBinding.
- [x] 5.6 Converted `GridActivity.java` → `GridActivity.kt` with ViewBinding.
- [x] 5.7 Converted `PerfilActivity.java` → `PerfilActivity.kt` with ViewBinding + Coil (the dead
      `findViewById(new Integer(99))` call and unused fields were already dropped in 4.6).
- [x] 5.8 Converted `MainActivity.java` → `MainActivity.kt` with ViewBinding; `Handler()` no-arg
      constructor (deprecated) → `Handler(Looper.getMainLooper())`, same behavior.
- [x] 5.9 Converted `LoginActivity.java` → `LoginActivity.kt` with ViewBinding + Coil. Networking
      now goes through a `suspend fun executa(Request)` helper + a `Call.await()` extension
      (`suspendCancellableCoroutine` over OkHttp `enqueue`), launched via `lifecycleScope.launch` —
      the section-4 `runOnUiThread` wrappers are gone. Sign-in uses Credential Manager's suspend
      `getCredential`. Firebase's `signInWithCredential` keeps its `Task` listener (awaiting it
      would need the extra `kotlinx-coroutines-play-services` artifact — not worth a dependency for
      one call site). Known small behavior notes: photo-URL null check added (old code would NPE if
      a Google account had no photo); `carregaHost` reads "last line" via `trim().lines().last()`
      (Java's `split` drops trailing empties; Kotlin's doesn't — trim replicates the old result).
- [x] 5.10 `gradlew.bat clean assembleDebug` green (`BUILD SUCCESSFUL in 25s`, 42 tasks); `find`
      confirms **0** `.java` files remain. Picasso/`picasso-transformations` removed from
      `app/build.gradle`.
- [x] 5.11 Removed the 5 stray `*-web.png` files under `app/src/main/`, `close_app` string, the
      whole `dimens.xml` (only held unused `fab_margin`), and `snackBarSuccess`/`snackBarError`/
      `snackBarNeutral` colors. **Correction to the earlier exploration report**: `snackBarAlert`
      and `white` are NOT dead — `drawable/selector.xml` (the avatar grid's `listSelector`)
      references both; they stay.

## 6. Host discovery response validation

- [x] 6.1 Added `hostValido()` in `LoginActivity.kt`: fetched value is trimmed, must be non-blank
      and parse as a URI with an `https` scheme before being persisted to
      `SharedPreferences["host"]`; otherwise the previously cached host is left untouched and a
      warning is logged. Matches `specs/resilient-host-discovery/spec.md` (empty body, malformed
      URL, and failed-request scenarios all leave the cache alone — the failed-request path was
      already correct since section 4's OkHttp rewrite).
- [x] 6.2 `BUILD SUCCESSFUL in 10s`.

## 7. Final verification (build-level, everything this environment can check)

- [x] 7.1 Diff review of `app/build.gradle` done. Removed as planned: `com.android.support:*`,
      `com.google.android.gms:play-services-auth` (direct), `com.loopj.android:android-async-http`,
      `com.squareup.picasso:picasso`, `jp.wasabeef:picasso-transformations`, `firebase-core`.
      Present as actually used: appcompat 1.7.1, constraintlayout 2.2.1, material 1.14.0, Firebase
      BoM 34.16.0 (+`firebase-auth`), okhttp 5.4.0, credentials 1.6.0 (×2), googleid 1.2.0,
      coroutines-android 1.11.0, lifecycle-runtime-ktx 2.11.0, coil3 3.5.0 (+`coil-network-okhttp`).
- [x] 7.2 Final `minSdk` 24 / `compileSdk` 36 / `targetSdk` 36 confirmed in `app/build.gradle`
      (36, not 37 — per the 1.2 fallback; no `android-37` platform in the local SDK). Note: root
      `build.gradle` is on AGP **9.3.0** — bumped from 9.2.0 by the user mid-migration; all builds
      from section 5 onward ran against 9.3.0.
- [x] 7.3 Final `gradlew.bat clean assembleDebug`: **`BUILD SUCCESSFUL in 23s`, 42 actionable
      tasks**, zero warnings beyond Gradle-10 deprecation notices from plugins.

## 8. Manual regression pass — REQUIRES A HUMAN WITH A DEVICE/EMULATOR

**Not performed in this environment — no Android emulator or physical device is available here.**
Everything above is verified at the level a build/compile pass can check (compiles, resolves,
matches the spec'd shape); none of it has been exercised at runtime. This is a real gap, not a
formality. Please run these manually via Android Studio before shipping:

- [ ] 8.1 Guest login end-to-end (fresh install, no cached token).
- [ ] 8.2 Guest session renewal (relaunch with a cached token).
- [ ] 8.3 Google sign-in end-to-end: account picker shown, backend session created, profile displayed.
- [ ] 8.4 Sign-out and re-sign-in (previous account not auto-selected).
- [ ] 8.5 Profile name edit (`NomeActivity`) and avatar change (`GridActivity`/`ImageAdapter`) —
      confirm the new avatar shows on the login screen, profile screen, and grid.
- [ ] 8.6 WebView loads the backend HTML5 app with correct host/token query params.
- [ ] 8.7 JS bridge: `Android.showToast()` and `Android.exitApp()` from the web page.
- [ ] 8.8 Back-button behavior from `MainActivity` returns to `LoginActivity`.
- [ ] 8.9 Simulated empty/failed host-discovery response does not clear or corrupt a previously
      working cached host (per `resilient-host-discovery` spec).
