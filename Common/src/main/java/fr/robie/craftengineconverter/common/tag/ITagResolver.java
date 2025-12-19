package fr.robie.craftengineconverter.common.tag;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Utility interface for resolving custom tags in text strings using registered {@link TagProcessor}s.
 * <p>
 * This interface provides methods to initialize tag processors and resolve tags within messages,
 * supporting player-specific context for dynamic content.
 * </p>
 */
public interface ITagResolver {

    /**
     * Initializes all tag processors that will be used for tag resolution.
     * <p>
     * This method should be called once during plugin startup to register and configure
     * all available tag processors (e.g., {@code GlyphTagProcessor}, {@code ItemTagProcessor}).
     * </p>
     * <p>
     * After initialization, the processors will be ready to handle tag resolution
     * through {@link #resolveTags(String, Player)}.
     * </p>
     */
    void initTagProcessors();

    /**
     * Resolves all custom tags in the given message using registered tag processors.
     * <p>
     * This method processes the input message through all registered tag processors,
     * replacing recognized tags with their corresponding values. The player parameter
     * can be used for context-specific replacements (e.g., player name, permissions).
     * </p>
     * <p>
     * Tags are processed sequentially by each registered processor. If multiple processors
     * match overlapping patterns, the order of processing determines the final result.
     * </p>
     *
     * @param message The message containing tags to resolve (e.g., "Hello {@code <glyph:heart>}")
     * @param player The player for whom to resolve tags, may be {@code null} for context-free resolution
     * @return An {@link Optional} containing the resolved message if any tags were processed,
     *         or {@link Optional#empty()} if no tags were found or resolved
     */
    Optional<String> resolveTags(String message, Player player);
}
