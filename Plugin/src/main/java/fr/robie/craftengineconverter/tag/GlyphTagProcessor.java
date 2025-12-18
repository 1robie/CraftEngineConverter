package fr.robie.craftengineconverter.tag;

import fr.robie.craftengineconverter.common.CraftEngineImageUtils;
import fr.robie.craftengineconverter.common.tag.TagProcessor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tag processor for converting Nexo glyph tags to CraftEngine font format.
 * <p>
 * This processor handles {@code <glyph:id>} and {@code \<glyph:id>} (escaped) tags,
 * converting them to MiniMessage font tags using registered image data from
 * {@link CraftEngineImageUtils}.
 * </p>
 * <p>
 * <b>Example conversions:</b>
 * <ul>
 *   <li>{@code <glyph:heart>} → {@code <font:custom_font>❤</font>}</li>
 *   <li>{@code \<glyph:star>} → {@code <font:custom_font>⭐</font>}</li>
 * </ul>
 * </p>
 * <p>
 * The processor uses {@link CraftEngineImageUtils#convert(String)} to retrieve
 * the font representation of each glyph. If no conversion is registered, the
 * original tag is preserved (with backslash removed if present).
 * </p>
 *
 * @see TagProcessor
 * @see CraftEngineImageUtils
 */
public class GlyphTagProcessor implements TagProcessor {
    /**
     * Regex pattern matching both escaped and non-escaped glyph tags.
     * <p>
     * Pattern breakdown:
     * <ul>
     *   <li>{@code \\\\?} - Optional backslash (escaped in regex)</li>
     *   <li>{@code <glyph:} - Literal tag opening</li>
     *   <li>{@code ([^>]+)} - Capture group: one or more non-'>' characters (glyph ID)</li>
     *   <li>{@code >} - Literal tag closing</li>
     * </ul>
     * </p>
     */
    private static final Pattern GLYPH_PATTERN = Pattern.compile("\\\\?<glyph:([^>]+)>");

    /**
     * {@inheritDoc}
     *
     * @return "Nexo Glyph"
     */
    @Override
    public String getTagName() {
        return "Nexo Glyph";
    }

    /**
     * {@inheritDoc}
     *
     * @return The compiled pattern for matching glyph tags
     */
    @Override
    public Pattern getPattern() {
        return GLYPH_PATTERN;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Efficiently checks if the input contains any glyph tags without performing
     * the actual conversion, useful for short-circuiting processing.
     * </p>
     *
     * @param input The input string to check
     * @return {@code true} if at least one glyph tag is found
     */
    @Override
    public boolean hasTag(String input) {
        return GLYPH_PATTERN.matcher(input).find();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Processes all glyph tags in the input string by:
     * <ol>
     *   <li>Extracting the glyph ID from each tag</li>
     *   <li>Looking up the conversion via {@link CraftEngineImageUtils#convert(String)}</li>
     *   <li>Replacing the tag with the font format if found</li>
     *   <li>Removing backslash escape if conversion fails</li>
     * </ol>
     * </p>
     * <p>
     * <b>Processing examples:</b>
     * <ul>
     *   <li>Input: {@code "Hello <glyph:heart> world"}<br>
     *       Output: {@code "Hello <font:custom>❤</font> world"}</li>
     *   <li>Input: {@code "Test \<glyph:star>"}<br>
     *       Output: {@code "Test <font:custom>⭐</font>"}</li>
     *   <li>Input: {@code "\<glyph:unknown>"} (no conversion)<br>
     *       Output: {@code "<glyph:unknown>"} (backslash removed)</li>
     * </ul>
     * </p>
     *
     * @param input The input string containing glyph tags to convert
     * @return An {@link Optional} containing the processed string with converted glyphs,
     *         or {@link Optional#empty()} if no glyph tags were found
     */
    @Override
    public Optional<String> process(String input) {
        Matcher matcher = GLYPH_PATTERN.matcher(input);

        if (!matcher.find()) {
            return Optional.empty();
        }

        StringBuilder result = new StringBuilder(input.length());
        int lastEnd = 0;

        // Reset to start for processing all matches
        matcher.reset();

        while (matcher.find()) {
            String glyphId = matcher.group(1);
            String fullMatch = matcher.group(0);

            // Append text before the match
            result.append(input, lastEnd, matcher.start());

            // Try to convert the glyph ID
            Optional<String> converted = CraftEngineImageUtils.convert(glyphId);

            if (converted.isPresent()) {
                // Successfully converted - append the font tag
                result.append(converted.get());
            } else {
                // No conversion found - keep original but remove backslash if present
                result.append(fullMatch.startsWith("\\") ? fullMatch.substring(1) : fullMatch);
            }

            lastEnd = matcher.end();
        }

        // Append remaining text after the last match
        result.append(input, lastEnd, input.length());

        return Optional.of(result.toString());
    }
}
