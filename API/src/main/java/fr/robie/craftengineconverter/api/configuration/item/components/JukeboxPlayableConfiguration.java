package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class JukeboxPlayableConfiguration implements ItemConfigurationSerializable {
    private final String song;

    public JukeboxPlayableConfiguration(@NotNull String song) {
        this.song = song;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection jukeboxPlayableComponent = this.getOrCreateSection(components, "minecraft:jukebox_playable");
        jukeboxPlayableComponent.set("song", this.song);
    }
}
