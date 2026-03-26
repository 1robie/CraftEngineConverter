package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.functions.Function;
import fr.robie.craftengineconverter.api.utils.ConfigurationSerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IfElseFunction extends AbstractEventFunction {
    private final List<Rule> rules = new ArrayList<>();

    public IfElseFunction() {
        super("if_else");
    }

    public void addRule(@NotNull Rule rule) {
        this.rules.add(rule);
    }

    public record Rule(List<Condition> conditions, List<Function> functions) {
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            if (this.conditions != null && !this.conditions.isEmpty()) {
                map.put("conditions", ConfigurationSerializationUtils.serializeCollection(this.conditions, ConfigurationSerializationUtils::toMap));
            }
            map.put("functions", ConfigurationSerializationUtils.serializeCollection(this.functions, Function::serialize));
            return map;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("rules", ConfigurationSerializationUtils.serializeCollection(this.rules, Rule::serialize));
        return map;
    }
}
