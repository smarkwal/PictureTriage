# Picture Triage: Architecture

## Overview

Picture Triage is organized into a layered architecture with strict separation of concerns.
Each layer has a single responsibility and depends only on layers below it.

```text
Bootstrap → Coordinator → Controllers → Services → Domain
                       ↘ UI Components ↗
```

---

## Layer Descriptions

### Bootstrap Layer

**`Main`** — Java entry point. Delegates immediately to the JavaFX launcher to avoid module-path issues with JavaFX applications.

**`PictureTriageApplication`** — Extends `javafx.application.Application`.
Creates the primary `Stage`, wires the `AppCoordinator`, and launches the first scene.

---

### Coordination Layer

**`AppCoordinator`** — Central state machine that manages phase transitions.
It receives completion signals from controllers and navigates to the next phase.
It also owns the deletion task and results dialog flow that span multiple controllers.
The coordinator does not contain phase-specific UI logic; that is delegated to controllers.

---

### Controller Layer

Each controller owns the UI setup and user interaction for exactly one phase.

**`FolderSelectionController`** — Presents a directory chooser.
On confirmation, triggers image scanning and hands off to Phase 1.

**`Phase1Controller`** — Shows images one at a time with Keep / Triage / Delete action buttons
and a segmented progress bar. Drives `Phase1WorkflowService` forward on each decision.

**`Phase2Controller`** — Shows two images side by side for pairwise comparison.
Drives `QuicksortInteractiveRanker` forward on each choice. Skipped entirely if no images were triaged in Phase 1.

**`Phase3Controller`** — Displays all images in a scrollable thumbnail grid.
Maintains the current keep/delete state for each image and handles the “Finish & Delete” confirmation and deletion flow.

---

### Service Layer

Services contain all business logic and are fully decoupled from JavaFX.
They can be instantiated and tested without a running UI.

**`ImageScannerService`** — Recursively scans a directory for supported image files.
Returns a `ScanResult` record containing the sorted image list and separate counts of unreadable and error files.
Files are sorted deterministically by their full path string.

**`ScanResult`** — Record returned by `ImageScannerService`. Contains the list of discovered `ImageItem` instances,
a count of files skipped because they were unreadable, and a count of files skipped due to access errors.

**`Phase1WorkflowService`** — Holds the cursor position and the three classification buckets
(keep, triage, delete). Exposes methods to advance the cursor and apply a decision.
Emits a completion event when all images have been classified.

**`QuicksortInteractiveRanker`** — Implements an interactive in-place quicksort. On each step it
provides the next pair of images to compare; the caller supplies the result. Tracks comparison progress for UI feedback.

**`Phase3WorkflowService`** — Manages the final per-image keep/delete decisions.
Initializes from Phase 1 and Phase 2 results. Supports toggling individual decisions
and querying the current decision for any image.

**`ImageDeleteService`** — Performs file-system deletion for a list of images.
Processes each file independently so a single failure does not abort the batch.
Returns a result record with success and failure counts.

**`ImageCache`** — Caches decoded `Image` instances to avoid redundant disk reads across all workflow phases.
Uses a `WeakHashMap` for automatic GC-based eviction to avoid excessive memory use.

**`ComparisonPair`** — Record holding the left and right images for a Phase 2 comparison step.

**`ComparisonChoice`** — Enum with values `LEFT_BETTER` and `RIGHT_BETTER`. Passed back to the ranker after each comparison.

---

### Domain Layer

All domain types are immutable records or enums.

**`ImageItem`** — Represents a single image file. Stores the absolute path, file name,
file size in bytes, and last-modified timestamp.

**`Phase1Decision`** — Enum with values `KEEP`, `TRIAGE`, `DELETE`.

**`Phase3Decision`** — Enum with values `KEEP`, `DELETE`.

**`Phase3GridState`** — Immutable snapshot of the Phase 3 grid, mapping each `ImageItem`
to its current `Phase3Decision`. Used to initialize the Phase 3 controller.

**`ResultBundle`** — Aggregates the three final image lists (kept, ranked/triaged, deleted) produced after Phase 2 completes.

---

### UI Component Layer

Reusable JavaFX components with no business logic. They accept data and expose callbacks or properties;
they do not hold workflow state.

**`PhaseLayoutContainer`** — Standard two-panel layout used by phase controllers:
a top content area and a bottom action bar.

**`BlockProgressBar`** — A segmented progress bar where each segment represents one image decision.
Segments are colored by decision category (keep/triage/delete).

**`ImageDisplayPane`** — Displays a single image scaled to fit within a fixed area, preserving aspect ratio.

**`ImageThumbnailButton`** — A 200×200 px clickable thumbnail. Shows a colored border
(green for KEEP, red for DELETE) indicating the current decision. Used in the Phase 3 grid.

**`Phase3GridPane`** — A responsive scrollable grid of `ImageThumbnailButton` instances for Phase 3 review.
The column count adapts automatically to the available viewport width; each thumbnail is 200×200 px with a 10 px gap.

**`QuicksortProgressPane`** — Displays two images side by side with Left / Right choice buttons
and a comparison progress indicator for Phase 2.

**`DeleteConfirmationDialog`** — Modal dialog used to confirm deletion before proceeding
and to show a deletion results summary afterward.

---

### Utility Layer

**`StringUtils`** — Shared string formatting helpers used across controllers and UI components.

---

## Data Flow

```text
ImageScannerService.scan()
    → List<ImageItem>
    → Phase1WorkflowService
        → keep list, triage list, delete list
        → QuicksortInteractiveRanker (triage list)
            → ranked triage list
            → ResultBundle
                → Phase3WorkflowService
                    → final keep list, final delete list
                    → ImageDeleteService
```

---

## Key Design Decisions

| Decision | Rationale |
| --- | --- |
| Immutable domain records | Prevents accidental mutation; simplifies reasoning about state |
| Deterministic image ordering | Reproducible behavior across runs; predictable test scenarios |
| Services decoupled from JavaFX | Services can be unit-tested without a running UI thread |
| One controller per phase | Keeps each phase self-contained; easy to locate and modify |
| Image cache in service layer | Avoids re-decoding images during rapid Phase 2 comparisons |

---

## Window Layout

- Default window size: **80% of the primary screen's usable area**
- Window resizing: enabled (window is resizable)
- Phase 3 grid: responsive column count, 200 × 200 px thumbnails, 10 px gap, vertically scrollable
