package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemRenderer implements EntityRenderer {
    private final String type = "item";
    private String item;
    private String position;

    public ItemRenderer setItem(String item) {
        this.item = item;
        return this;
    }

    public ItemRenderer setPosition(String position) {
        this.position = position;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", this.type);
        if (this.item != null) {
            map.put("item", this.item);
        }
        if (this.position != null) {
            map.put("position", this.position);
        }
        return map;
    }
}
