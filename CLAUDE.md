# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Fl-Mane is a small single-module Android app (7 Kotlin classes) that is mostly a WebView shell
around a server-hosted HTML5 app (`f1mane/html5/index.html`). Native code covers guest/Google
sign-in, local profile editing (display name + preset avatar via `SharedPreferences`), and a thin
JS bridge into the WebView. There is no automated test suite.

**Toolchain was modernized in July 2026** (change `modernize-android-toolchain`): Kotlin 2.4.0,
Gradle 9.6.1, AGP 9.3.0, AndroidX/Material 3, OkHttp+coroutines, Coil 3, Credential Manager.
The full verified record of that migration is
[openspec/changes/modernize-android-toolchain/tasks.md](openspec/changes/modernize-android-toolchain/tasks.md).
**⚠️ Its section 8 (manual on-device regression) was never run** — no emulator/device was available
during the migration. Compile-level verification only; treat runtime behavior of every flow as
unverified until a human runs that checklist.

## Build / run

- Build debug APK: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`)
- Clean: `./gradlew clean`
- No lint/test/CI tasks are configured beyond Gradle's defaults; there is no test source set.
- Gradle 9.6.1 (wrapper) requires a modern JDK — JDK 21 (Android Studio's bundled JBR) and JDK 25
  both work. Repositories are `google()` + `mavenCentral()` (no jcenter).
- The Kotlin Gradle Plugin 2.4.0 classpath entry in the root `build.gradle` is **required** even
  though no Kotlin plugin is applied: AGP 9's built-in Kotlin support embeds a 2.2.0 compiler that
  cannot read the 2.4.0 metadata in Coil/coroutines' stdlib without the pin. Removing it breaks
  `compileDebugKotlin`.
- `app/google-services.json` is committed (Firebase config) — this is expected for this project, not a leak to fix.

## Architecture

Single Gradle module (`app`), namespace `com.firebaseapp.sowbreira_26fe1.fl_mane`, all Kotlin with
ViewBinding (`buildFeatures { viewBinding true }`). Five activities, no fragments/ViewModels/Room —
state lives in `SharedPreferences` (`"F1ManePrefs"`, opened via
`getSharedPreferences(LoginActivity.PREFS_NAME, 0)`), holding `host`, `token`, `nome`, `foto`.

Flow: **LoginActivity → MainActivity (WebView) / PerfilActivity → NomeActivity / GridActivity**

- **`LoginActivity`** — entry point (`LAUNCHER`). On create, fetches the active backend host from
  `https://sowbreira-26fe1.firebaseapp.com/f1mane/host` (`carregaHost()`, OkHttp via a
  `Call.await()` suspend extension on `lifecycleScope`; the response is validated by `hostValido()`
  — non-blank, well-formed `https` URI — before overwriting the cached host, per
  `openspec/specs/resilient-host-discovery/spec.md`). Offers Google sign-in via **Credential
  Manager** (`getCredential` + `GetGoogleIdOption`, per `openspec/specs/google-sign-in/spec.md`) or
  falls through to guest login. Guest/authenticated login hits the backend via OkHttp
  (`suspend fun executa(Request)` helper):
  - `criarSessaoVisitante` — new guest session (no cached token)
  - `renovarSessaoVisitante/{token}` — renew guest session
  - `criarSessaoGoogle` — exchange Firebase Google profile (UID/name/email/photo, sent as request
    headers — `addUnsafeNonAscii` for the accent-capable ones) for a backend session
  All three return `{"sessaoCliente": {"token": ...}}`, parsed by hand with `org.json`. On success,
  `irParaMain(token)` starts `MainActivity` with `token`+`host` as Intent extras. Retries failed
  login up to 5 times (`contTentaEntrar`) before showing a "server on maintenance" message.
- **`MainActivity`** — hosts the `WebView` (`f1mane`) that loads
  `{host}/f1mane/html5/index.html?plataforma=android&limpar=S&token={token}`. JS bridge registered
  as `Android` via `WebAppInterface` (`showToast(String)`, `exitApp()` → back to `LoginActivity`).
  Back button always returns to `LoginActivity` (not the browser-style WebView back-history).
- **`PerfilActivity`** — profile screen: change name (→ `NomeActivity`), change avatar (→
  `GridActivity`), sign out (`FirebaseAuth.signOut()` + Credential Manager
  `clearCredentialStateAsync` + clear all `SharedPreferences`), back to `LoginActivity`.
- **`NomeActivity`** — single `EditText` that writes `nome` into `SharedPreferences`.
- **`GridActivity`** / **`ImageAdapter`** — a fixed grid of 12 preset avatars
  (`https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-{0..11}.png`, loaded via
  Coil's `imageView.load { transformations(CircleCropTransformation()) }`); selecting one writes
  its URL into `SharedPreferences["foto"]`.

Key dependencies (see [app/build.gradle](app/build.gradle)): AndroidX appcompat 1.7.1 /
constraintlayout 2.2.1 / Material 1.14.0, Firebase BoM 34.16.0 (`firebase-auth`),
`androidx.credentials` 1.6.0 + `googleid` 1.2.0, OkHttp 5.4.0, kotlinx-coroutines 1.11.0,
lifecycle-runtime-ktx 2.11.0 (for `lifecycleScope`), Coil 3.5.0 + `coil-network-okhttp` (network
fetcher auto-registers via ServiceLoader — no custom `ImageLoader` needed).

Naming convention throughout: Portuguese method/variable names (`entrarAnonimo`, `entrarAutenticado`,
`preencheFotoNomeUsuario`, `carregaHost`, `voltaPerfil`, `tentaEntrar`) mixed with English class/API
names — this is consistent with the existing codebase, follow it when touching these files rather
than translating piecemeal.

## openspec history — read before trusting the archive

`openspec/changes/archive/2026-07-13-modernize-android-relaunch/` is an **older, never-implemented**
version of the modernization whose `tasks.md` falsely marks nearly every step `[x]` complete, with
fabricated version numbers, bug reports, and fixes (merged as PR #1, which only added docs). It was
superseded by `openspec/changes/modernize-android-toolchain/`, which actually performed the
migration in July 2026 with every step verified by a real build before being checked off. If the
two disagree about anything, trust `modernize-android-toolchain` — and ultimately, trust the source
tree over either.
