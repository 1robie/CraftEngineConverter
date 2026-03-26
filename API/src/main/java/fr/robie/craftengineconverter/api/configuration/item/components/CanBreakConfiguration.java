package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.craftengineconverter.api.configuration.utils.BlockPredicateConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CanBreakConfiguration implements ItemConfigurationSerializable {
    private final BlockPredicateConfiguration delegate;

    public CanBreakConfiguration(List<BlockPredicateConfiguration.BlockPredicate> predicates) {
        this.delegate = new BlockPredicateConfiguration(BlockPredicateConfiguration.Type.CAN_BREAK, predicates);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.delegate.serialize(yamlConfiguration, path, itemSection, itemId);
    }
}