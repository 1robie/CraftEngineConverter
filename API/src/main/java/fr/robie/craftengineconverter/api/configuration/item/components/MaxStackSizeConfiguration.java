package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class MaxStackSizeConfiguration implements ItemConfigurationSerializable, BedrockComponent {
    private final int maxStackSize;

    public MaxStackSizeConfiguration(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        this.getOrCreateSection(itemSection, "components").set("minecraft:max_stack_size", this.maxStackSize);
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        componentObject.addProperty("minecraft:max_stack_size", this.maxStackSize);
    }
}
