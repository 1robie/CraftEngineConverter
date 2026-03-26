package fr.robie.craftengineconverter.api.configuration.item.loottables;

import fr.robie.craftengineconverter.api.configuration.item.loottables.functions.LootFunction;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LootTable implements LootConfiguration {
    private final List<LootFunction> functions = new ArrayList<>();
    private final List<LootPool> pools = new ArrayList<>();

    public void addFunction(@NotNull LootFunction function) {
        this.functions.add(Objects.requireNonNull(function, "function cannot be null"));
    }

    public void addPool(@NotNull LootPool pool) {
        this.pools.add(Objects.requireNonNull(pool, "pool cannot be null"));
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        if (this.functions.isEmpty() && this.pools.isEmpty())
            return;

        ConfigurationSection lootSection = section.createSection("loot");

        if (!this.functions.isEmpty()) {
            lootSection.set("functions", ConfigurationSerializationUtils.serializeCollection(this.functions, ConfigurationSerializationUtils::toMap));
        }

        if (!this.pools.isEmpty()) {
            lootSection.set("pools", ConfigurationSerializationUtils.serializeCollection(this.pools, ConfigurationSerializationUtils::toMap));
        }
    }
}
