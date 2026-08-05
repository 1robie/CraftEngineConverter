package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;

import com.google.gson.JsonObject;

public class HasComponentPredicate extends ConditionPredicate {
    private final String component;

    public HasComponentPredicate(String component) {
        super(true);
        this.component = component;
    }

    public HasComponentPredicate(String component, boolean expected) {
        super(expected);
        this.component = component;
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        jsonObject.addProperty("component", this.component);
    }

    @Override
    protected String propertyName() {
        return "has_component";
    }
}
