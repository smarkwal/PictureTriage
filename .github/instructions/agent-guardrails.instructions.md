---
description: "Safety guardrails for AI agents working in this repository. Always applied — covers git, file system, software installation, network, secrets, build tasks, CI workflows, and process behavior."
applyTo: "**"
---

# Agent Guardrails

These rules apply unconditionally to every task in this repository.
Ask the user for explicit approval before taking any action listed as prohibited below.

## Git Operations

- Never run `git push` or `git push --force`.
- Never run `git reset --hard`.
- Never delete a local or remote branch.
- Never amend a commit (`git commit --amend`) that has already been pushed.
- Never run `git clean -fd` or any variant — it silently discards untracked files that may be work in progress.
- Never run `git stash drop` or `git stash clear`.

## File System and Destructive Operations

- Never run `rm -rf` on any path outside the workspace root.
- Never delete, overwrite, or move files inside the `test/` directory — it contains real user images.
- Never manually edit `gradle.lockfile` — update it only via `./gradlew dependencies --write-locks --no-parallel`.
- Never write directly into the `build/` directory — it is generated output managed by Gradle.

## Software and System

- Never install software globally (`brew install`, `pip install`, `npm install -g`, `apt-get install`, etc.)
  without explicit user approval.
- Never modify system configuration files (e.g. `/etc/`, `~/.zshrc`, `~/.bashrc`, `/usr/local/`).
- Never change or reconfigure the JDK installation or Java toolchain outside of `build.gradle.kts`.

## Network and Data

- Never send source code, file paths, filenames, or user data to external APIs or third-party services.
- The only permitted outbound network calls are approved MCP documentation tool lookups
  (`mcp_context7_*` and similar read-only documentation tools).

## Secrets

- Never write passwords, API keys, tokens, or any other credentials to any file in the repository.
- Never commit sensitive data to git, even in a temporary or draft commit.
- If a credential is accidentally discovered in the environment, do not log or echo it.

## Build and Project Tasks

- Never run `./gradlew run` unless the user explicitly asks to launch the application.
- Never run `./gradlew jpackage`, `distZip`, `distTar`, or `installDist` without explicit user approval —
  these produce distributable artifacts.
- Always use the project Gradle wrapper `./gradlew`; never invoke a system-installed `gradle` binary.
- Always verify that `./gradlew build` passes before considering a task complete.

## CI and Workflow Files

- Never modify any file under `.github/workflows/` without explicit user approval.

## Process Behavior

- Never start a long-running or daemonized background process without informing the user first.
- Do not bypass safety checks such as `--no-verify` on git hooks.
- Do not discard or overwrite files that appear to be unfinished work-in-progress.
