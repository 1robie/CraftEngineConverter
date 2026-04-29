package fr.robie.craftengineconverter.api.configuration.loader.models.condition;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.condition.KeybindDownConditionConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.AbstractConditionLoader;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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