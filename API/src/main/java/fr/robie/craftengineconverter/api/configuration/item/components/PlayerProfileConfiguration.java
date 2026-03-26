package fr.robie.craftengineconverter.api.configuration.item.components;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerProfileConfiguration implements ItemConfigurationSerializable {

    private final String name;
    private final String uuid;
    private final List<Property> properties;
    private final String texture;
    private final String cape;
    private final String elytra;
    private final String model;

    public PlayerProfileConfiguration(String name, String uuid, List<Property> properties, String texture, String cape, String elytra, String model) {
        this.name = name;
        this.uuid = uuid;
        this.properties = properties;
        this.texture = texture;
        this.cape = cape;
        this.elytra = elytra;
        this.model = model;
    }

    public record Property(String name, String value, String signature) {
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection profileSection = this.getOrCreateSection(components, "minecraft:profile");

        if (this.name != null && !this.name.isBlank()) {
            profileSection.set("name", this.name);
        }

        if (this.uuid != null && !this.uuid.isBlank()) {
            profileSection.set("id", this.uuid);
        }

        if (this.properties != null && !this.properties.isEmpty()) {
            List<Map<String, Object>> serializedProperties = new ArrayList<>();
            for (Property property : this.properties) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", property.name());
                map.put("value", property.value());
                if (property.signature() != null && !property.signature().isBlank()) {
                    map.put("signature", property.signature());
                }
                serializedProperties.add(map);
            }
            profileSection.set("properties", serializedProperties);
        }

        if (this.texture != null && !this.texture.isBlank()) {
            profileSection.set("texture", this.texture);
        }

        if (this.cape != null && !this.cape.isBlank()) {
            profileSection.set("cape", this.cape);
        }

        if (this.elytra != null && !this.elytra.isBlank()) {
            profileSection.set("elytra", this.elytra);
        }

        if (this.model != null && !this.model.isBlank()) {
            profileSection.set("model", this.model);
        }
    }
}