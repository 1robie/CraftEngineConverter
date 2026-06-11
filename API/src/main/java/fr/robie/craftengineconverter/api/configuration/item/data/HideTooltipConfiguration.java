package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.craftengineconverter.api.enums.ComponentFlag;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HideTooltipConfiguration implements ItemConfigurationSerializable {
    private final Set<ComponentFlag> flags = new HashSet<>();

    public HideTooltipConfiguration(Collection<ComponentFlag> flags) {
        this.flags.addAll(flags);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        List<String> flagNames = new ArrayList<>();
        for (ComponentFlag flag : this.flags) {
            flagNames.add(flag.getKey());
        }
        this.getOrCreateSection(itemSection, "data").set("hide-tooltip", flagNames);
    }
}
