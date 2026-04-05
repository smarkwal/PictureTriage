---
name: security-audit
description: 'Perform an in-depth security analysis of the PictureTriage codebase. Covers grep-based static checks, manual review of sensitive files, OWASP Top 10 checklist, dependency CVE scan, and optional SpotBugs analysis. Triggers: security audit, security analysis, vulnerability scan, CVE check, OWASP, security review, hardening.'
---

# Security Audit

This skill runs a repeatable, in-depth security analysis of the PictureTriage codebase.
The result is reported directly in the conversation — no report file is written to the repo.

---

## Application Threat Model (Established Context)

PictureTriage is a **single-user offline desktop app**. There is no network, no authentication, no database,
and no multi-user concern. The relevant attack surface is:

- **File system access** — recursive scanning, image loading, file deletion
- **Image codec parsing** — trusting `javax.imageio.ImageIO` to handle potentially malformed files
- **Path handling** — computing and displaying relative paths

The following OWASP Top 10 categories are **not applicable** to this app and should be skipped without discussion:

| Category | Reason skipped |
| --- | --- |
| A02 Cryptographic Failures | No sensitive data; no encryption |
| A05 Security Misconfiguration (server) | No server/cloud component |
| A07 Identification & Auth Failures | No login or session management |
| A08 Software & Data Integrity (supply chain) | No plugin system or auto-update mechanism |
| A10 SSRF | Zero network calls |

---

## Step 1 — Grep-Based Static Checks

Run these searches in parallel. Each one is looking for a specific dangerous pattern.
For each hit found, read the surrounding context to determine whether it is an actual issue.

### 1a — Shell injection

```bash
grep -rn "Runtime.exec\|ProcessBuilder\|Runtime.getRuntime" src/main/java/
```

Expected result: **no matches**. Any hit must be investigated for injection risk.

### 1b — Deserialization

```bash
grep -rn "ObjectInputStream\|readObject\(\|readResolve\(\|readExternal\(" src/main/java/
```

Expected result: **no matches**.

### 1c — Reflection and dynamic class loading

```bash
grep -rn "Class\.forName\|getDeclaredMethod\|getDeclaredField\|setAccessible" src/main/java/
```

Expected result: **no matches** in application code (framework internals don't count).

### 1d — Hardcoded credentials or secrets

```bash
grep -rni "password\s*=\|secret\s*=\|apikey\s*=\|token\s*=\|credential" src/main/java/
```

Expected result: **no matches**.

### 1e — Unsafe or deprecated APIs

```bash
grep -rn "sun\.misc\.Unsafe\|finalize()\|System\.exit\|Thread\.stop\|Thread\.suspend" src/main/java/
```

Expected result: **no matches** in application code. `System.exit` in bootstrap is acceptable;
flag any other usage.

### 1f — Logging calls that may expose sensitive paths

```bash
grep -rn "System\.err\.print\|System\.out\.print\|\.printStackTrace\|Logger\." src/main/java/
```

For each hit, note whether a file path or stack trace is included in the message.
This is a **LOW** risk information-disclosure item for a desktop app, but worth documenting.

---

## Step 2 — Manual Review of Sensitive Files

Read each file fully before assessing it. Check every item in the checklist for that file.

### 2a — `ImageScannerService.java`

File: `src/main/java/net/markwalder/picturetriage/service/ImageScannerService.java`

Checklist:

- [ ] `Files.walkFileTree(...)` is called **without** `FileVisitOption.FOLLOW_LINKS` — symlinks are not traversed
- [ ] Unreadable directories are caught and skipped gracefully (no uncaught `IOException`)
- [ ] File extension check is case-insensitive and limited to the supported set (JPG, JPEG, PNG, WEBP)
- [ ] No string concatenation is used to build file paths — `Path` API used throughout
- [ ] No shell command is constructed from the folder path

### 2b — `ImageDeleteService.java`

File: `src/main/java/net/markwalder/picturetriage/service/ImageDeleteService.java`

Checklist:

- [ ] Deletion is *trash-first* — `Desktop.moveToTrash()` attempted before `Files.delete()`
- [ ] Each file deletion is individually wrapped in try-catch — one failure does not block others
- [ ] Deletion is only triggered after the user has confirmed through `DeleteConfirmationDialog`
- [ ] The file path is resolved via `Path` API, not string concatenation
- [ ] No path traversal: confirm the paths deleted come from the scanned set, not from user-typed input

### 2c — `ImageCache.java`

File: `src/main/java/net/markwalder/picturetriage/service/ImageCache.java`

Checklist:

- [ ] **Image size limit** — is there a maximum file size or dimension check before calling `ImageIO.read()`?
  If absent, flag as **LOW** hardening item: a very large or malformed image could exhaust heap memory.
- [ ] Cache eviction — a `WeakHashMap` or equivalent is used so entries are released under memory pressure
- [ ] Errors from `ImageIO.read()` are caught and do not propagate as unchecked exceptions to the UI

### 2d — Logging and error reporting across all service files

For each `System.err.println(...)` or `System.out.println(...)` call found in Step 1f:

- Note which file and method contains it
- Note whether it embeds a file path, exception message, or stack trace
- Classify as: **Acceptable** (desktop app; developer context only) or **Flag** (sensitive data beyond paths)

---

## Step 3 — OWASP Top 10 Checklist

Address only the applicable categories. Mark each as Confirmed Safe, Finding, or N/A.

### A01: Broken Access Control

- Does the app rely solely on OS-level file permissions for access control? (Acceptable for single-user desktop)
- Is there any code that bypasses OS permissions (e.g., elevated-privilege file access)?
- Is deletion gated behind a user confirmation step?

### A03: Injection

- **Command injection** — covered by Step 1a. No `Runtime.exec` or `ProcessBuilder`.
- **Path traversal** — are paths normalized via `.normalize()` before use?
  Check `ImageDisplayPane.java` for `.normalize()` + `.startsWith()` guards on relative path display.
- **Image codec injection** — `ImageIO.read()` parses untrusted binary data. This delegates trust to the
  JDK and TwelveMonkeys; flag only if a known CVE is found in Step 5.

### A04: Insecure Design

- Is there a confirmation dialog before any destructive operation?
- Are temporary files (if any) created in predictable, world-writable locations?
- Is the deletion list derived only from the scanned image set, not from arbitrary user text input?

### A06: Vulnerable and Outdated Components

Covered by Step 5 (CVE scan) and the `dependency-update` skill for version currency.
Note here only if a component is known-vulnerable at the time of the audit.

### A09: Security Logging and Monitoring

- Are error messages logged with enough detail for debugging but without exposing secrets?
- Is there any sensitive data beyond file paths in log output?
- For a desktop app, `System.err` is the expected channel; flag only if paths are sent externally.

---

## Step 4 — Dependency CVE Scan (Optional but Recommended)

Use the OWASP Dependency-Check Gradle plugin to scan for known CVEs in the dependency tree.

### 4a — Add the plugin temporarily

In `build.gradle.kts`, add to the `plugins` block:

```kotlin
id("org.owasp.dependencycheck") version "12.1.1"
```

Also add configuration to reduce scan noise (optional):

```kotlin
dependencyCheck {
    failBuildOnCVSS = 7.0f   // fail only on high/critical
    suppressionFile = null    // no suppression file yet
}
```

### 4b — Run the scan

```bash
./gradlew dependencyCheckAnalyze --no-parallel
```

The HTML report is written to `build/reports/dependency-check-report.html`.
Read `build/reports/dependency-check-report.html` (or the JSON variant) to extract findings.

### 4c — Interpret and report

For each CVE found, note:

- CVE ID and CVSS score
- Affected dependency (group:artifact:version)
- Whether the vulnerable code path is actually reachable in PictureTriage
- Recommended action: bump the dependency (use the `dependency-update` skill) or suppress with justification

### 4d — Remove the plugin after the audit

Remove the `id("org.owasp.dependencycheck")` line and the `dependencyCheck { }` block from `build.gradle.kts`.
Run `./gradlew build` to confirm the build is clean.

**Do not commit the OWASP plugin to `build.gradle.kts`** — it is used only during the audit session.

---

## Step 5 — Optional SpotBugs Static Analysis

SpotBugs finds common Java bug patterns, some of which are security-relevant (e.g., path traversal,
hardcoded IVs, insecure random usage).

### 5a — Add the plugin temporarily

In `build.gradle.kts`, add to the `plugins` block:

```kotlin
id("com.github.spotbugs") version "6.1.7"
```

### 5b — Run the analysis

```bash
./gradlew spotbugsMain --no-parallel
```

The HTML report is at `build/reports/spotbugs/main.html`.

### 5c — Filter for security-relevant findings

Focus on these SpotBugs bug categories:

| Category | Description |
| --- | --- |
| `PATH_TRAVERSAL_IN` | Relative path passed to file API |
| `DM_DEFAULT_ENCODING` | Implicit platform charset use |
| `DMI_HARDCODED_ABSOLUTE_FILENAME` | Hard-coded path strings |
| `PREDICTABLE_RANDOM` | `java.util.Random` used where `SecureRandom` is needed |
| `MS_EXPOSE_REP` | Mutable static exposed via public accessor |

Ignore non-security categories (performance, style) during a security audit.

### 5d — Remove the plugin after the audit

Remove the `id("com.github.spotbugs")` lines and any `spotbugs { }` configuration blocks.
Run `./gradlew build` to confirm the build is clean.

**Do not commit the SpotBugs plugin to `build.gradle.kts`** — it is used only during the audit session.

---

## Step 6 — Report Findings

Summarize in the following structure:

### Confirmed Safe

List each area checked and state explicitly that no issue was found. This gives confidence
that the check was actually performed and not just skipped.

### Findings

For each issue found, report:

| Field | Content |
| --- | --- |
| **Severity** | Critical / High / Medium / Low / Informational |
| **Location** | File and line number (if applicable) |
| **Description** | What the issue is and why it matters |
| **Evidence** | The relevant code snippet or grep output |
| **Recommendation** | Specific fix or hardening action |

### Hardening Opportunities

List improvements that are not vulnerabilities but would increase robustness:

- Low-risk items deferred for later (e.g., image size limits, log redaction)
- Future features that would introduce new risk areas (e.g., network access, multi-user)

### Summary

End with a one-paragraph overall assessment of the security posture.
