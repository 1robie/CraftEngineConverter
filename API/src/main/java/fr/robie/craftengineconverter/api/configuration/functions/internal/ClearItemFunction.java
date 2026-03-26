package fr.robie.craftengineconverter.api.configuration.functions.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ClearItemFunction extends AbstractEventFunction {
    private final String itemId;
    private int count = 1;

    public ClearItemFunction(@NotNull String itemId) {
        super("clear_item");
        this.itemId = itemId;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("id", this.itemId);
        if (this.count != 1) {
            map.put("count", this.count);
        }
        return map;
    }
}
