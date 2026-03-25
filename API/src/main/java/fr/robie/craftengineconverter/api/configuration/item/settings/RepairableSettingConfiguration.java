package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class RepairableSettingConfiguration implements ItemConfigurationSerializable {
    private final boolean allowCraftingTableRepairing;
    private final boolean allowAnvilRepairing;
    private final boolean allowAnvilCombining;

    public RepairableSettingConfiguration(boolean allowCraftingTableRepairing, boolean allowAnvilRepairing, boolean allowAnvilCombining) {
        this.allowCraftingTableRepairing = allowCraftingTableRepairing;
        this.allowAnvilRepairing = allowAnvilRepairing;
        this.allowAnvilCombining = allowAnvilCombining;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection settings = this.getOrCreateSection(itemSection, "settings");
        if (this.allowAnvilCombining && this.allowAnvilRepairing && this.allowCraftingTableRepairing) {
            settings.set("repairable", true);
        } else {
            ConfigurationSection repairableSettings = this.getOrCreateSection(settings, "repairable");
            repairableSettings.set("crafting-table", this.allowCraftingTableRepairing);
            repairableSettings.set("anvil-repair", this.allowAnvilRepairing);
            repairableSettings.set("anvil-combine", this.allowAnvilCombining);
        }
    }
}
