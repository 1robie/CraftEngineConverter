package fr.robie.craftengineconverter.api.configuration.loader.models.condition;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.KeybindDownConditionConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.AbstractConditionLoader;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoModelConfigurationLoader({"keybind_down", "minecraft:keybind_down"})
public class KeybindDownConditionLoader extends AbstractConditionLoader {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        String keybind = section.getString("keybind");
        if (keybind == null) {
            return null;
        }

        KeybindDownConditionConfiguration config = new KeybindDownConditionConfiguration(keybind);
        this.loadBranches(config, section);
        return config;
    }
}