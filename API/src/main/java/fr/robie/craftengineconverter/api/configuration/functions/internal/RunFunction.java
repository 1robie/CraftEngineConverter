package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.functions.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RunFunction extends AbstractEventFunction {
    private final List<Function> functions = new ArrayList<>();
    private Integer delay;

    public RunFunction() {
        super("run");
    }

    public void addFunction(Function function) {
        this.functions.add(function);
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.delay != null) {
            map.put("delay", this.delay);
        }
        List<Map<String, Object>> serializedFunctions = new ArrayList<>();
        for (Function function : this.functions) {
            serializedFunctions.add(function.serialize());
        }
        map.put("functions", serializedFunctions);
        return map;
    }
}
