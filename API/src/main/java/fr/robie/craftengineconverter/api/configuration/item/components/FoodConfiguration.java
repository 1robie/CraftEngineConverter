package fr.robie.craftengineconverter.api.configuration.item.components;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.component.BedrockComponent;
import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class FoodConfiguration implements ItemConfigurationSerializable, BedrockComponent {
    private final int nutrition;
    private final float saturation;
    private final boolean canAlwaysEat;

    public FoodConfiguration(int nutrition, float saturation, boolean canAlwaysEat) {
        assert nutrition >= 0 : "Nutrition must be non-negative";
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.canAlwaysEat = canAlwaysEat;
    }

    public FoodConfiguration(int nutrition, float saturation) {
        this(nutrition, saturation, false);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection foodComponent = this.getOrCreateSection(components, "minecraft:food");
        foodComponent.set("nutrition", this.nutrition);
        foodComponent.set("saturation", this.saturation);
        if (this.canAlwaysEat) {
            foodComponent.set("can_always_eat", true);
        }
    }

    @Override
    public void applyTo(@NotNull JsonObject componentObject) {
        JsonObject foodComponent = new JsonObject();
        foodComponent.addProperty("nutrition", this.nutrition);
        foodComponent.addProperty("saturation", this.saturation);
        if (this.canAlwaysEat) {
            foodComponent.addProperty("can_always_eat", true);
        }
        componentObject.add("minecraft:food", foodComponent);
    }

}
