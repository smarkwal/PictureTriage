# PictureTriage: Copilot Instructions

## Project Overview

PictureTriage is a desktop application for efficiently organizing and ranking pictures through a multi-phase interactive workflow. It is built with **Java 25**, **JavaFX 25.0.1**, and **Gradle**.

For a detailed description of the architecture, see [ARCHITECTURE.md](../ARCHITECTURE.md).

## Tech Stack & Build Configuration

- **Java 25** with Gradle build system; JavaFX plugin version `0.1.0`
- **JavaFX 25.0.1**: Desktop UI framework; modules `javafx.controls`, `javafx.swing`
- **TwelveMonkeys ImageIO 3.13.1**: WebP image support
- **Build**: `./gradlew build`
- **Run**: `./gradlew run`
- JVM flag `--enable-native-access=javafx.graphics` is required and automatically configured in the Gradle build

## Architecture

The codebase uses a layered architecture:

- **Bootstrap**: Application entry point and JavaFX startup
- **Domain**: Immutable records and enums representing workflow state and decisions
- **Service**: UI-independent business logic (file scanning, workflow orchestration, sorting, deletion)
- **Controller**: Phase-specific UI orchestration — one controller per workflow phase
- **UI**: Reusable JavaFX components (progress bars, grid layout, thumbnails, dialogs)
- **Util**: Shared utility classes

## Package Structure

```
net.markwalder.picturetriage
├── (root)           — bootstrap and central coordinator
├── domain/          — immutable domain records and enums
├── service/         — business logic and workflow services
├── controller/      — phase controllers (folder selection, phases 1–3)
├── ui/              — reusable JavaFX UI components
└── util/            — shared utilities
```

## Workflow

The application follows a sequential, one-directional phase flow:

1. **Folder Selection** — User picks a directory; images are scanned and sorted deterministically by path
2. **Phase 1** — User categorizes each image: Keep, Triage, or Delete
3. **Phase 2** — Triaged images are ranked via interactive pairwise comparisons (quicksort-based); skipped if no images were triaged
4. **Phase 3** — All images shown in a scrollable grid; user toggles keep/delete per image, then confirms deletion
5. **Results** — Final summary with deletion feedback

## Supported Image Formats

JPG, JPEG, PNG, WEBP (extension check is case-insensitive)

## Context7 Library IDs

If the Context7 MCP tools are available, use these IDs with the `mcp_context7_query-docs` tool to fetch up-to-date documentation:

| Library | Context7 ID |
| --- | --- |
| Java SE & JDK 25 | `/websites/oracle_en_java_javase_25` |
| JavaFX 25 | `/websites/openjfx_io_javadoc_25` |
| Gradle User Manual | `/websites/gradle` |
| TwelveMonkeys ImageIO | `/haraldk/twelvemonkeys` |
| JUnit 5 | `/websites/junit_current` |

## Code Style & Conventions

See [java.instructions.md](instructions/java.instructions.md) — automatically applied when editing Java files.

## Common Development Tasks

- **Keeping docs in sync**: After adding a class, renaming a type, changing workflow phases, or updating dependencies, run the `doc-sync` skill to review and update `README.md`, `ARCHITECTURE.md`, `copilot-instructions.md`, and the `SKILL.md` files for accuracy.
- **Checking for dependency updates**: Run `./gradlew dependencyUpdates --no-parallel`; the report is printed to the console and saved to `build/dependencyUpdates/report.txt`. Only stable-version upgrades are shown (pre-releases and release candidates are filtered out). Update version numbers directly in `build.gradle.kts`, then run `./gradlew dependencies --write-locks --no-parallel` to regenerate the lockfile, and `./gradlew build` to verify.
- **Adding a new image format**: Update the image scanner to recognize the extension; add a dependency if the format requires one
- **Modifying workflow logic**: Update the relevant workflow service; update the corresponding phase controller
- **Modifying UI**: Update the relevant UI component or controller; check `application.css` for unused CSS classes
- **Adding a new phase**: Add a domain record for progress tracking, a workflow service, a controller, and wire the transition in the coordinator
- **Formatting and imports**: Format Java code and optimize imports when editing any file
- **Renaming, moving, or deleting files**: After any file is renamed, moved, or deleted, search **all** files in the repository for references to the old filename or path — including Java source files, `build.gradle.kts`, Markdown documentation, `.github/copilot-instructions.md`, instruction files (`.github/instructions/`), skill files (`.github/skills/`), GitHub Actions workflows, and CSS files — and update or remove any stale references before finishing the task.

## Commit Message Guidelines
- Follow the pattern `<type>: <area>: <message>` where:
  - `type` is one of `Feature`, `Bugfix`, `Change`, `Code`, `Docs`, `Tests`, `Project`
    - Prefer `Feature`, `Bugfix`, `Change` for changes that affect the user experience; `Code`, `Docs`, `Tests`, `Project` for internal changes
  - `area` is optional and describes the affected area (e.g., `UI`, `Folder Selection`, `Phase 2`, etc.)
- Keep the message line under 120 characters
- Start the message with a capital letter
- Use present tense: "Add feature" not "Added feature"

Examples:

- `Feature: Phase 3: Highlight selected image with a white border`
- `Bugfix: UI: Do not change window size when switching to another phase`
- `Change: Workflow: Add a Back button to return to the previous phase`
- `Code: Format code and optimize imports`
- `Docs: Update README.d with new features`
- `Tests: Add unit tests for the image scanner`
- `Project: Add GitHub Actions workflow for CI`

## Debugging & Troubleshooting

- **Compilation fails**: Ensure Java 25 JDK is installed and resolvable by Gradle toolchains
- **Native access error**: Verify `--enable-native-access=javafx.graphics` is present in JVM args
- **Image not displayed**: Check that the image scanner recognizes the file extension; verify TwelveMonkeys plugin is on the classpath
- **File not found**: Verify paths are absolute and files are readable

## Security Considerations

- The application does not execute any untrusted code or handle network input, so the attack surface is minimal
- File deletion is gated behind a confirmation dialog and per-file error handling to prevent accidental data loss
- Whenever possible, files are moved to the system trash instead of permanently deleted, providing an additional safety net
