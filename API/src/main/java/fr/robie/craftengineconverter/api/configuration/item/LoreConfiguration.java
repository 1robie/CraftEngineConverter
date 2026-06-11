package fr.robie.craftengineconverter.api.configuration.item;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LoreConfiguration extends AbstractItemConfiguration {
    private final List<String> lore;

    public LoreConfiguration(List<String> lore) {
        super(false);
        this.lore = lore;
    }

    public LoreConfiguration(List<String> lore, boolean disableDefaultItalic) {
        super(disableDefaultItalic);
        this.lore = lore;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.lore == null || this.lore.isEmpty()) {
            return;
        }
        ConfigurationSection data = this.getOrCreateSection(itemSection, "data");
        data.set("lore", this.applyNoItalic(this.lore));
    }
}
