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
        map.put("name", this.name);
        if (this.number != null) {
            map.put("number", this.number);
        }
        if (this.asInt != null) {
            map.put("as-int", this.asInt);
        }
        if (this.text != null) {
            map.put("text", this.text);
        }
        return map;
    }
}
