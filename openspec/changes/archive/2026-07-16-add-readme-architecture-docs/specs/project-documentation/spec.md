## ADDED Requirements

### Requirement: Root README describes the app and its architecture
The repository SHALL contain a `README.md` at its root that describes, in human-readable prose:
what the app is and does, the tech stack actually in use (language, key libraries, build tooling
and required versions), the activity flow, and the native/WebView boundary.

#### Scenario: Developer opens the repo for the first time
- **WHEN** a developer clones the repository and opens `README.md`
- **THEN** they can determine what the app does, which language and framework versions it targets,
  how to build a debug APK, and how the activities/WebView fit together, without reading source
  files first

### Requirement: README does not overstate migration status
The README SHALL NOT claim that the Kotlin/AndroidX/Gradle-8 modernization described under
`openspec/changes/archive/2026-07-13-modernize-android-relaunch/` has been applied to the source
tree, since it has not.

#### Scenario: Developer cross-checks README against openspec archive
- **WHEN** a developer reads both `README.md` and the archived modernization change's `tasks.md`
  (which marks nearly all steps complete)
- **THEN** the README makes clear that the archived change only merged documentation, and that the
  app remains on the legacy Java/Gradle-4.10.1/AGP-3.3.2 toolchain

### Requirement: Build dependency repositories do not rely on jcenter
Gradle repository declarations in the project SHALL NOT include `jcenter()`, since jcenter is
shut down and poses a dependency-resolution risk on a clean checkout.

#### Scenario: Clean checkout resolves dependencies
- **WHEN** a developer runs `gradlew.bat clean assembleDebug` on a machine with no prior Gradle
  cache for this project
- **THEN** all declared dependencies resolve successfully from `google()` and `mavenCentral()`
  without requiring `jcenter()`

#### Scenario: No dependency version changes
- **WHEN** the repository blocks in `build.gradle` are updated to remove `jcenter()`
- **THEN** every dependency version declared in `app/build.gradle` and the root `build.gradle`
  remains unchanged
