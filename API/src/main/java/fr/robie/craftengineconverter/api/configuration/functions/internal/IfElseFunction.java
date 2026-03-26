package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.conditions.Condition;
import fr.robie.craftengineconverter.api.configuration.functions.Function;
import org.bukkit.configuration.file.YamlConfiguration;
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
                List<Map<String, Object>> serializedConditions = new ArrayList<>();
                for (Condition condition : this.conditions) {
                    YamlConfiguration temp = new YamlConfiguration();
                    condition.serialize(temp);
                    serializedConditions.add(temp.getValues(true));
                }
                map.put("conditions", serializedConditions);
            }
            List<Map<String, Object>> serializedFunctions = new ArrayList<>();
            for (Function function : this.functions) {
                serializedFunctions.add(function.serialize());
            }
            map.put("functions", serializedFunctions);
            return map;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        List<Map<String, Object>> serializedRules = new ArrayList<>();
        for (Rule rule : this.rules)
            serializedRules.add(rule.serialize());
        map.put("rules", serializedRules);
        return map;
    }
}
