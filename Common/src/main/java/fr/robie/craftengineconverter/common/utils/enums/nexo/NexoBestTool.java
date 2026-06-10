package fr.robie.craftengineconverter.common.utils.enums.nexo;

import java.util.Locale;

public enum NexoBestTool {
    AXE,
    PICKAXE,
    SHOVEL,
    HOE,
    SWORD("minecraft:sword_efficient");

    private final String tagName;

    NexoBestTool() {
        this.tagName = "minecraft:mineable/"+this.name().toLowerCase(Locale.ROOT);
    }

    NexoBestTool(String tagName) {
        this.tagName = tagName;
    }

    public String getBestTool(){
        return this.tagName;
    }
}
