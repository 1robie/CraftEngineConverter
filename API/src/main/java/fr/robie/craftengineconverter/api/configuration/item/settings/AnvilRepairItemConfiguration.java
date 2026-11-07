package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import net.momirealms.craftengine.core.item.ItemSelector;
import net.momirealms.craftengine.core.item.setting.value.AnvilRepairItem;
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
            List<String> targets = new ArrayList<>();
            for (ItemSelector selector : repairItem.targets()) {
                targets.add(selectorId(selector));
            }
            if (targets.size() == 1) {
                repairItemMap.put("target", targets.getFirst());
            } else {
                repairItemMap.put("target", targets);
            }
            if (repairItem.amount().getDouble() > 0) {
                repairItemMap.put("amount", repairItem.amount().getInt());
            }
            if (repairItem.percent().getDouble() > 0) {
                repairItemMap.put("percent", repairItem.percent().getDouble());
            }
            repairItemsList.add(repairItemMap);
        }
        settings.set("anvil-repair-item", repairItemsList);
    }

    private static String selectorId(ItemSelector selector) {
        if (selector instanceof ItemSelector.Tag tag) {
            return "#" + tag.tag();
        }
        if (selector instanceof ItemSelector.Item item) {
            return item.id().toString();
        }
        return selector.toString();
    }
}
