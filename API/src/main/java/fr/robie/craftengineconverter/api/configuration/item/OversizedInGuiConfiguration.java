package fr.robie.craftengineconverter.api.configuration.item;

import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class OversizedInGuiConfiguration implements ItemConfigurationSerializable {
    private final boolean isOversizedInGui;

    public OversizedInGuiConfiguration(boolean isOversizedInGui) {
        this.isOversizedInGui = isOversizedInGui;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (!this.isOversizedInGui) {
            return;
        }
        itemSection.set("oversized-in-gui", true);
    }
}
