## Context

Fl-Mane is a small single-module Android app (7 Java classes) that is mostly a WebView shell around a server-hosted HTML5 app; native code covers guest/Google login, local profile editing (name + preset avatar via `SharedPreferences`), and a thin JS bridge (`WebAppInterface`). It hasn't been touched since 2019: the build depends on the now-dead `jcenter()` repository, AGP 3.3.2, `com.android.support.*`, `android-async-http` (abandoned 2016), Picasso, and `GoogleSignInClient` (deprecated by Google in favor of Credential Manager). There is no automated test suite. The app is being relaunched on the Play Store, which forces a `targetSdkVersion` bump regardless of anything else — that's the trigger for doing the rest of the overdue modernization in the same pass rather than reopening these files again later. The user explicitly chose a "big jump" straight to current toolchain/library versions over a staged migration, given the codebase's small size and lack of custom build logic.

## Goals / Non-Goals

**Goals:**
- A clean-checkout build that works on current Gradle/AGP/JDK, with no dependency on dead repositories.
- `compileSdk`/`targetSdk` 36 and `minSdk` 24, meeting Play Store's 2026 relaunch requirements.
- Replace deprecated/abandoned dependencies (`com.android.support.*`, `GoogleSignInClient`, `android-async-http`, Picasso) with maintained equivalents (AndroidX/Material 3, Credential Manager, OkHttp+coroutines, Coil).
- Full Kotlin codebase.
- Make host discovery resilient to blank/malformed responses (it already used HTTPS; the gap is response validation, not transport).

**Non-Goals:**
- No product feature work or UI redesign — Material 3's visual shift is a side effect of the AndroidX migration, not a redesign initiative.
- No automated test suite (flagged as future work; this change should not make future testing harder).
- No backend/server-side changes. Host discovery already talks HTTPS; nothing server-side to change.
- No Retrofit or typed networking layer — deliberately minimal OkHttp usage for 3 endpoints.
- No `AuthorizationClient` / additional Google API scopes — Credential Manager is used for authentication only, matching what `GoogleSignInClient` was actually used for here.
- No multi-module restructuring or `SharedPreferences` → DataStore migration — out of scope for this change.
- No certificate pinning for the host-discovery endpoint — out of scope, unrelated to the response-validation gap being closed here.

## Decisions

**Big-jump toolchain upgrade, not staged.** Gradle 4.10.1/AGP 3.3.2 → Gradle 8.11+/AGP 9.2.0 directly, instead of stepping through intermediate AGP majors. For a 7-file app with no custom Gradle logic, staged upgrades cost more time than they save. Mitigated by sequencing the *rest* of the migration (see Migration Plan) so toolchain breakage isn't tangled up with library/language breakage.

**Repositories: `jcenter()` → `mavenCentral()` + `google()`.** Every dependency kept post-migration (AndroidX, Material, Firebase, OkHttp, Coil, Credential Manager) resolves from these. Libraries being removed (`android-async-http`, Picasso) don't need to resolve going forward.

**AndroidX migration done by hand alongside the Kotlin conversion**, not via Android Studio's automated refactor + Jetifier compatibility shim. Since every file is being rewritten in Kotlin anyway, mapping `android.support.v7.app.AppCompatActivity` → `androidx.appcompat.app.AppCompatActivity` etc. by hand costs nothing extra and avoids leaving a Jetifier flag in the build long-term.

**Credential Manager for authentication only, no `AuthorizationClient`.** The app only ever used the Google idToken plus basic profile (name/email/photo) — it never requested Drive or other scopes. This keeps the migration to a single new API surface (`androidx.credentials` + Google ID library) instead of also adopting the separate authorization API.

**Guest/anonymous login is unaffected by Credential Manager.** `criarSessaoVisitante` / `renovarSessaoVisitante` never touched Google Sign-In APIs — only the `criarSessaoGoogle` path changes.

**Networking: OkHttp + coroutines, manual JSON parsing — no Retrofit.** 3 endpoints, each returning one ad hoc JSON object. Retrofit's typed-interface-plus-converter overhead doesn't pay for itself at this scale; `org.json` (already on the Android platform) is enough. Revisit if the REST surface grows materially.

**Image loading: Coil over Picasso.** Kotlin-first, coroutine-native, actively maintained; consistent with the rest of the coroutine-based modernization. `jp.wasabeef.picasso-transformations`'s `CropCircleTransformation` is replaced by Coil's built-in circle-crop transformation.

**Kotlin conversion done in one pass across all 7 files**, not interleaved file-by-file over time. The codebase is small enough that partial conversion doesn't reduce risk, only prolongs a period of mixed-language inconsistency. ViewBinding replaces manual `findViewById` casts as a natural, no-extra-dependency companion to the conversion (every Activity currently does raw `findViewById` + casts).

**`SharedPreferences` persistence model (`F1ManePrefs`) is unchanged.** Small, stable keyspace (`host`, `token`, `nome`, `foto`); a DataStore migration isn't justified by this change's scope.

**Host discovery: add response validation, no transport change.** `carregaHost()` already fetched over HTTPS before this change (an earlier draft of this design incorrectly claimed it was plaintext HTTP — corrected). The actual gap: the fetched value was persisted verbatim with no validation. Now only a non-blank, well-formed HTTPS URL gets persisted, so a flaky/empty response can't silently corrupt the cached host that guest/Google login and the WebView all depend on; on any other response, the previously cached host is left untouched.

## Risks / Trade-offs

- [Risk] `minSdk` 24 drops devices below Android 7.0 → [Mitigation] Negligible remaining market share in 2026; explicitly accepted by the user for the relaunch.
- [Risk] Toolchain, AndroidX, library swaps, and Kotlin conversion landing together makes a single build failure hard to attribute → [Mitigation] Sequence tasks so each axis reaches a green build before the next starts (toolchain → AndroidX → libraries → Kotlin), even though they ship in one release.
- [Risk] Credential Manager's Google Sign-In depends on the Firebase project's Web Client ID / SHA-1 config matching `google-services.json` — misconfiguration breaks login silently → [Mitigation] Verify Firebase console config as part of the auth task; guest/anonymous login remains a working fallback throughout.
- [Risk] Dropping `AuthorizationClient` means any future need for extra Google scopes requires another migration → [Mitigation] Accepted; not used today, documented here as a known limitation.
- [Risk] No automated tests — every flow above is validated manually → [Mitigation] tasks.md includes explicit manual verification per flow (guest login, Google login, profile edit, avatar picker, WebView load, JS bridge) before the change is considered done.
- [Risk] Stricter URL validation in `carregaHost()` could reject a legitimately-formatted host the backend starts returning in a format not anticipated here → [Mitigation] Validation only checks for a non-blank value with an `https` scheme, matching the endpoint's actual current output; verified via a live `curl` against the endpoint before implementing.

## Migration Plan

Single relaunch build, no live rollout/rollback concerns — but internal sequencing matters for isolating failures:

1. **Toolchain**: Gradle 8.11+/AGP 9.2.0, `mavenCentral()`+`google()`, `compileSdk`/`targetSdk` 36, `minSdk` 24 — get a green build with the existing Java + old support-lib code first.
2. **AndroidX + Material 3**: migrate `com.android.support.*` references and theme, still in Java — green build again.
3. **Library swaps**, one at a time, still in Java: OkHttp+coroutines replacing `android-async-http`; Coil replacing Picasso; Credential Manager replacing `GoogleSignInClient`. Manually verify each affected flow after its swap.
4. **Kotlin conversion**: all 7 classes, in one pass, on top of the already-modernized Java code (so conversion isn't fighting toolchain/library issues at the same time). Introduce ViewBinding here.
5. **Host-discovery response validation** in `carregaHost()` (no transport change — it was already HTTPS).
6. **Manual regression pass**: guest login, Google login, profile name/avatar edit, WebView load with token, JS bridge (`showToast`/`exitApp`), sign-out.

Rollback: since this ships as a single relaunch build with no live traffic yet and no `SharedPreferences` schema or backend contract changes, rollback is simply "don't ship" — no data migration to reverse.

## Open Questions

- Does the current Firebase project have a Web Client ID valid for Credential Manager's `GetGoogleIdOption`, or does Firebase console config need updating first?
- Confirm ViewBinding is in scope: it's a natural, zero-extra-dependency fit for the Kotlin conversion, but wasn't explicitly requested — flagging before tasks.md locks it in.

(Resolved) Whether host discovery already used HTTPS: yes — confirmed by reading `carregaHost()` directly. An earlier draft of this design incorrectly stated it used plaintext HTTP; corrected throughout this document once caught during section 6 implementation.
