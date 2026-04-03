# PictureTriage

A desktop application for organizing and ranking pictures through an interactive multi-phase workflow. PictureTriage helps you efficiently process large photo directories by categorizing images and ranking those requiring further review.

## Features

- **Phase 1 - Initial Triage**: Quickly categorize images into three buckets: Keep, Triage, or Delete
- **Phase 2 - Interactive Ranking**: Rank triaged images by preference using an interactive quicksort algorithm
- **Phase 3 - Final Review**: Browse all images in a grid layout and make final keep/delete decisions
- **File Deletion**: Safely delete marked images from disk with per-file error handling and confirmation dialog
- **Multi-Format Support**: Handles common image formats plus WebP via the TwelveMonkeys ImageIO library
- **Progress Tracking**: Visual progress indicators throughout the workflow
- **Results Summary**: View final organized results with deletion feedback

## Requirements

- **Java 25** or later — the project is configured to compile to Java 25 bytecode
- **Gradle** — included via the Gradle wrapper; no separate installation needed

## Building

```bash
./gradlew build
```

## Running

```bash
./gradlew run
```

Alternatively, after building, run the generated distribution script:

```bash
./build/scripts/PictureTriage
```

On Windows, use `./build/scripts/PictureTriage.bat`.

## Dependencies

- **JavaFX 25.0.1** — modern desktop UI framework with native graphics support
- **TwelveMonkeys ImageIO 3.13.1** — extended image format support including WebP
