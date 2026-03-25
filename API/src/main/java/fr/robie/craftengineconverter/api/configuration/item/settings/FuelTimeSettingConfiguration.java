package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
