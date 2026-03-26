package fr.robie.craftengineconverter.api.configuration.item;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SectionProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public interface ItemConfigurationSerializable extends SectionProvider {

    void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId);
}
