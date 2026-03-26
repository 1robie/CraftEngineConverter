package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.functions.Function;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;

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
        map.put("type", this.type);
        if (!this.conditions.isEmpty()) {
            map.put("conditions", ConfigurationSerializationUtils.serializeCollection(this.conditions, ConfigurationSerializationUtils::toMap));
        }
        return map;
    }
}
