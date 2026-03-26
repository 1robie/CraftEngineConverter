package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.functions.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhenFunction extends AbstractEventFunction {
    private final String source;
    private final List<Case> cases = new ArrayList<>();
    private List<Function> fallback;

    public WhenFunction(String source) {
        super("when");
        this.source = source;
    }

    public void addCase(Case c) {
        this.cases.add(c);
    }

    public void setFallback(List<Function> fallback) {
        this.fallback = fallback;
    }

    public record Case(Object when, List<Function> functions) {
        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("when", this.when);
            List<Map<String, Object>> serializedFunctions = new ArrayList<>();
            for (Function function : this.functions) serializedFunctions.add(function.serialize());
            map.put("functions", serializedFunctions);
            return map;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("source", this.source);
        List<Map<String, Object>> serializedCases = new ArrayList<>();
        for (Case c : this.cases) serializedCases.add(c.serialize());
        map.put("cases", serializedCases);
        if (this.fallback != null) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (Function f : this.fallback) serialized.add(f.serialize());
            map.put("fallback", serialized);
        }
        return map;
    }
}
