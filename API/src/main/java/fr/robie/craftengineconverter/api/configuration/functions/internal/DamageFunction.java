package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class DamageFunction extends AbstractEventFunction {
    private double amount = 1.0;
    private PlayerTarget target = PlayerTarget.SELF;
    private String damageType = "generic";

    public DamageFunction() {
        super("damage");
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    public void setDamageType(String damageType) {
        this.damageType = damageType;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.amount != 1.0) {
            map.put("amount", this.amount);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        if (!"generic".equals(this.damageType)) {
            map.put("damage-type", this.damageType);
        }
        return map;
    }
}
