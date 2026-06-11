package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class UnbreakableConfiguration implements ItemConfigurationSerializable {
    private final boolean unbreakable;

    public UnbreakableConfiguration(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (!this.unbreakable) {
            return;
        }
        ConfigurationSection dataSection = this.getOrCreateSection(itemSection, "data");
        dataSection.set("unbreakable", true);
    }
}
