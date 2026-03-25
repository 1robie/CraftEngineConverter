package fr.robie.craftengineconverter.api.configuration.events;


public enum EventTrigger {
    BREAK,
    RIGHT_CLICK,
    LEFT_CLICK,
    CONSUME,
    PICK_UP,

    PLACE,
    STEP;


    public String getKey() {
        return this.name();
    }
}
