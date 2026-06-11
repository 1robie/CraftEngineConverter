package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.AbstractItemConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class ItemNameConfiguration extends AbstractItemConfiguration {
    private final String itemName;

    public ItemNameConfiguration(String itemName) {
        super(false);
        this.itemName = itemName;
    }

    public ItemNameConfiguration(String itemName, boolean applyNoItalic) {
        super(applyNoItalic);
        this.itemName = itemName;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.itemName == null || this.itemName.isEmpty()) {
            return;
        }
        ConfigurationSection data = this.getOrCreateSection(itemSection, "data");
        data.set("item-name", this.applyNoItalic(this.itemName));
    }
}
