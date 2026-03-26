package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class DamageItemFunction extends AbstractEventFunction {
    private int amount = 1;

    public DamageItemFunction() {
        super("damage_item");
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.amount != 1) {
            map.put("amount", this.amount);
        }
        return map;
    }
}
