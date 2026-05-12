package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import org.jetbrains.annotations.NotNull;

public class CustomModelDataPredicate extends BedrockPredicate {
    private final int index;
    private final String value;

    public CustomModelDataPredicate(int index, @NotNull String value) {
        this(index, value, true);
    }

    public CustomModelDataPredicate(int index, @NotNull String value, boolean expected) {
        super(expected);
        this.index = index;
        this.value = value;
        this.withType(Type.MATCH);
    }

    @Override
    protected void addExtraFields(com.google.gson.JsonObject jsonObject) {
        jsonObject.addProperty("index", this.index);
        jsonObject.addProperty("value", this.value);
    }

    @Override
    protected String propertyName() {
        return "custom_model_data";
    }
}
