package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class FuelTimeSettingConfiguration implements ItemConfigurationSerializable {
    private final int fuelTime;

    public FuelTimeSettingConfiguration(int fuelTime) {
        this.fuelTime = fuelTime;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.fuelTime <= 0) {
            return;
        }

        ConfigurationSection settings = this.getOrCreateSection(itemSection, "settings");
        settings.set("fuel-time", this.fuelTime);
    }
}
