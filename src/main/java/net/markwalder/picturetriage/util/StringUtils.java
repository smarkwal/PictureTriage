package net.markwalder.picturetriage.util;

/**
 * Utility methods for string formatting.
 */
public final class StringUtils {
    private StringUtils() {
        // Utility class
    }

    /**
     * Choose singular or plural noun form based on count.
     *
     * @param count item count
     * @param singular singular noun form
     * @param plural plural noun form
     * @return noun form matching the count
     */
    public static String pluralize(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
}