---
description: "Use when writing, editing, or reviewing Markdown files. Covers line length, code block formatting, and table style rules required by markdownlint."
applyTo: "**/*.md"
---

# Markdown Style

## Line Length (MD013)

- Maximum line length is **120 characters**.
- Wrap long prose lines at a natural clause or phrase boundary — do not break in the middle of a sentence unless unavoidable.
- Code blocks, URLs inside links, and table rows are exempt from the line-length limit.

## Fenced Code Blocks (MD040)

- Every fenced code block **must** include a language identifier.
- Use `text` for plain diagrams or output with no specific language:
  ````
  ```text
  Bootstrap → Coordinator → Controllers
  ```
  ````
- Use `java`, `kotlin`, `bash`, `json`, etc. for language-specific content.
- Never leave the opening fence as bare ` ``` ` without a language.

## Table Style (MD060)

- Delimit every cell with a space before and after the pipe: `| cell |`.
- The separator row must also use spaced pipes: `| --- | --- |`.
- Do **not** use compact separators like `|---|---|`.

## General Rules

- Use ATX-style headings (`#`, `##`, `###`) — not underline style.
- Blank line before and after every heading, code block, list, and table.
- Do not use trailing spaces (they render as hard line-breaks).
