---
name: dependency-update
description: 'Update library versions or Gradle plugin versions in the PictureTriage project. Covers the full safe update workflow: check for new versions, edit build.gradle.kts, regenerate the lockfile, and verify the build. Triggers: update dependency, upgrade library, bump version, check for updates, new version.'
---

# Dependency Update Workflow

Updating a dependency requires **four steps** in order. Skipping any step risks broken builds or stale lockfile entries causing CI failures.

---

## Step 1 — Check for Available Updates

```
./gradlew dependencyUpdates --no-parallel
```

The report is printed to the console and saved to `build/dependencyUpdates/report.txt`.

Only **stable** version upgrades are shown — pre-release and release-candidate versions are automatically filtered out by the build configuration. You do not need to manually exclude them.

---

## Step 2 — Edit `build.gradle.kts`

Locate the dependency or plugin version and update it in place.

**Application dependency** (in the `dependencies` block):
```kotlin
implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.1")  // → new version
```

**JavaFX library version** (in the `javafx` block):
```kotlin
javafx {
    version = "25.0.1"   // → new version
    ...
}
```

**Gradle plugin version** (in the `plugins` block):
```kotlin
id("org.openjfx.javafxplugin") version "0.1.0"           // → new version
id("com.github.ben-manes.versions") version "0.53.0"      // → new version
```

Only change the version string. Do not restructure the file.

---

## Step 3 — Regenerate the Lockfile

The project uses dependency locking for reproducible builds. After any version change, the lockfile **must** be regenerated:

```
./gradlew dependencies --write-locks --no-parallel
```

This rewrites `gradle.lockfile`. Stage the updated lockfile together with `build.gradle.kts` in the same commit.

---

## Step 4 — Verify the Build

```
./gradlew build
```

A successful build confirms the new version resolves, compiles, and passes all tests. If the build fails:
- Check the Gradle error output for resolution conflicts or API changes
- Revert the version in `build.gradle.kts` and re-run step 3 if the conflict cannot be resolved quickly

---

## Commit

Include both changed files in the commit:
- `build.gradle.kts` — version bump
- `gradle.lockfile` — regenerated pins

Suggested commit message format:
```
Project: Bump <library-name> from <old> to <new>
```

Example:
```
Project: Bump twelvemonkeys imageio-webp from 3.12.0 to 3.13.1
```
