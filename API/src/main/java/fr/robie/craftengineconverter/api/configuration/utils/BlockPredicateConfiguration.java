package fr.robie.craftengineconverter.api.configuration.utils;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockPredicateConfiguration implements ItemConfigurationSerializable {

    public enum Type {
        CAN_PLACE_ON("minecraft:can_place_on"),
        CAN_BREAK("minecraft:can_break");

        private final String componentKey;

        Type(String componentKey) {
            this.componentKey = componentKey;
        }

        public String getComponentKey() {
            return this.componentKey;
        }
    }

    public record BlockPredicate(Object blocks) {
    }

    private final Type type;
    private final List<BlockPredicate> predicates;

    public BlockPredicateConfiguration(Type type, List<BlockPredicate> predicates) {
        this.type = type;
        this.predicates = predicates;
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.predicates == null || this.predicates.isEmpty()) {
            return;
        }

        ConfigurationSection components = this.getOrCreateSection(itemSection, "components");
        ConfigurationSection predicateSection = this.getOrCreateSection(components, this.type.getComponentKey());

        List<Map<String, Object>> serialized = new ArrayList<>();
        for (BlockPredicate predicate : this.predicates) {
            Map<String, Object> map = new HashMap<>();
            map.put("blocks", predicate.blocks());
            serialized.add(map);
        }

        predicateSection.set("predicates", serialized);
    }
}