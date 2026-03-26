package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class SetExpFunction extends AbstractEventFunction {
    private final int count;
    private boolean add = false;
    private PlayerTarget target = PlayerTarget.SELF;

    public SetExpFunction(int count) {
        super("set_exp");
        this.count = count;
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
        map.put("count", count);
        if (add) {
            map.put("add", true);
        }
        if (target != PlayerTarget.SELF) {
            map.put("target", target.name().toLowerCase());
        }
        return map;
    }
}
