## Plan: Class Blueprint for JavaFX Picture Triage

**Implementation Status (2026-04-01)**
Completed
1. JavaFX bootstrap and app launch wiring: Main, PictureTriageApplication, AppCoordinator.
2. Domain core records and enums: ImageItem, Phase1Decision, Phase1Progress, Phase2Progress, ResultBundle.
3. Core services: ImageScannerService, Phase1WorkflowService, QuicksortInteractiveRanker, ComparisonChoice, ComparisonPair, ResultsPrinter.
4. UI progress components: SegmentedProgressBar and QuicksortProgressPane.
5. End-to-end runnable flow: folder selection -> phase 1 -> phase 2 -> results.

Partially implemented
1. AppCoordinator currently combines multiple controller responsibilities (FolderSelectionController, Phase1Controller, Phase2Controller, ResultsController not yet split into separate classes).
2. Quicksort progress uses in-ranker estimation; dedicated Phase2ProgressEstimator class is not yet separated.
3. Event-driven architecture from blueprint is not yet extracted (DomainEvent, EventPublisher, EventSubscriber and concrete event payload classes are pending).

Planned next
1. Extract dedicated controller classes from AppCoordinator.
2. Introduce explicit event payload records and event bus interfaces.
3. Add ResultsFileWriter export path.
4. Add automated tests for scanner, phase 1 decisions/progress, and quicksort progress monotonicity.
5. Add decode-failure placeholder rendering and optional image prefetch.

Use a layered structure with explicit app-state orchestration, UI-independent workflow services, and event payload records that controllers consume. The flow remains folder selection -> phase 1 triage -> phase 2 interactive quicksort -> results, with dedicated progress contracts for both phases.

**Steps**
1. Define package boundaries and bootstrap classes.
2. Define domain records and enums used across phases.
3. Define application events and payload contracts.
4. Implement phase 1 workflow classes and segmented progress state.
5. Implement phase 2 interactive quicksort classes and progress estimation.
6. Implement JavaFX controllers and view components bound to emitted events.
7. Implement output/reporting and end-of-flow composition.
8. Add unit and manual verification for behavior and event correctness.

**Class Blueprint**
1. Bootstrapping and navigation
1.1 Class name: Main
Responsibilities: Java entrypoint, delegates to JavaFX launcher.
Key methods: main(String[] args).
1.2 Class name: PictureTriageApplication extends javafx.application.Application
Responsibilities: create primary stage, initialize navigation controller, start with folder selection scene.
Key methods: start(Stage primaryStage), stop().
1.3 Class name: AppCoordinator
Responsibilities: central state machine and scene transitions; subscribes to domain events and routes to next phase.
Key methods: begin(), onFolderSelected(Path folder), onPhase1Completed(Phase1CompletedEvent e), onPhase2Completed(Phase2CompletedEvent e), showResults(ResultBundle result).

2. Domain model
2.1 Record name: ImageItem
Fields: Path path, String fileName, long fileSizeBytes, Instant lastModified.
2.2 Enum name: Phase1Decision
Values: KEEP, TRIAGE, DELETE.
2.3 Record name: Phase1Progress
Fields: int totalImages, int reviewedCount, int keepCount, int triageCount, int deleteCount, List<Phase1Decision> decisionTimeline.
Notes: decisionTimeline drives segmented color bar in exact chronological order.
2.4 Record name: Phase2Progress
Fields: int triageTotal, int comparisonsCompleted, int estimatedComparisons, int activeRanges, int finishedRanges, boolean done.
2.5 Record name: ResultBundle
Fields: List<ImageItem> keptImages, List<ImageItem> rankedTriageImages, List<ImageItem> deletedImages.

3. Event and payload contracts
3.1 Interface name: DomainEvent
Responsibilities: marker interface for workflow-to-UI signals.
3.2 Record name: FolderScanCompletedEvent implements DomainEvent
Fields: Path rootFolder, List<ImageItem> images, int skippedUnreadable.
3.3 Record name: Phase1ImagePresentedEvent implements DomainEvent
Fields: int index, int total, ImageItem image, Phase1Progress progress.
3.4 Record name: Phase1DecisionAppliedEvent implements DomainEvent
Fields: ImageItem image, Phase1Decision decision, Phase1Progress progress.
3.5 Record name: Phase1CompletedEvent implements DomainEvent
Fields: List<ImageItem> keptImages, List<ImageItem> triageImages, List<ImageItem> deletedImages, Phase1Progress finalProgress.
3.6 Record name: Phase2ComparisonRequestedEvent implements DomainEvent
Fields: ImageItem leftCandidate, ImageItem rightCandidate, Phase2Progress progress.
3.7 Record name: Phase2ProgressUpdatedEvent implements DomainEvent
Fields: Phase2Progress progress.
3.8 Record name: Phase2CompletedEvent implements DomainEvent
Fields: List<ImageItem> rankedTriageImages, Phase2Progress finalProgress.
3.9 Record name: FatalWorkflowErrorEvent implements DomainEvent
Fields: String userMessage, Throwable cause.

4. Services: ingestion and phase 1
4.1 Class name: ImageScannerService
Responsibilities: recursive folder scan, extension filtering, deterministic sort, unreadable-file accounting.
Key methods: ScanResult scan(Path rootFolder).
Supporting record: ScanResult(List<ImageItem> images, int skippedUnreadable).
4.2 Class name: Phase1WorkflowService
Responsibilities: own cursor and classification lists, apply user decisions, emit phase 1 events.
Key methods: start(List<ImageItem> images), applyDecision(Phase1Decision decision), currentImage(), isComplete(), snapshot().
Internal state: index pointer, mutable buckets, immutable Phase1Progress snapshots per action.
4.3 Interface name: EventPublisher
Responsibilities: publish DomainEvent instances to subscribers.
Key methods: publish(DomainEvent event), subscribe(EventSubscriber subscriber).
4.4 Interface name: EventSubscriber
Responsibilities: consume DomainEvent instances.
Key methods: onEvent(DomainEvent event).

5. Services: phase 2 ranking
5.1 Enum name: ComparisonChoice
Values: LEFT_BETTER, RIGHT_BETTER.
5.2 Record name: ComparisonPair
Fields: ImageItem left, ImageItem right.
5.3 Interface name: InteractiveRanker
Responsibilities: stateful ranking protocol controlled by repeated user choices.
Key methods: start(List<ImageItem> triageImages), Optional<ComparisonPair> currentPair(), submitChoice(ComparisonChoice choice), boolean isComplete(), List<ImageItem> result(), Phase2Progress progress().
5.4 Class name: QuicksortInteractiveRanker implements InteractiveRanker
Responsibilities: iterative quicksort stack/range management, pivot placement, request comparisons one-at-a-time.
Key methods: start(...), currentPair(), submitChoice(...), advancePartition(), progress().
Notes: no JavaFX dependencies; emits progress through EventPublisher.
5.5 Class name: Phase2ProgressEstimator
Responsibilities: compute estimatedComparisons baseline and clamp behavior.
Key methods: int estimateForN(int n), Phase2Progress updateAfterComparison(Phase2Progress current, int activeRanges, int finishedRanges, boolean done).
Estimator guideline: base = max(1, round(n * log2(max(2, n)))), with floor and completion clamp at done=true.

6. UI controllers and components
6.1 Class name: FolderSelectionController
Responsibilities: show DirectoryChooser, invoke scan, forward results to coordinator.
Key methods: chooseFolder(), onScanCompleted(FolderScanCompletedEvent e).
6.2 Class name: Phase1Controller
Responsibilities: render single image, handle up/right/down keys, update segmented progress bar and counters.
Key methods: showImage(Phase1ImagePresentedEvent e), onKeyPressed(KeyEvent e), applyPhase1Progress(Phase1Progress progress), onPhase1Complete(Phase1CompletedEvent e).
6.3 Class name: Phase2Controller
Responsibilities: render left/right candidates, handle left/right keys, update quicksort progress bar and telemetry labels.
Key methods: showComparison(Phase2ComparisonRequestedEvent e), onKeyPressed(KeyEvent e), applyPhase2Progress(Phase2Progress progress), onPhase2Complete(Phase2CompletedEvent e).
6.4 Class name: ResultsController
Responsibilities: render and optionally export ordered final lists.
Key methods: showResults(ResultBundle result), exportToText(Path destination).
6.5 Class name: SegmentedProgressBar
Responsibilities: custom JavaFX control for phase 1 decisionTimeline rendering.
Key methods: setTimeline(List<Phase1Decision> decisions, int total), setColors(Paint keep, Paint triage, Paint delete).
6.6 Class name: QuicksortProgressPane
Responsibilities: phase 2 progress visualization and explanatory labels.
Key methods: setProgress(Phase2Progress progress), setTelemetryText(String text).

7. Output and reporting
7.1 Class name: ResultsPrinter
Responsibilities: print final ordered lists exactly in required sequence.
Key methods: print(ResultBundle result, PrintStream out).
7.2 Class name: ResultsFileWriter
Responsibilities: optional plain-text export of the same ordered structure.
Key methods: write(ResultBundle result, Path destination).

8. Event payload behavior rules
1. Every decision in phase 1 must emit Phase1DecisionAppliedEvent and then either Phase1ImagePresentedEvent for next image or Phase1CompletedEvent when done.
2. Every comparison decision in phase 2 must emit Phase2ProgressUpdatedEvent; when a new pair is needed emit Phase2ComparisonRequestedEvent.
3. When ranking completes, emit Phase2CompletedEvent with done=true in final progress.
4. On any unrecoverable decode/workflow failure, emit FatalWorkflowErrorEvent and transition to recoverable UI state.

9. Dependencies and execution order
1. Implement domain records and events before services.
2. Implement services before controllers.
3. Implement custom progress controls after progress models are stable.
4. Integrate coordinator and navigation last.

**Relevant files**
- /Users/stephan.markwalder/Documents/PictureTriage/build.gradle.kts - add JavaFX and test configuration.
- /Users/stephan.markwalder/Documents/PictureTriage/src/main/java/net/markwalder/picturetriage/Main.java - replace hello-world entrypoint.
- /Users/stephan.markwalder/Documents/PictureTriage/idea.md - requirement source for acceptance checks.

**Verification**
1. Unit test ImageScannerService for recursion/filtering/skipped counts.
2. Unit test Phase1WorkflowService emits event sequence and exact Phase1Progress counters/timeline.
3. Unit test QuicksortInteractiveRanker with scripted choices returns deterministic ordered list.
4. Unit test Phase2ProgressEstimator monotonic progress and completion clamp.
5. UI manual test: color segments in phase 1 exactly match decision history; phase 2 bar and telemetry update on each comparison.
6. End-to-end manual test: final printed order is kept, ranked triage, deleted.

**Decisions**
- Keep an internal event bus to decouple services from JavaFX controllers.
- Use immutable event payload records so UI updates are deterministic and testable.
- Phase 1 bar is exact by timeline; phase 2 bar is estimate plus telemetry due to quicksort data dependence.

**Further Considerations**
1. If exact phase 2 percentage is mandatory, replace quicksort with merge sort while reusing the same comparison-request event contract.
2. Add keyboard hint overlays to reduce user confusion during phase transitions.
3. Add optional undo for the most recent decision in each phase as a future enhancement.
