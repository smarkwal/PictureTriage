# PictureTriage

A desktop application for organizing and ranking pictures through an interactive multi-phase workflow. PictureTriage helps you efficiently process large photo directories by categorizing images and ranking those requiring further review.

## Features

- **Phase 1 - Initial Triage**: Quickly categorize images into three buckets: Keep, Triage, or Delete
- **Phase 2 - Interactive Ranking**: Use an interactive quicksort algorithm to rank triaged images by preference
- **Phase 3 - Final Review**: Browse all images in a grid layout and make final keep/delete decisions
- **File Deletion**: Safely delete marked images from disk with per-file error handling and confirmation
- **Multi-Format Support**: Handles standard image formats plus WebP through the TwelveMonkeys ImageIO library
- **Progress Tracking**: Visual progress indicators throughout the workflow
- **Results Management**: View final organized results with deletion summary

## Architecture

PictureTriage follows a layered architecture with clear separation of concerns:

- **Domain Model**: Image records, decision types, and progress tracking
- **Services**: Image scanning, workflow orchestration, and interactive ranking algorithms
- **UI Components**: JavaFX-based user interface with progress visualization
- **Coordination**: Central state machine (`AppCoordinator`) managing phase transitions

The application flows through:
1. **Folder Selection** → Scan and load images from a selected directory
2. **Phase 1 Workflow** → Categorize each image (Keep/Triage/Delete)
3. **Phase 2 Workflow** → Rank triaged images using interactive quicksort
4. **Phase 3 Workflow** → Review all images in grid layout; toggle keep/delete decisions; delete marked files
5. **Results** → View organized images and deletion summary

## Requirements

- **Java 25** (or later)
  - The project is configured to use Java 25 bytecode and requires a JDK 25 installation
- **Gradle** (included via Gradle wrapper, no separate installation needed)

## Building the Project

Build the application using the Gradle wrapper:

```bash
./gradlew build
```

This command:
- Compiles the Java source code
- Resolves dependencies
- Packages the application

## Running the Application

Run the application directly using:

```bash
./gradlew run
```

Alternatively, after building, run the generated distribution script:

```bash
./build/scripts/PictureTriage
```

(On Windows, use `./build/scripts/PictureTriage.bat`)

## Project Structure

```
src/main/java/net/markwalder/picturetriage/
├── Main.java                          # Java entry point
├── PictureTriageApplication.java     # JavaFX application bootstrap
├── AppCoordinator.java               # Central state machine and navigation
├── domain/                           # Domain model classes
│   ├── ImageItem.java               # Represents an image file
│   ├── Phase1Decision.java          # Triage decision enum
│   ├── Phase1Progress.java          # Phase 1 progress tracking
│   ├── Phase2Progress.java          # Phase 2 progress tracking
│   ├── Phase3Decision.java          # Final review decision enum
│   ├── Phase3Progress.java          # Phase 3 progress tracking
│   ├── Phase3GridState.java         # Phase 3 grid state snapshot
│   └── ResultBundle.java            # Final organized results
├── service/                          # Business logic services
│   ├── ImageScannerService.java     # Recursive folder scanning
│   ├── Phase1WorkflowService.java   # Phase 1 triage workflow
│   ├── QuicksortInteractiveRanker.java # Interactive sorting algorithm
│   ├── Phase3WorkflowService.java   # Phase 3 final review workflow
│   ├── ImageDeleteService.java      # Safe file deletion
│   ├── ComparisonPair.java          # A/B comparison data
│   ├── ComparisonChoice.java        # Comparison result enum
│   └── ResultsPrinter.java          # Results output formatter
└── ui/                              # JavaFX user interface
    ├── SegmentedProgressBar.java    # Visual progress bar
    ├── QuicksortProgressPane.java   # Phase 2 progress display
    ├── Phase3GridPane.java          # Phase 3 grid layout
    ├── ImageThumbnailButton.java    # Clickable image thumbnail
    └── DeleteConfirmationDialog.java # Deletion confirmation dialogs
```

## Dependencies

- **JavaFX 25.0.1**: Modern desktop UI framework with native graphics support
- **TwelveMonkeys ImageIO 3.13.1**: Extended image format support including WebP

## Development Notes

- Java bytecode is compiled to Java 25 regardless of the host JDK version
- The application requires `--enable-native-access=javafx.graphics` JVM flag for native graphics rendering (automatically configured in the Gradle build)
- Images are processed deterministically after scanning for consistent behavior
