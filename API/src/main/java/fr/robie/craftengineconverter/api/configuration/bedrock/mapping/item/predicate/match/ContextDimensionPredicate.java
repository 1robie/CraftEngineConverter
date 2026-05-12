package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import org.jetbrains.annotations.NotNull;

public class ContextDimensionPredicate extends BedrockPredicate {
    private final String dimension;

    public ContextDimensionPredicate(@NotNull String dimension) {
        this(dimension, true);
    }

    public ContextDimensionPredicate(@NotNull String dimension, boolean expected) {
        super(expected);
        this.dimension = dimension;
        this.withType(Type.MATCH);
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        jsonObject.addProperty("value", this.dimension);
    }

    @Override
    protected String propertyName() {
        return "context_dimension";
    }
}
