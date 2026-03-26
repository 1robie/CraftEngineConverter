package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class SetCooldownFunction extends AbstractEventFunction {
    private final String id;
    private final String time;
    private Boolean add;

    public SetCooldownFunction(String id, String time) {
        super("set_cooldown");
        this.id = id;
        this.time = time;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("id", id);
        map.put("time", time);
        if (add != null) {
            map.put("add", add);
        }
        return map;
    }
}
