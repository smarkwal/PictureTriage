---
name: remove-unused-code
description: 'Find and remove unused code in the PictureTriage project. Use when agent changes have left behind orphaned Java classes, methods, or fields, or unused CSS style rules in application.css. Triggers: cleanup, dead code, unused code, orphaned code, stale code, remove unused.'
---

# Remove Unused Code

## When to Use

After code changes, refactors, or feature removals that may have left behind:
- Unused Java classes, methods, fields, or constants
- Unused CSS rules or style classes in `application.css`
- Unused Java imports

---

## Part 1: Unused Java Code

### Step 1 — Check for compiler warnings

Run a clean build and capture any "unused" warnings:

```
./gradlew clean compileJava 2>&1 | grep -i "unused\|never used\|never read"
```

### Step 2 — Find unused private members

For each Java source file, look for `private` methods, fields, or constants that are never referenced outside their declaring class.

Use `vscode_listCodeUsages` on suspicious symbols. A private symbol with **zero usages** (or only its own declaration) is a candidate for removal.

Focus on:
- `private` fields and constants
- `private` methods and constructors
- `private` inner classes

Do **not** remove `public`, `protected`, or package-private members without confirming they have no external callers — `vscode_listCodeUsages` is the tool to verify.

### Step 3 — Find unused classes

Use `vscode_listCodeUsages` on each class that is not a JavaFX controller, entry point (`Main`, `PictureTriageApplication`), or service wired by name. A class with no usages is a removal candidate.

### Step 4 — Find unused imports

Run:

```
./gradlew compileJava 2>&1 | grep "warning.*import"
```

Or check files for imports flagged by the IDE (shown as greyed-out or with a warning).

### Step 5 — Remove safely

For each unused symbol identified:
1. Confirm zero usages with `vscode_listCodeUsages`.
2. Remove the symbol and its imports.
3. Run `./gradlew build` after each removal batch to confirm no regressions.

---

## Part 2: Unused CSS Rules

CSS classes in `application.css` are referenced as **string literals** in Java code, typically via:

```java
node.getStyleClass().add("class-name");
node.getStyleClass().addAll("class-name", ...);
node.setId("id-name");
getStyleClass().add("class-name");
```

Or via `-fx-style-class` in FXML (not used in this project — it uses pure Java UI).

### Step 1 — Extract all CSS class names

Read `src/main/resources/net/markwalder/picturetriage/application.css` and list every selector that defines a **custom** class (i.e. class names that are NOT JavaFX built-ins).

**Known JavaFX built-in selectors** to skip (do not search for these):
`.root`, `.label`, `.button`, `.scroll-pane`, `.scroll-bar`, `.thumb`, `.track`,
`.viewport`, `.text`, `.tooltip`, `.dialog-pane`, `.content`, `.increment-button`,
`.decrement-button`, `.increment-arrow`, `.decrement-arrow`, `.progress-indicator`

**Custom classes currently in this project** (update this list as the project evolves):
```
button-primary
button-keep
button-triage
button-delete
label-body
label-title-main
label-title-secondary
header-panel
phase-layout-container
phase-layout-title-bar
phase-layout-content
phase-layout-action-bar
phase-layout-footer
phase-layout-progress-bar-row
image-display-pane
image-display-image-area
image-display-footer
image-display-meta
image-display-path
image-display-placeholder
thumbnail-button
thumbnail-image-placeholder
grid-pane-phase3
phase3-scroll-pane
app-dialog
```

### Step 2 — Search for each class name in Java source

For each custom CSS class, run `grep_search` with the class name (as a string) across all `**/*.java` files:

```
query: "class-name"
includePattern: "src/main/java/**/*.java"
isRegexp: false
```

A CSS class with **zero matches** in Java source is unused.

### Step 3 — Remove unused CSS rules

For each unused class:
1. Confirm zero Java references.
2. Remove its rule block from `application.css`.
3. Run `./gradlew build` to confirm no regressions.

---

## Completion Checklist

- [ ] Build passes with `./gradlew build` after all removals
- [ ] No test failures
- [ ] No runtime errors (run with `./gradlew run` and do a quick smoke test)
- [ ] Removed code is committed with message `Code: Remove unused [Java|CSS] code`
