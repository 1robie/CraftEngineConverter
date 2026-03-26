package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RangeDispatchModelConfiguration implements ModelConfiguration {
    private final String property;
    private Double scale;
    private final List<Entry> entries = new ArrayList<>();
    private ModelConfiguration fallback;

    public RangeDispatchModelConfiguration(@NotNull String property) {
        this.property = this.namespaced(property);
    }

    public void setScale(@Nullable Double scale) {
        this.scale = scale;
    }

    public void setFallback(@Nullable ModelConfiguration fallback) {
        this.fallback = fallback;
    }

    public void addEntry(double threshold, @NotNull ModelConfiguration model) {
        this.entries.add(new Entry(threshold, model));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        section.set("type", "minecraft:range_dispatch");
        section.set("property", this.property);
        if (this.scale != null) {
            section.set("scale", this.scale);
        }
        if (this.fallback != null) {
            section.set("fallback", ConfigurationSerializationUtils.toMap(this.fallback));
        }
        if (!this.entries.isEmpty()) {
            List<Map<String, Object>> serializedEntries = new ArrayList<>();
            for (Entry entry : this.entries) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("threshold", entry.getThreshold());
                map.put("model", ConfigurationSerializationUtils.toMap(entry.getModel()));
                serializedEntries.add(map);
            }
            section.set("entries", serializedEntries);
        }
    }

    public static class Entry {
        private final double threshold;
        private final ModelConfiguration model;

        public Entry(double threshold, @NotNull ModelConfiguration model) {
            this.threshold = threshold;
            this.model = Objects.requireNonNull(model, "model cannot be null");
        }

        public double getThreshold() {
            return this.threshold;
        }

        public ModelConfiguration getModel() {
            return this.model;
        }
    }
}
