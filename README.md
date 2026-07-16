# Fl-Mane (Android)

Fl-Mane's Android app is a thin native shell around a server-hosted HTML5 app
(`{host}/f1mane/html5/index.html`). Native code covers guest/Google sign-in, local profile editing
(display name + a preset avatar), and a small JavaScript bridge into the WebView. There is no
automated test suite.

## Tech stack (modernized July 2026)

| Component | Version |
|---|---|
| Language | Kotlin 2.4.0 (ViewBinding, coroutines) |
| Gradle / AGP | 9.6.1 / 9.3.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| UI | AndroidX AppCompat 1.7.1, Material 3 (1.14.0), ConstraintLayout 2.2.1 |
| Auth | Firebase Auth (BoM 34.16.0) + Credential Manager 1.6.0 ("Sign in with Google") |
| Networking | OkHttp 5.4.0 + kotlinx-coroutines 1.11.0 (manual `org.json` parsing, no Retrofit) |
| Images | Coil 3.5.0 (`coil-network-okhttp`) |

The code-level migration (Kotlin conversion, AndroidX, OkHttp, Coil, Credential Manager, host-
discovery validation) is complete and builds green — see
[openspec/changes/modernize-android-toolchain/tasks.md](openspec/changes/modernize-android-toolchain/tasks.md)
for the full, honestly-tracked record of what was done and how each step was verified.
**⚠️ The manual on-device regression pass (section 8 of that file) has NOT been run** — no
emulator/device was available in the migration environment. Run it before shipping.

Note on history: `openspec/changes/archive/2026-07-13-modernize-android-relaunch/` is an older,
**never-implemented** version of this migration whose `tasks.md` falsely claims completion. The
real migration is the one above; the archive is kept only as a design record.

## Build / run

- Debug APK: `gradlew.bat assembleDebug` (or `./gradlew assembleDebug` on macOS/Linux)
- Clean: `gradlew.bat clean`
- No lint/test/CI tasks beyond Gradle's defaults; there is no test source set.
- `app/google-services.json` is committed (Firebase config) — expected for this project, not a leak.
- Requires a JDK compatible with the Gradle version in
  [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) — check that
  file for the exact version currently pinned.

## Architecture

Single Gradle module (`app`), package `com.firebaseapp.sowbreira_26fe1.fl_mane`. Five activities, no
fragments/ViewModels/Room — state lives in `SharedPreferences` (`"F1ManePrefs"`, opened via
`getSharedPreferences(LoginActivity.PREFS_NAME, 0)`), holding `host`, `token`, `nome`, `foto`.

Flow: **LoginActivity → MainActivity (WebView) / PerfilActivity → NomeActivity / GridActivity**

- **`LoginActivity`** — entry point (`LAUNCHER`). Fetches the active backend host from
  `https://sowbreira-26fe1.firebaseapp.com/f1mane/host` and persists it to `SharedPreferences`.
  Offers Google sign-in or falls through to guest login. Both flows exchange credentials for a
  backend session token via `criarSessaoVisitante`/`renovarSessaoVisitante`/`criarSessaoGoogle`. On
  success, starts `MainActivity` with the session `token` and `host` as Intent extras. Retries a
  failed login up to 5 times before showing a "server on maintenance" message.
- **`MainActivity`** — hosts the `WebView` that loads
  `{host}/f1mane/html5/index.html?plataforma=android&limpar=S&token={token}`. Exposes a JS bridge as
  `Android` (`showToast(String)`, `exitApp()`). Back button always returns to `LoginActivity`.
- **`PerfilActivity`** — profile screen: change name (→ `NomeActivity`), change avatar (→
  `GridActivity`), sign out, back to `LoginActivity`.
- **`NomeActivity`** — single `EditText` that writes `nome` into `SharedPreferences`.
- **`GridActivity`** / **`ImageAdapter`** — a fixed grid of 12 preset avatars, loaded from
  `https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-{0..11}.png`; selecting one writes
  its URL into `SharedPreferences["foto"]`.

Naming convention: Portuguese method/variable names (`entrarAnonimo`, `entrarAutenticado`,
`preencheFotoNomeUsuario`, `carregaHost`, `voltaPerfil`, `tentaEntrar`) mixed with English class/API
names — consistent with the existing codebase; follow it when touching these files.

## Specs and change tracking

This project uses [OpenSpec](openspec/) to track in-progress and completed changes. See
`openspec/changes/` for active work and `openspec/specs/` for the current capability specs
(`google-sign-in`, `resilient-host-discovery`).
