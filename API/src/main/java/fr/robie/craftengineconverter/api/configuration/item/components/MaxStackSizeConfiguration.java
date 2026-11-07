package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MaxStackSizeConfiguration implements ItemConfigurationSerializable {
    private final int maxStackSize;

    public MaxStackSizeConfiguration(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getComponentsSection(itemSection).set("minecraft:max_stack_size", this.maxStackSize);
    }

}
