---
name: javafx-styling
description: 'Add or modify visual styles in the PictureTriage JavaFX application. Covers the CSS token system, JavaFX-specific CSS properties, pseudo-classes, how to apply styles from Java code, and how to trace which files use a given CSS selector. Triggers: style, CSS, color, spacing, font, button appearance, layout padding, visual, JavaFX CSS.'
---

# JavaFX Styling

All styles live in a single stylesheet:
`src/main/resources/net/markwalder/picturetriage/application.css`

The stylesheet is loaded once at application startup in `PictureTriageApplication.java`.

---

## The Token System

Design tokens (colors, sizes, spacing) are defined as **CSS custom properties** on `.root`. Always use tokens instead of raw hex values or pixel counts. Reference them with `var(...)` or the token name directly.

### Available Tokens

Read the `.root` block in `application.css` for the full list — each CSS custom property
has an inline comment explaining its purpose.

---

## JavaFX CSS Differences from Web CSS

JavaFX uses its own CSS engine — not a browser. Key differences:

- All JavaFX properties use the `-fx-` prefix: `-fx-background-color`, `-fx-text-fill`, `-fx-padding`
- Text color is **`-fx-text-fill`** on controls, **`-fx-fill`** on `Text` nodes (not `color`)
- Background colors cannot use `rgba()`; use `-fx-background-color` with `-fx-opacity` for transparency
- Gradients: `linear-gradient(to right, #111, #222)` — same syntax as web
- Border shorthand does not exist; use `-fx-border-color`, `-fx-border-width`, `-fx-border-radius` separately
- `px` units are the only supported unit; `%`, `em`, `rem` are not supported
- `font-weight: bold` is written as `-fx-font-weight: bold`
- Insets (padding, margin): use shorthand `top right bottom left` — e.g., `-fx-padding: 8px 12px`

### Working Pseudo-Classes

| Pseudo-class | Triggered when |
|---|---|
| `:hover` | Mouse is over the node |
| `:focused` | Node has keyboard focus |
| `:pressed` | Mouse button is held down |
| `:disabled` | `node.setDisable(true)` |
| `:selected` | Applies to `ToggleButton`, `CheckBox`, etc. |

### Structural Selectors

| Selector | Meaning |
|---|---|
| `.parent .child` | `.child` anywhere inside `.parent` |
| `.parent > .child` | `.child` as a **direct** child only |
| `.a, .b` | applies to both `.a` and `.b` |

---

## Applying Styles from Java

CSS classes are applied to nodes at construction time in the controller or UI component constructor:

```java
// Apply a single class
button.getStyleClass().add("button-primary");

// Apply multiple classes
label.getStyleClass().addAll("label-body", "image-display-placeholder");

// Remove a class (e.g., to swap state styles)
node.getStyleClass().remove("button-keep");
node.getStyleClass().add("button-delete");
```

Never hard-code inline styles via `node.setStyle(...)` — put the style in `application.css` and apply it with `getStyleClass().add(...)`. The only accepted exception is a truly dynamic, per-instance value that cannot be represented as a CSS class (e.g., a width calculated at runtime from data).

---

## Existing CSS Classes

`application.css` is the authoritative reference — classes are organized into sections with
inline comments describing what each is applied to. Read it before adding a new class to
check whether a suitable one already exists.

---

## Adding a New Style

1. **Define the rule** in `application.css` under the appropriate section comment
2. Use existing tokens for colors and spacing — do not introduce raw hex values or pixel sizes unless a token does not cover the need; if adding a new global value, define it in `.root` first
3. If the new style is a variation of an existing one (e.g., a new button color), follow the same pattern as the nearest similar class (`.button-keep`, `.button-delete`, etc.)
4. Apply the class in the Java constructor of the relevant component via `getStyleClass().add(...)`
5. After the change, run `./gradlew run` and visually verify the style

---

## Java Color Constants — AppColors

Whenever a `javafx.scene.paint.Color` object is required in Java code (e.g. for
`Canvas` drawing in `BlockProgressBar`), use the constants in
`net.markwalder.picturetriage.ui.AppColors` instead of calling `Color.web("#...")` inline.

Read `AppColors.java` for the current list — each constant has a Javadoc comment explaining
its purpose and the corresponding CSS token.

Rules:
- **Never write `Color.web("#...")`** outside of `AppColors.java` itself.
- When adding a new color that also has a CSS usage, add it to both `AppColors.java`
  **and** as a token in `.root` in `application.css`, keeping the values in sync.
- If a color is only ever used in CSS, define it only as a token — no need for an
  `AppColors` constant.

---

## Tracing: Which Java File Uses a CSS Selector?

To find all Java files that apply a given style class, search for the class name string:

```
grep -r '"button-primary"' src/main/java/
```

Or use the VS Code search panel with the class name in quotes to find all `getStyleClass().add("...")` call sites.
