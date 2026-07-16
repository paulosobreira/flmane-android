## Why

The repository has no README.md, so anyone cloning it (including future Claude Code sessions) has no
entry-point overview of what the app does, how it's built, or how the pieces fit together — they must
either read `CLAUDE.md` (an AI-agent-oriented doc, not a human-facing one) or reverse-engineer the 7
source files. Separately, the build declares `jcenter()` as a dependency repository in
[build.gradle](../../../build.gradle), and jcenter has been shut down — a clean checkout can fail to
resolve dependencies once they age out of Google's/Maven Central's caches. Both are worth fixing now,
independent of the (separately tracked, not-yet-implemented) full Kotlin/AndroidX modernization.

## What Changes

- Add a `README.md` at the repo root documenting: what the app is, the tech stack actually in use,
  build/run instructions, the activity flow, and the WebView/native-bridge architecture.
- Remove `jcenter()` from both repository blocks in [build.gradle](../../../build.gradle), relying on
  `google()` and `mavenCentral()` (added where missing) to resolve the same dependency versions —
  **no dependency version changes**, no Kotlin/AndroidX/Gradle-version changes.
- No other technology upgrades — scope explicitly excludes the Kotlin/AndroidX/Gradle 8 migration
  already speced (but not implemented) in
  `openspec/changes/archive/2026-07-13-modernize-android-relaunch/`.

## Capabilities

### New Capabilities
- `project-documentation`: A root-level README.md giving a human-readable overview of the app's
  purpose, architecture, tech stack, and build/run steps.

### Modified Capabilities
(none — `google-sign-in` and `resilient-host-discovery` specs are unaffected; this change touches
build repository configuration and documentation only, not runtime behavior)

## Impact

- **Affected files**: `README.md` (new), [build.gradle](../../../build.gradle) (repository blocks
  only).
- **Affected systems**: Gradle dependency resolution (repository source, not versions). No app code,
  no runtime behavior, no APIs change.
- **Risk**: Low. Removing `jcenter()` could theoretically break a build if some dependency is only
  ever available on jcenter and never mirrored elsewhere — verified during implementation by running
  a clean `assembleDebug`.
