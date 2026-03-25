package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeModifiersConfiguration implements ItemConfigurationSerializable {
    private final List<AttributeModifier> attributeModifiers;

    public AttributeModifiersConfiguration(List<AttributeModifier> attributeModifiers) {
        this.attributeModifiers = attributeModifiers;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        ConfigurationSection data = this.getOrCreateSection(itemSection, "data");
        List<Map<String, Object>> serializedModifiers = new ArrayList<>();
        for (AttributeModifier modifier : this.attributeModifiers) {
            Map<String, Object> serializedModifier = new HashMap<>();
            serializedModifier.put("type", modifier.type().toLowerCase());
            serializedModifier.put("amount", modifier.amount());
            serializedModifier.put("operation", modifier.operation().id());
            if (modifier.id() != null) {
                serializedModifier.put("id", modifier.id().asString());
            }
            serializedModifier.put("slot", modifier.slot().name().toLowerCase());
            if (modifier.display() != null) {
                serializedModifier.put("display", Map.of(
                        "type", modifier.display().type().name().toLowerCase(),
                        "value", modifier.display().value()
                ));
            }
            serializedModifiers.add(serializedModifier);
        }
        data.set("attribute-modifiers", serializedModifiers);
    }
}
