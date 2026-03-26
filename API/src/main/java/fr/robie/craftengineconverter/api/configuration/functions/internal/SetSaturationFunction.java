package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class SetSaturationFunction extends AbstractEventFunction {
    private final double saturation;
    private boolean add = false;
    private PlayerTarget target = PlayerTarget.SELF;

    public SetSaturationFunction(double saturation) {
        super("set_saturation");
        this.saturation = saturation;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("saturation", this.saturation);
        if (this.add) {
            map.put("add", true);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        return map;
    }
}
