package fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RangeDispatchModelConfiguration implements ModelConfiguration {
    private final String property;
    private Float scale;
    private final List<Entry> entries = new ArrayList<>();
    private ModelConfiguration fallback;

    public RangeDispatchModelConfiguration(@NotNull String property) {
        this.property = this.namespaced(property);
    }

    @NotNull
    public String getProperty() {
        return this.property;
    }

    @Nullable
    public Float getScale() {
        return this.scale;
    }

    @NotNull
    public List<Entry> getEntries() {
        return this.entries;
    }

    @Nullable
    public ModelConfiguration getFallback() {
        return this.fallback;
    }

    public void setScale(@Nullable Float scale) {
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
                map.put("threshold", entry.threshold());
                map.put("model", ConfigurationSerializationUtils.toMap(entry.model()));
                serializedEntries.add(map);
            }
            section.set("entries", serializedEntries);
        }
    }

    public record Entry(double threshold, ModelConfiguration model) {
        public Entry(double threshold, @NotNull ModelConfiguration model) {
            this.threshold = threshold;
            this.model = Objects.requireNonNull(model, "model cannot be null");
        }
    }
}
