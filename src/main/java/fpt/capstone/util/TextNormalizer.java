package fpt.capstone.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normalizes free-text names for duplicate detection (BR-47): strip accents,
 * fold Vietnamese đ/Đ to d, lowercase, and collapse whitespace. Two names that
 * differ only by casing, spacing, or diacritics normalize to the same key.
 */
public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    /** @return the normalized key, or {@code null} when {@code input} is null/blank. */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String stripped = Normalizer.normalize(input, Normalizer.Form.NFD);
        stripped = DIACRITICS.matcher(stripped).replaceAll("");
        // đ/Đ carry no combining mark, so NFD leaves them untouched — fold explicitly.
        stripped = stripped.replace('đ', 'd').replace('Đ', 'D');
        stripped = WHITESPACE.matcher(stripped.trim()).replaceAll(" ").toLowerCase();
        return stripped.isEmpty() ? null : stripped;
    }
}
