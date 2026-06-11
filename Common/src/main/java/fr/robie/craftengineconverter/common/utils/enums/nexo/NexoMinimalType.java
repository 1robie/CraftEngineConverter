package fr.robie.craftengineconverter.common.utils.enums.nexo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public enum NexoMinimalType {
    WOODEN,
    STONE,
    IRON,
    GOLDEN,
    DIAMOND,
    NETHERITE,
    WOOD(WOODEN);
    private final NexoMinimalType parent;

    NexoMinimalType() {
        this.parent = null;
    }

    NexoMinimalType(NexoMinimalType parent) {
        this.parent = parent;
    }

    public List<String> getCorrectTools() {
        List<String> tools = new ArrayList<>();
        for (String tool : new String[]{"axe", "pickaxe", "shovel", "hoe", "sword"}) {
            tools.add("minecraft:" + Objects.requireNonNullElse(this.parent, this).name().toLowerCase(Locale.ROOT) + "_" + tool);
        }
        return tools;
    }
}
