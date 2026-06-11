package fr.robie.craftengineconverter.api.configuration.item.loottables.entries;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemEntry extends AbstractLootEntry {
    private final String item;

    public ItemEntry(@NotNull String item) {
        super("item");
        this.item = this.namespaced(Objects.requireNonNull(item, "item cannot be null"));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("item", this.item);
    }
}
