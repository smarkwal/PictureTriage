package net.markwalder.picturetriage.ui;

import javafx.scene.paint.Color;

/**
 * Central repository of Color constants mirroring the CSS design tokens defined in application.css.
 * Used wherever a Java {@link Color} object is required (e.g. Canvas drawing in
 * {@link BlockProgressBar}) and the value cannot be driven by a CSS rule.
 *
 * <p>Every constant here must stay in sync with the corresponding CSS custom property on
 * {@code .root} in {@code application.css}.</p>
 */
public final class AppColors {

    /** Matches {@code -fx-color-keep}: green used for "keep" decisions. */
    public static final Color KEEP = Color.web("#2e9f44");

    /** Matches {@code -fx-color-triage}: purple used for "triage" decisions. */
    public static final Color TRIAGE = Color.web("#8a5cff");

    /** Matches {@code -fx-color-delete}: red used for "delete" decisions. */
    public static final Color DELETE = Color.web("#bf2f2f");

    /** Matches {@code -fx-color-border-dark}: dark border / neutral block fill. */
    public static final Color BORDER_DARK = Color.web("#414760");

    /** Matches {@code -fx-color-neutral}: neutral surface background. */
    public static final Color NEUTRAL = Color.web("#232638");

    /** White, used for focus highlight borders on canvas-drawn blocks. */
    public static final Color HIGHLIGHT = Color.web("#ffffff");

    /** Dark tile of the transparency checkerboard in {@link ImageDisplayPane}. */
    public static final Color CHECKER_DARK = Color.web("#222222");

    /** Light tile of the transparency checkerboard in {@link ImageDisplayPane}. */
    public static final Color CHECKER_LIGHT = Color.web("#424242");

    private AppColors() {
    }
}
