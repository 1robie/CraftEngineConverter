package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import net.momirealms.craftengine.core.item.setting.AnvilRepairItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnvilRepairItemConfiguration implements ItemConfigurationSerializable {
    private final List<AnvilRepairItem> repairItems;

    public AnvilRepairItemConfiguration(List<AnvilRepairItem> repairItems) {
        this.repairItems = repairItems;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection settings = this.getOrCreateSection(itemSection, "settings");
        List<Map<String, Object>> repairItemsList = new ArrayList<>();
        for (AnvilRepairItem repairItem : this.repairItems) {
            Map<String, Object> repairItemMap = new HashMap<>();
            if (repairItem.targets().size() == 1) {
                repairItemMap.put("target", repairItem.targets().getFirst());
            } else {
                repairItemMap.put("target", repairItem.targets());
            }
            if (repairItem.amount() > 0) {
                repairItemMap.put("amount", repairItem.amount());
            }
            if (repairItem.percent() > 0) {
                repairItemMap.put("percent", repairItem.percent());
            }
            repairItemsList.add(repairItemMap);
        }
        settings.set("anvil-repair-item", repairItemsList);
    }
}
