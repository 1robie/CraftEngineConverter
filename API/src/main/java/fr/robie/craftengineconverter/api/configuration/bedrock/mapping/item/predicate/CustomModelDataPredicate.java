package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

import com.google.gson.JsonObject;

public class CustomModelDataPredicate extends BedrockPredicate {
    private final int index;

    public CustomModelDataPredicate(int index) {
        this(index, true);
    }

    public CustomModelDataPredicate(int index, boolean expected) {
        super(expected);
        this.index = index;
    }

    @Override
    protected String propertyName() {
        return "custom_model_data";
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        jsonObject.addProperty("index", this.index);
    }
}
