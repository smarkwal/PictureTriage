# PictureTriage: Copilot Instructions

## Project Overview

PictureTriage is a desktop application for efficiently organizing and ranking pictures through a multi-phase interactive workflow. The application is built with **Java 25**, **JavaFX 25.0.1**, and **Gradle**, featuring a layered architecture with clear separation of domain, service, and UI concerns.

## Key Technical Requirements

### Java Version & Build Configuration
- **Java 25** is required and enforced via `build.gradle.kts`
  - Gradle is configured to compile to Java 25 bytecode regardless of host JDK
  - JavaFX plugin version: `0.1.0`
  - JavaFX modules used: `javafx.controls`, `javafx.swing`
  
### JVM Arguments
- The application requires `--enable-native-access=javafx.graphics` to allow JavaFX native graphics rendering
- This is automatically configured in the Gradle build's `applicationDefaultJvmArgs`

### Dependencies
- **JavaFX 25.0.1**: Modern desktop UI framework with native renderer integration
- **TwelveMonkeys ImageIO 3.13.1** (`com.twelvemonkeys.imageio:imageio-webp`): Enables WebP image support

### Build & Run Commands
- **Build**: `./gradlew build`
- **Run**: `./gradlew run`
- **Distribution**: Built distribution scripts are at `./build/scripts/PictureTriage` (or `.bat` on Windows)

## Architecture & Design Patterns

### Layered Architecture

The codebase is organized into four primary layers:

1. **Bootstrap Layer** (`Main.java`, `PictureTriageApplication.java`)
   - `Main`: Java entry point; delegates to JavaFX launcher
   - `PictureTriageApplication`: Extends `Application`; creates primary stage and initializes `AppCoordinator`

2. **Domain Layer** (`domain/` package)
   - Records and enums representing immutable domain concepts
   - `ImageItem`: Wraps a file path and metadata (fileName, fileSizeBytes, lastModified)
   - `Phase1Decision`: Enum with values KEEP, TRIAGE, DELETE
   - `Phase1Progress`: Tracks counts and decision timeline for phase 1
   - `Phase2Progress`: Tracks rankings and comparison progress for phase 2
   - `Phase3Decision`: Enum with values KEEP, DELETE (final review decisions)
   - `Phase3Progress`: Tracks keep/delete counts for phase 3
   - `Phase3GridState`: Immutable snapshot of grid state with decisions map
   - `ResultBundle`: Final collected results (kept, triaged, deleted lists)

3. **Service Layer** (`service/` package)
   - UI-independent business logic and workflow orchestration
   - `ImageScannerService`: Recursive folder scan, deterministic sort, unreadable-file tracking
   - `Phase1WorkflowService`: Manages cursor position and classification lists; emits phase 1 events
   - `QuicksortInteractiveRanker`: Interactive quicksort algorithm for phase 2
   - `Phase3WorkflowService`: Manages final review decisions, toggles keep/delete state, provides final image lists
   - `ImageDeleteService`: Safely deletes image files from disk with per-file error handling
   - `ComparisonChoice` (enum): LEFT_BETTER, RIGHT_BETTER
   - `ComparisonPair`: Record representing left/right image comparison
   - `ResultsPrinter`: Formats and outputs final results

4. **UI Layer** (`AppCoordinator`, `ui/` package)
   - `AppCoordinator`: Central state machine; orchestrates scene transitions and workflow
   - `QuicksortProgressPane`: Displays phase 2 comparison UI
   - `SegmentedProgressBar`: Visual progress bar with color segments per decision
   - `Phase3GridPane`: 4-column grid layout for phase 3 image thumbnails
   - `ImageThumbnailButton`: Clickable 200×200px thumbnail with green/red border per decision
   - `DeleteConfirmationDialog`: Simple modal dialogs for deletion confirmation and results

### Workflow Flow

The application follows a rigid, sequential phase flow:

```
Folder Selection → Phase 1 Triage → Phase 2 Ranking → Phase 3 Final Review → Results Display
```

1. **Folder Selection**: User selects a directory via native file chooser
2. **Phase 1**: User categorizes each image (Keep/Triage/Delete) sequentially
3. **Phase 2**: User ranks triaged images via interactive pairwise comparisons (only if triaged list non-empty)
4. **Phase 3**: User performs final review in grid layout; toggles keep/delete state per image; deletes marked files
5. **Results**: Final organized results displayed with deletion summary

### Key Design Principles

- **Immutable Domain Records**: Domain objects (Phase{1,2}Progress, ResultBundle, ImageItem) are immutable records
- **Deterministic Scanning**: Images are sorted deterministically (by path string) after scanning for reproducible ordering
- **Event-Driven Architecture (Partially Implemented)**:
  - Services emit events for UI consumption
  - `AppCoordinator` subscribes and routes to next phase
  - Future: Extract explicit event bus and event payload records
- **Cursor-Based Phase 1**: Phase 1 maintains an index cursor and mutable buckets internally
- **Quicksort Phase 2**: Phase 2 uses interactive quicksort for efficient ranking

## Code Style & Conventions

### Naming
- **Packages**: `net.markwalder.picturetriage.*` (bootstrap/domain/service/ui subpackages)
- **Classes**: PascalCase for services and UI components
- **Records/Enums**: PascalCase (e.g., `Phase1Progress`, `Phase1Decision`)
- **Methods**: camelCase (e.g., `applyDecision()`, `isSupportedImage()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `WINDOW_WIDTH`, `WINDOW_HEIGHT`)

### File Organization
- One public class/record per file
- Related domain records grouped in `domain/` package
- Workflow services in `service/` package
- UI components in `ui/` package
- `AppCoordinator` at root of main package (orchestrator role)

### Image Scanning
- `ImageScannerService.scan()` returns `ScanResult(images, skippedUnreadable)`
- Supported extensions: JPG, JPEG, PNG, GIF, TIFF, BMP, WEBP (case-insensitive)
- Files are sorted by full path string for deterministic order
- Unreadable files are counted but not included in results

## Common Development Tasks

### Adding a New Image Format
1. Update `ImageScannerService.isSupportedImage()` to include new extension
2. No configuration needed if ImageIO can already handle the format
3. For new formats requiring a library, add dependency to `build.gradle.kts`

### Modifying Phase 1 Workflow
1. Update `Phase1Decision` enum if new decisions are needed
2. Modify `Phase1WorkflowService` to emit new events/progress states
3. Update `AppCoordinator.onPhase1Completed()` to route accordingly
4. Update UI in `AppCoordinator` phase 1 scene setup

### Modifying Phase 2 Ranking
1. Changes to ranking algorithm go in `QuicksortInteractiveRanker`
2. Progress estimation logic updates in `Phase2Progress` snapshot logic
3. UI updates in `QuicksortProgressPane` and `AppCoordinator` phase 2 scene

### Adding Progress Estimation
- `Phase2Progress` currently tracks:
  - `triageTotal`: number of images to rank
  - `comparisonsCompleted`: pairwise comparisons done so far
  - `estimatedComparisons`: total comparisons needed (~n*log2(n) for quicksort)
  - `activeRanges` and `finishedRanges`: range partition counts
- Extend fields or introduce `Phase2ProgressEstimator` service for detailed estimates

### Phase 3 Implementation Details

**Grid Layout**: 4-column fixed layout, 200×200px thumbnails with 10px gap
- Uses `GridPane` wrapped in `ScrollPane` for vertical scrolling
- Images concatenated in order: kept (from phase 1) + triaged (sorted from phase 2) + deleted (from phase 1)
- Initial state: kept/triaged images show green border (KEEP), deleted images show red border (DELETE)

**User Interaction**:
- Click any thumbnail to toggle border color and decision state
- Progress label shows keep/delete tallies in real-time
- "Finish & Delete" button shows confirmation dialog, performs file deletion, displays results
- "Cancel" button returns to folder selection

**Deletion Flow**:
1. User clicks "Finish & Delete"
2. Simple confirmation dialog shows count of images to delete
3. If confirmed: `ImageDeleteService` deletes files (per-file error handling)
4. Results dialog shows deletion summary (count of successful/failed deletions)
5. Results page displays with deletion feedback showing in text area

**File Deletion Strategy**:
- `ImageDeleteService.deleteFiles()` performs immediate file-system deletion
- Per-file error handling: accumulates failures, continues with remaining files
- `DeleteResult` record tracks deletion count, failure count, and list of failed paths with reasons
- Errors are surfaced to user in results dialog

## Future Planned Refactoring

From `plan.md`:

1. **Controller Extraction**: Split `AppCoordinator` into dedicated controllers
   - `FolderSelectionController`
   - `Phase1Controller`
   - `Phase2Controller`
   - `Phase3Controller`
   - `ResultsController`

2. **Event Architecture Formalization**:
   - Extract `DomainEvent` interface (marker interface)
   - Extract event payload records (e.g., `FolderScanCompletedEvent`, `Phase1CompletedEvent`, `Phase2CompletedEvent`, `Phase3CompletedEvent`)
   - Implement explicit `EventPublisher` and `EventSubscriber` interfaces
   - Wire event bus across services and controllers

3. **Results Export**: Add `ResultsFileWriter` for exporting organized images to disk

4. **Testing**: Add unit tests for:
   - `ImageScannerService` (folder scanning correctness)
   - `Phase1WorkflowService` (decision logic and progress)
   - `QuicksortInteractiveRanker` (quicksort progress monotonicity)
   - `Phase3WorkflowService` (decision toggle and list filtering)
   - `ImageDeleteService` (file deletion and error handling)

5. **Error Handling**: Add graceful decode-failure rendering for corrupted/unsupported images

6. **Performance**: Optional image prefetching to reduce UI blocking during display

7. **Phase 3 Enhancements**:
   - Keyboard navigation (arrow keys to move, Space to toggle)
   - Undo capability for individual decisions
   - Batch decision operations (select multiple images, apply same decision)
   - Sort/filter options (e.g., by filename, file size, date modified)

## Common Gradle Tasks

```bash
# Build and run
./gradlew run

# Build distribution
./gradlew build

# Clean build artifacts
./gradlew clean

# Run with custom JVM args (if needed)
./gradlew run --args="--some-flag"
```

## Debugging & Troubleshooting

- **Java 25 Required**: If compilation fails, ensure Java 25 JDK is installed and accessible
- **Native Access Error**: If JavaFX fails with native access errors, verify `--enable-native-access=javafx.graphics` is used
- **Image Not Displayed**: Check `ImageScannerService.isSupportedImage()` includes the format; verify TwelveMonkeys ImageIO plugin is loaded
- **File Not Found**: Verify paths are absolute and files are readable via `Files.isReadable()`

## Window Layout Constants

- Default window resolution: `1200x820` pixels
- Defined in `AppCoordinator` as `WINDOW_WIDTH` and `WINDOW_HEIGHT`
- Used consistently across all scene setups

## Important Notes

- **Deterministic Behavior**: Images are always sorted by path string after scanning; this ensures reproducible behavior across runs
- **UI-Independent Services**: All business logic in `service/` package is decoupled from JavaFX; services can be tested and reused independently
- **Immutability**: Domain layer relies on immutable records; mutations occur only in workflow state (e.g., `Phase1WorkflowService` internal buckets)
- **Event-Driven (In Progress)**: Current implementation uses method calls; future refactoring will introduce explicit event bus for cleaner decoupling
