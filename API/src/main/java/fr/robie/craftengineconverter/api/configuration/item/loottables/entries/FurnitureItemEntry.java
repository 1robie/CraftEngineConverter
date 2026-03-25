package fr.robie.craftengineconverter.api.configuration.item.loottables.entries;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FurnitureItemEntry extends AbstractLootEntry {
    private final String item;

    public FurnitureItemEntry(@NotNull String fallbackItem) {
        super("furniture_item");
        this.item = this.namespaced(Objects.requireNonNull(fallbackItem, "fallbackItem cannot be null"));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("item", this.item);
    }
}
