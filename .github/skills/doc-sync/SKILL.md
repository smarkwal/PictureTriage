---
name: doc-sync
description: 'Review and update project documentation for accuracy against the current codebase. Use when adding a class, renaming a type, changing workflow phases, updating dependencies, or modifying the package structure. Covers README.md, ARCHITECTURE.md, copilot-instructions.md, and SKILL.md files. Triggers: review docs, update documentation, update README, review ARCHITECTURE, sync docs, documentation review, doc sync, stale docs, inaccurate docs, docs out of date.'
---

# Documentation Sync

Review all project documentation for accuracy against the current source code, then apply corrections in place. Do not rewrite sections that are still accurate — only fix what is wrong or missing.

## Documents in Scope

| File | Primary risk of drift |
|------|----------------------|
| `README.md` | Dependency versions, feature list |
| `ARCHITECTURE.md` | Class names, responsibilities, data flow |
| `copilot-instructions.md` | Versions, package structure, workflow phases, image formats |
| `.github/skills/*/SKILL.md` | File paths, class names, Gradle commands |

---

## Step 1 — Establish Ground Truth

Collect the authoritative facts from source before touching any document.

### 1a — Dependency versions

Read `build.gradle.kts` and note the current values for:
- Java version (toolchain `languageVersion`)
- JavaFX version (`javafx { version = "..." }`)
- All library dependencies in the `dependencies` block (group, artifact, version)
- All Gradle plugin versions in the `plugins` block

### 1b — Source class inventory

List every `.java` file under `src/main/java/` to get the complete set of current class names and their packages:

```
find src/main/java -name "*.java" | sort
```

### 1c — Supported image formats

Read `ImageScannerService.java` and extract the exact set of recognized file extensions. The scanner is the single source of truth for what formats are supported.

### 1d — Workflow phases

Count the phase controllers: `FolderSelectionController` + one controller per phase + any results/summary step. Read `AppCoordinator.java` to confirm the actual phase transition sequence.

### 1e — Package structure

Read the directory listing of `src/main/java/net/markwalder/picturetriage/` to get the current sub-package list.

---

## Step 2 — Review and Update `ARCHITECTURE.md`

This file is highest-risk because it names every class and describes its responsibility.

### 2a — Verify named classes exist

For each class name mentioned in `ARCHITECTURE.md`, confirm it exists in the source inventory from Step 1b. If a class has been renamed or removed, update or delete its entry.

### 2b — Verify new classes are documented

For each class in the source inventory, check whether it appears in `ARCHITECTURE.md`. Any class that is not a test class and not an inner class should have an entry. Add brief entries for missing classes in the appropriate layer section.

### 2c — Verify layer diagram

Check the ASCII diagram at the top. Confirm it still accurately reflects the layering — especially if a new layer or component has been introduced.

### 2d — Verify data flow

Read the data flow section at the bottom. Check that the service names and return types match the current service implementations. Update if signatures or flow has changed.

---

## Step 3 — Review and Update `README.md`

### 3a — Feature list

Check each bullet in the Features section against the implemented phases and capabilities. Add bullets for new features; remove bullets for removed ones.

### 3b — Java version

Compare the stated Java requirement against the toolchain version from Step 1a.

### 3c — Dependency versions

Compare every dependency version listed in the README against the values from Step 1a. Update any version string that has changed.

### 3d — Build and run commands

Verify the documented Gradle commands (`./gradlew build`, `./gradlew run`, etc.) are still valid. If new build tasks have been added (e.g., `jpackage`), confirm they are documented accurately.

---

## Step 4 — Review and Update `copilot-instructions.md`

### 4a — Tech stack versions

Compare every version number in the "Tech Stack & Build Configuration" section against the values from Step 1a. Fix any that are out of date.

### 4b — Package structure block

Compare the package tree in the "Package Structure" section against the directory listing from Step 1e. Add missing sub-packages; remove deleted ones.

### 4c — Workflow phases list

Compare the numbered phase list in the "Workflow" section against the actual phase sequence from Step 1d. Update phase count, names, and descriptions to match.

### 4d — Supported image formats list

Compare the formats listed in the "Supported Image Formats" section against the extensions from Step 1c. The two must be identical (case-insensitive).

### 4e — Layer names and responsibilities

The "Architecture" bullet list is a summary of the layer descriptions in `ARCHITECTURE.md`. If any layer was renamed or its purpose changed, update this summary accordingly.

---

## Step 5 — Review SKILL.md Files

For each file in `.github/skills/*/SKILL.md`:

### 5a — Verify file paths

Any file path mentioned (e.g., `src/main/resources/.../application.css`, `build.gradle.kts`) must still exist. Use `file_search` to confirm. Update paths that have moved.

### 5b — Verify class and method names

Any Java class, method, or field name mentioned must still exist in the source. Use `grep_search` to verify. Update names that have been renamed.

### 5c — Verify Gradle commands

Any Gradle task mentioned (e.g., `dependencyUpdates`, `compileJava`, `dependencies --write-locks`) must still be valid. Compare against `build.gradle.kts` task definitions.

---

## Step 6 — Final Check

After all edits, re-read each changed document and confirm:
- No version number, class name, or file path was left in a stale state
- No section was accidentally restructured or its meaning changed
- Formatting is consistent with the rest of the document (heading levels, code fences, bold conventions)

Do **not** rewrite accurate prose for style. Only correct factual inaccuracies.
