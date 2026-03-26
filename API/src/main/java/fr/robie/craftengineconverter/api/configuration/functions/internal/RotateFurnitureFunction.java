package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.configuration.functions.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RotateFurnitureFunction extends AbstractEventFunction {
    private final int degree;
    private final List<Function> onSuccess = new ArrayList<>();
    private final List<Function> onFailure = new ArrayList<>();

    public RotateFurnitureFunction(int degree) {
        super("rotate_furniture");
        this.degree = degree;
    }

    public void addOnSuccess(Function function) {
        this.onSuccess.add(function);
    }

    public void addOnFailure(Function function) {
        this.onFailure.add(function);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("degree", this.degree);
        if (!this.onSuccess.isEmpty()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (Function f : this.onSuccess) serialized.add(f.serialize());
            map.put("on-success", serialized);
        }
        if (!this.onFailure.isEmpty()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (Function f : this.onFailure) serialized.add(f.serialize());
            map.put("on-failure", serialized);
        }
        return map;
    }
}
