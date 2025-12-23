package fr.robie.craftengineconverter.utils.enums;

import java.util.ArrayList;
import java.util.List;

public enum NexoMinimalType {
    WOODEN,
    STONE,
    IRON,
    GOLDEN,
    DIAMOND,
    NETHERITE
    ;

    public List<String> getCorrectTools(){
        List<String> tools = new ArrayList<>();
        for (String tool : new String[]{"axe", "pickaxe", "shovel", "hoe", "sword"}) {
            tools.add("minecraft:"+this.name().toLowerCase()+"_"+tool);
        }
        return tools;
    }
}
