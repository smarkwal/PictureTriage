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

JPG, JPEG, PNG, GIF, TIFF, BMP, WEBP (extension check is case-insensitive)

## Code Style & Conventions

See [java.instructions.md](instructions/java.instructions.md) — automatically applied when editing Java files.

## Common Development Tasks

- **Adding a new image format**: Update the image scanner to recognize the extension; add a dependency if the format requires one
- **Modifying workflow logic**: Update the relevant workflow service; update the corresponding phase controller
- **Modifying UI**: Update the relevant UI component or controller; check `application.css` for unused CSS classes
- **Adding a new phase**: Add a domain record for progress tracking, a workflow service, a controller, and wire the transition in the coordinator
- **Formatting and imports**: Format Java code and optimize imports when editing any file

## Debugging & Troubleshooting

- **Compilation fails**: Ensure Java 25 JDK is installed and resolvable by Gradle toolchains
- **Native access error**: Verify `--enable-native-access=javafx.graphics` is present in JVM args
- **Image not displayed**: Check that the image scanner recognizes the file extension; verify TwelveMonkeys plugin is on the classpath
- **File not found**: Verify paths are absolute and files are readable

## Security Considerations

- The application does not execute any untrusted code or handle network input, so the attack surface is minimal
- File deletion is gated behind a confirmation dialog and per-file error handling to prevent accidental data loss
- Whenever possible, files are moved to the system trash instead of permanently deleted, providing an additional safety net
