package fr.robie.craftengineconverter.api.configuration.recipe.postprocessor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base for post-processors that hold a single list of strings (components, tags, paths, …).
 * Subclasses only need to supply the processor type key and the YAML list key.
 */
public abstract class AbstractListPostProcessor extends PostProcessor {
    private final String listKey;
    private final List<String> items = new ArrayList<>();

    protected AbstractListPostProcessor(@NotNull String type, @NotNull String listKey) {
        super(type);
        this.listKey = listKey;
    }

    public void addItem(@NotNull String item) {
        this.items.add(item);
    }

    public @NotNull List<String> getItems() {
        return this.items;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (!this.items.isEmpty()) {
            map.put(this.listKey, List.copyOf(this.items));
        }
        return map;
    }
}
