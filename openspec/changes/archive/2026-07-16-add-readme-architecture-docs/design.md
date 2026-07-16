## Context

The repo has `CLAUDE.md`, written for AI-agent consumption (terse, imperative, assumes the reader
will grep to verify claims). It has no `README.md` for humans landing on the repo (GitHub/GitLab
renders README.md on the repo homepage; CLAUDE.md is not surfaced there). Separately, both
repository blocks in [build.gradle](../../../build.gradle) declare `jcenter()`, which has been
shut down (no new packages, existing ones frozen) — a real but currently-latent build risk.

## Goals / Non-Goals

**Goals:**
- Give humans a fast, accurate mental model of the app via README.md: what it is, how it's built,
  how the pieces connect.
- Remove the jcenter() build risk by switching to still-live repositories, with zero change to
  resolved dependency versions.

**Non-Goals:**
- No Kotlin/AndroidX/Gradle-version/dependency-version migration. That is
  `openspec/changes/archive/2026-07-13-modernize-android-relaunch/` — a separate, much larger,
  not-yet-implemented change.
- No changes to app behavior, activities, or the JS bridge.
- No CI/lint/test scaffolding — CLAUDE.md is explicit that none exists today and this change
  doesn't add any.

## Decisions

- **README content sourced from CLAUDE.md's architecture section, not re-derived independently.**
  CLAUDE.md was itself produced from reading the actual source tree (see its "openspec history"
  section correcting the record against `openspec/`'s aspirational docs), so it's the most reliable
  existing description of current-state architecture. The README will restate it in a human-facing
  tone (less imperative, no "you MUST" framing) rather than re-reading all 7 source files from
  scratch — cheaper and equally accurate, verified by spot-checking file paths/class names still
  exist.
- **README explicitly does not claim the openspec-documented modernization happened.** CLAUDE.md
  had to correct this once already (PR #1 merged only docs, not code) — the README repeats that
  caveat so a human skimming the repo doesn't get misled by `openspec/changes/archive/` claiming
  `tasks.md` is `[x]` complete.
- **jcenter() removal: `google()` + `mavenCentral()`, order preserved before jcenter's old
  position.** All dependencies in `app/build.gradle` (support library, Firebase, play-services,
  android-async-http, Picasso, picasso-transformations) are published to Maven Central or
  google's repo; none are jcenter-exclusive as of writing. Alternative considered: swap in
  `jcenter()`'s community mirror (`plugins.gradle.org`'s legacy proxy) — rejected, since Maven
  Central mirrors the same artifacts without depending on a third-party proxy's continued uptime.
- **Verify via a clean `assembleDebug`, not by inspecting POM files by hand.** Fastest and most
  direct signal that resolution still works after the repository swap.

## Risks / Trade-offs

- [Risk] Some dependency version is only ever mirrored on jcenter and not on Maven Central/google.
  → Mitigation: run `gradlew.bat clean assembleDebug` after the change; if resolution fails for a
  specific artifact, that's caught immediately and can be addressed (e.g., pin a slightly different
  version) before merging.
- [Risk] README drifts from CLAUDE.md over time (two documents describing the same architecture).
  → Mitigation: keep the README high-level (overview, not exhaustive per-class detail) so it needs
  updates less often than CLAUDE.md's finer-grained notes; not solved structurally in this change.

## Migration Plan

1. Write `README.md`.
2. Edit `build.gradle` to drop `jcenter()` from both repository blocks, add `mavenCentral()`.
3. Run `gradlew.bat clean assembleDebug` to confirm dependency resolution still succeeds.
4. No rollback complexity — both changes are additive/substitutive and easily revertible via git if
   the build breaks.

## Open Questions

None outstanding — scope was narrowed via user confirmation to "fix build risks only, no framework
migration."
