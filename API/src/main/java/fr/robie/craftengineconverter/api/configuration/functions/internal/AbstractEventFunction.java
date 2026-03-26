package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.functions.Function;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractEventFunction implements Function {
    private final String type;
    private final List<Condition> conditions = new ArrayList<>();

    protected AbstractEventFunction(String type) {
        this.type = type;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        if (!this.conditions.isEmpty()) {
            List<Map<String, Object>> serializedConditions = new ArrayList<>();
            for (Condition condition : this.conditions) {
                YamlConfiguration temp = new YamlConfiguration();
                condition.serialize(temp);
                serializedConditions.add(temp.getValues(true));
            }
            map.put("conditions", serializedConditions);
        }
        return map;
    }
}
