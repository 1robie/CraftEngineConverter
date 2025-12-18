package fr.robie.craftengineconverter.common.tag;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Interface for processing custom tags within text strings.
 * <p>
 * Tag processors are responsible for identifying and transforming specific tag patterns
 * (e.g., {@code <glyph:emoji_name>}) into their corresponding output format.
 * </p>
 * <p>
 * Implementations should be stateless and thread-safe where possible.
 * </p>
 */
public interface TagProcessor {

    /**
     * Gets the human-readable name of this tag processor.
     * <p>
     * This name is used for logging and debugging purposes to identify which
     * processor is handling a particular tag.
     * </p>
     *
     * @return The descriptive name of this processor (e.g., "Nexo Glyph")
     */
    String getTagName();

    /**
     * Gets the regex pattern used to match tags in input text.
     * <p>
     * The pattern should be compiled and cached for performance. It will be used
     * by both {@link #hasTag(String)} and {@link #process(String)} methods.
     * </p>
     *
     * @return The compiled regex pattern for matching this processor's tags
     */
    Pattern getPattern();

    /**
     * Processes the input string and replaces all matching tags with their converted values.
     * <p>
     * This method scans the input for all occurrences of tags matching {@link #getPattern()},
     * transforms them according to the processor's logic, and returns the resulting string.
     * </p>
     * <p>
     * If no tags are found or no transformations are needed, this method returns
     * {@link Optional#empty()}.
     * </p>
     *
     * @param input The input string that may contain tags to process
     * @return An {@link Optional} containing the processed string if any tags were found and processed,
     *         or {@link Optional#empty()} if no processing was needed
     */
    Optional<String> process(String input);

    /**
     * Checks if the input string contains any tags that this processor can handle.
     * <p>
     * This is a lightweight check that can be used to determine if {@link #process(String)}
     * needs to be called, potentially improving performance by avoiding unnecessary processing.
     * </p>
     *
     * @param input The input string to check for tags
     * @return {@code true} if the input contains at least one matching tag, {@code false} otherwise
     */
    boolean hasTag(String input);
}
