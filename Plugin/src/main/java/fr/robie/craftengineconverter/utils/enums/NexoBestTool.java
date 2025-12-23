package fr.robie.craftengineconverter.utils.enums;

public enum NexoBestTool {
    AXE,
    PICKAXE,
    SHOVEL,
    HOE,
    SWORD("minecraft:sword_efficient");

    private final String tagName;

    NexoBestTool() {
        this.tagName = "minecraft:mineable/"+this.name().toLowerCase();
    }

    NexoBestTool(String tagName) {
        this.tagName = tagName;
    }

    public String getBestTool(){
        return this.tagName;
    }
}
