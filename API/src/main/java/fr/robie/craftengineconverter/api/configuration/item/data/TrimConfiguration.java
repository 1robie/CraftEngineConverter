package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * The armour trim baked onto an item stack. CraftEngine has a dedicated {@code trim} processor for this, so it goes
 * straight on {@code data} rather than among the raw components; the processor requires both halves.
 */
public class TrimConfiguration implements ItemConfigurationSerializable {
    private final String material;
    private final String pattern;

    public TrimConfiguration(@NotNull String material, @NotNull String pattern) {
        this.material = material;
        this.pattern = pattern;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection trimSection = this.getOrCreateSection(this.getDataSection(itemSection), "trim");
        trimSection.set("material", this.material);
        trimSection.set("pattern", this.pattern);
    }
}
