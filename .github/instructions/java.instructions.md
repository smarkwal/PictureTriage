---
description: "Use when writing, editing, or reviewing Java source files. Covers naming conventions, code style, formatting, imports, and JavaFX-specific rules for the PictureTriage project."
applyTo: "**/*.java"
---

# Java Code Style

## Naming

- **Packages**: lowercase, dot-separated — `net.markwalder.picturetriage.*`
- **Classes, Records, Enums**: PascalCase — `ImageItem`, `Phase1Decision`
- **Methods**: camelCase — `applyDecision()`, `isSupportedImage()`
- **Constants**: UPPER_SNAKE_CASE — `WINDOW_WIDTH`, `MAX_CACHE_SIZE`

## File Organization

- One public type per file; file name matches the type name
- Import order: standard library → third-party → project; no wildcard imports
- Remove all unused imports

## Formatting

- Format Java code before finishing any edit (indent, brace style, line length)
- Use the project's existing style as the reference — do not introduce new formatting patterns

## Comments

- Add an inline comment to each logical block of code explaining its purpose
- Explain non-obvious logic; do not restate what the code obviously does

## Domain Layer

- Domain types (`domain/` package) must be immutable records or enums
- No mutable state in domain types; mutations belong in services only

## Service Layer

- Services must not import or reference any JavaFX types
- Services must be instantiable and testable without a running JavaFX application

## JavaFX Components

- UI components (`ui/` package) must not contain business logic or workflow state
- Components accept data via constructor or setters and expose callbacks or properties

## Java Language Version

- Never use Java preview features — only use features that are fully released and stable
- Avoid using deprecated Java or JavaFX APIs; prefer their documented replacements
