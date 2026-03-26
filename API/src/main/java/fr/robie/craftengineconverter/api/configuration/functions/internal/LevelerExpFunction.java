package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class LevelerExpFunction extends AbstractEventFunction {
    private final String plugin;
    private final String leveler;
    private final int count;

    public LevelerExpFunction(String plugin, String leveler, int count) {
        super("leveler_exp");
        this.plugin = plugin;
        this.leveler = leveler;
        this.count = count;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("plugin", this.plugin);
        map.put("leveler", this.leveler);
        map.put("count", this.count);
        return map;
    }
}
