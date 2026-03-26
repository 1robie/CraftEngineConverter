package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class RemoveCooldownFunction extends AbstractEventFunction {
    private String id;
    private Boolean all;

    public RemoveCooldownFunction() {
        super("remove_cooldown");
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.id != null) {
            map.put("id", this.id);
        }
        if (this.all != null) {
            map.put("all", this.all);
        }
        return map;
    }
}
