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

**Colors:**
| Token | Value | Purpose |
|---|---|---|
| `-fx-color-bg-primary` | `#050507` | Main window background |
| `-fx-color-bg-surface` | `#0f111a` | Card/panel background |
| `-fx-color-bg-surface-2` | `#171a28` | Secondary surface (buttons) |
| `-fx-color-keep` | `#2e9f44` | Keep action green |
| `-fx-color-triage` | `#8a5cff` | Triage action purple |
| `-fx-color-delete` | `#bf2f2f` | Delete action red |
| `-fx-color-accent-primary` | `#4f7cff` | Primary accent blue |
| `-fx-color-accent-secondary` | `#8a5cff` | Secondary accent purple |
| `-fx-color-neutral` | `#232638` | Neutral surface |
| `-fx-color-border-dark` | `#414760` | Dark border |
| `-fx-color-border-light` | `#2f3448` | Light border |
| `-fx-color-text-primary` | `#f4f6ff` | Main text |
| `-fx-color-text-secondary` | `#bcc3dd` | Secondary/muted text |
| `-fx-color-placeholder-bg` | `#262b40` | Placeholder fill |

**Spacing:**
| Token | Value |
|---|---|
| `-fx-size-padding-small` | `4px` |
| `-fx-size-padding-standard` | `8px` |
| `-fx-size-padding-large` | `10px` |
| `-fx-size-padding-xlarge` | `16px` |
| `-fx-size-padding-xxlarge` | `24px` |
| `-fx-size-gap-small` | `10px` |
| `-fx-size-gap-standard` | `16px` |

**Sizing:**
| Token | Value |
|---|---|
| `-fx-size-thumbnail` | `200px` |
| `-fx-size-thumbnail-border` | `4px` |
| `-fx-size-grid-gap` | `10px` |

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

Never hard-code inline styles via `node.setStyle(...)` outside of dynamic runtime values — put the style in the CSS file.

---

## Existing CSS Class Reference

### Typography
| Class | Use |
|---|---|
| `.label-title-main` | 28px bold — page titles |
| `.label-title-secondary` | 24px bold — phase titles |
| `.label-body` | 14px — body text, metadata |

### Buttons
| Class | Use |
|---|---|
| `.button-primary` | Blue CTA buttons |
| `.button-keep` | Green Keep button |
| `.button-triage` | Purple Triage button |
| `.button-delete` | Red Delete button |

### Phase Layout Structure
| Class | Applied to |
|---|---|
| `.phase-layout-container` | Root `VBox` of a phase |
| `.phase-layout-title-bar` | Top bar with phase title |
| `.phase-layout-content` | Middle content area |
| `.phase-layout-footer` | Bottom footer area |
| `.phase-layout-action-bar` | Button row inside footer |
| `.phase-layout-progress-bar-row` | Progress indicator row inside footer |

### Image Components
| Class | Applied to |
|---|---|
| `.image-display-pane` | Outer card container |
| `.image-display-image-area` | Image area of the card |
| `.image-display-footer` | Footer area of the card |
| `.image-display-path` | Path label (leading ellipsis) |
| `.image-display-meta` | Metadata label (right-aligned) |
| `.image-display-placeholder` | "No image" placeholder label |
| `.thumbnail-button` | Clickable thumbnail in Phase 3 grid |
| `.thumbnail-image-placeholder` | Placeholder fill for thumbnails |

### Phase 3-Specific
| Class | Applied to |
|---|---|
| `.grid-pane-phase3` | The `GridPane` holding thumbnails |
| `.phase3-scroll-pane` | The `ScrollPane` wrapping the grid |

### Dialogs
| Class | Applied to |
|---|---|
| `.app-dialog` | Custom-themed `Alert` dialog panes |

---

## Adding a New Style

1. **Define the rule** in `application.css` under the appropriate section comment
2. Use existing tokens for colors and spacing — do not introduce raw hex values or pixel sizes unless a token does not cover the need; if adding a new global value, define it in `.root` first
3. If the new style is a variation of an existing one (e.g., a new button color), follow the same pattern as the nearest similar class (`.button-keep`, `.button-delete`, etc.)
4. Apply the class in the Java constructor of the relevant component via `getStyleClass().add(...)`
5. After the change, run `./gradlew run` and visually verify the style

---

## Tracing: Which Java File Uses a CSS Selector?

To find all Java files that apply a given style class, search for the class name string:

```
grep -r '"button-primary"' src/main/java/
```

Or use the VS Code search panel with the class name in quotes to find all `getStyleClass().add("...")` call sites.
