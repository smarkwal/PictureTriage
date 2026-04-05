---
name: unit-testing
description: 'Write or review unit tests in the PictureTriage project. Use when adding tests for service classes, domain types, or utility classes. Covers test structure, naming, JUnit 5 conventions, and what can/cannot be tested without JavaFX. Triggers: unit test, write test, add test, test coverage, JUnit.'
---

# Unit Testing

## Scope

Only service, domain, and util code is unit-testable without a running JavaFX application.

| Package | Testable? | Notes |
|---|---|---|
| `service/` | Yes | No JavaFX imports; pure Java logic |
| `domain/` | Yes | Immutable records; test constructors and accessors |
| `util/` | Yes | Stateless helpers |
| `controller/` | No | Depends on JavaFX scene graph and UI lifecycle |
| `ui/` | No | JavaFX components require a running application |

---

## Test File Location & Naming

- Source root: `src/test/java/`
- Package: mirror the production package — `net.markwalder.picturetriage.service`, etc.
- Class name: `<ClassUnderTest>Test` — e.g., `Phase1WorkflowServiceTest`
- Test method name: `<methodName>_<scenario>` — e.g., `applyDecision_keep_addsToKeptList`

---

## Preferred Libraries

| Library | Purpose |
|---|---|
| **JUnit 5** | Test runner, assertions, parameterized tests |
| **Mockito** | Mocking dependencies |
| **AssertJ** | Fluent, readable assertions (`assertThat(...)`) |

Prefer AssertJ assertions over JUnit's built-in `assertEquals`/`assertTrue` — they produce better failure messages and read more naturally.

---

## Framework

Use **JUnit 5** (`org.junit.jupiter.api.*`). The dependencies must be declared in `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.x.x")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.x.x")
    testImplementation("org.assertj:assertj-core:3.x.x")
}

tasks.test {
    useJUnitPlatform()
}
```

After adding, run `./gradlew dependencies --write-locks --no-parallel` to regenerate the lockfile.

---

## Test Structure

Use the Arrange-Act-Assert (AAA) pattern, separated by blank lines:

```java
@Test
void applyDecision_keep_addsToKeptList() {

    // Arrange
    var images = List.of(new ImageItem(Path.of("a.jpg")));
    var service = new Phase1WorkflowService(images);

    // Act
    service.applyDecision(Phase1Decision.KEEP);

    // Assert
    assertThat(service.result().kept()).hasSize(1);
    // optional Mockito verifications

}
```

If the method under test has a return value, save it in a variable `result` and assert on it.

---

## Dos and Don'ts

- **Do** use `@BeforeEach` to set up shared state (e.g., a pre-populated service instance)
- **Do** test edge cases: empty list, single item, all same decision
- **Do** use `@ParameterizedTest` with `@EnumSource` when testing all enum values
- **Don't** import any `javafx.*` class in a test — it will fail without a JavaFX runtime
- **Don't** test private methods directly; test them through their public callers
- **Don't** leave tests `@Disabled` without a comment explaining why

---

## Running Tests

```
./gradlew test
```

Test reports are written to `build/reports/tests/test/index.html`.
