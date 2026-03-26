package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InventoryHasItemCondition extends AbstractCondition {
    private final String id;
    private final int count;

    public InventoryHasItemCondition(@NotNull String id, int count) {
        super("inventory_has_item");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.count = count;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("id", this.id);
        section.set("count", this.count);
    }
}
