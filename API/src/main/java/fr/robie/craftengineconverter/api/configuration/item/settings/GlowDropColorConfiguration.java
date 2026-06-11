package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class GlowDropColorConfiguration implements ItemConfigurationSerializable {
    private final DyeColor color;

    public GlowDropColorConfiguration(@NotNull DyeColor color) {
        this.color = color;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "settings").set("glow-color", this.color.name().toLowerCase(Locale.ROOT));
    }
}
