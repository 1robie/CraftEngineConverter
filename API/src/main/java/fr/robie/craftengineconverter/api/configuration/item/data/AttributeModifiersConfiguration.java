package fr.robie.craftengineconverter.api.configuration.item.data;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
            serializedModifier.put("type", modifier.type().toLowerCase(Locale.ROOT));
            serializedModifier.put("amount", modifier.amount());
            serializedModifier.put("operation", modifier.operation().id());
            if (modifier.id() != null) {
                serializedModifier.put("id", modifier.id().asString());
            }
            serializedModifier.put("slot", modifier.slot().name().toLowerCase(Locale.ROOT));
            if (modifier.display() != null) {
                serializedModifier.put("display", Map.of(
                        "type", modifier.display().type().name().toLowerCase(Locale.ROOT),
                        "value", modifier.display().value()
                ));
            }
            serializedModifiers.add(serializedModifier);
        }
        data.set("attribute-modifiers", serializedModifiers);
    }
}
