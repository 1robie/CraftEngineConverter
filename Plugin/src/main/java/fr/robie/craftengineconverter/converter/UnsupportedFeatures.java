package fr.robie.craftengineconverter.converter;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Collects the configuration a converter had to leave behind, so it can be reported once at the end rather than as a
 * warning per item. A pack with two thousand items that all use the same unsupported mechanic should produce one line,
 * not two thousand.
 */
public final class UnsupportedFeatures {
    private static final int MAX_EXAMPLES = 3;

    private final Map<String, Occurrences> occurrencesByFeature = new TreeMap<>();

    /** Notes that {@code itemId} configured {@code feature}, which the conversion does not carry over. */
    public void record(@NotNull String feature, @NotNull String itemId) {
        this.occurrencesByFeature.computeIfAbsent(feature, key -> new Occurrences()).add(itemId);
    }

    /**
     * Records every key of {@code section} the converter does not read, reported under {@code prefix}. Driving this
     * off what the converter actually reads - rather than off a list of what the source plugin supports - means a key
     * cannot go quietly missing just because nobody remembered to add it here.
     */
    public void recordUnhandledKeys(@NotNull String prefix, @Nullable ConfigurationSection section,
                                    @NotNull Set<String> handledKeys, @NotNull String itemId) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (!handledKeys.contains(key)) {
                this.record(prefix.isEmpty() ? key : prefix + "." + key, itemId);
            }
        }
    }

    public boolean isEmpty() {
        return this.occurrencesByFeature.isEmpty();
    }

    public void clear() {
        this.occurrencesByFeature.clear();
    }

    /** One entry per feature, ordered by name, each with how many items used it and a few of their ids. */
    public @NotNull Collection<Summary> summaries() {
        List<Summary> summaries = new ArrayList<>();
        this.occurrencesByFeature.forEach((feature, occurrences) ->
                summaries.add(new Summary(feature, occurrences.count, String.join(", ", occurrences.examples))));
        return summaries;
    }

    public record Summary(String feature, int count, String examples) {
    }

    private static final class Occurrences {
        private final Set<String> examples = new LinkedHashSet<>();
        private int count;

        private void add(String itemId) {
            this.count++;
            if (this.examples.size() < MAX_EXAMPLES) {
                this.examples.add(itemId);
            }
        }
    }
}
