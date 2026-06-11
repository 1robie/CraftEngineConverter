package fr.robie.craftengineconverter.api.configuration.events;


import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventConfiguration implements Event {
    private final List<EventAction> actions = new ArrayList<>();

    public void addAction(EventAction action) {
        this.actions.add(action);
    }

    public void serialize(@NotNull ConfigurationSection section) {
        if (this.actions.isEmpty()) {
            return;
        }

        Map<String, Object> eventsMap = new java.util.HashMap<>();
        for (EventAction action : this.actions) {
            eventsMap.putAll(action.serialize());
        }
        section.set("events", eventsMap);
    }
}
