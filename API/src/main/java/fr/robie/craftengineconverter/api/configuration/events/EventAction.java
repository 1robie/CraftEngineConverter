package fr.robie.craftengineconverter.api.configuration.events;

import fr.robie.craftengineconverter.api.configuration.functions.Function;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventAction {
    private final EventTrigger eventTrigger;
    private final List<Function> functions = new ArrayList<>();

    public EventAction(@NotNull EventTrigger eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    public void addFunction(@NotNull Function function) {
        this.functions.add(function);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> serializedFunctions = new ArrayList<>();
        for (Function function : this.functions) {
            serializedFunctions.add(function.serialize());
        }
        map.put(this.eventTrigger.getKey().toLowerCase(), serializedFunctions);
        return map;

    }
}
