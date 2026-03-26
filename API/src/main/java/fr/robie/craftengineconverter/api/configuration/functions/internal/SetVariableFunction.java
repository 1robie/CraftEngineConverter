package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class SetVariableFunction extends AbstractEventFunction {
    private final String name;
    private Double number;
    private Boolean asInt;
    private String text;

    public SetVariableFunction(String name) {
        super("set_variable");
        this.name = name;
    }

    public void setNumber(double number) {
        this.number = number;
    }

    public void setAsInt(boolean asInt) {
        this.asInt = asInt;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("name", name);
        if (number != null) {
            map.put("number", number);
        }
        if (asInt != null) {
            map.put("as-int", asInt);
        }
        if (text != null) {
            map.put("text", text);
        }
        return map;
    }
}
