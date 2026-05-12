package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public abstract class BedrockPredicate {
    private Type type;
    protected final boolean expected;

    protected BedrockPredicate() {
        this(true);
    }

    protected BedrockPredicate(boolean expected) {
        this.expected = expected;
    }

    public BedrockPredicate withType(Type type) {
        this.type = type;
        return this;
    }

    public Type getType() {
        return this.type;
    }

    protected abstract String propertyName();

    protected void addExtraFields(JsonObject jsonObject) {
    }

    public @NotNull JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", this.type.name().toLowerCase());
        jsonObject.addProperty("property", this.propertyName());
        if (!this.expected) {
            jsonObject.addProperty("expected", false);
        }
        this.addExtraFields(jsonObject);
        return jsonObject;
    }

    public enum Type {
        CONDITION,
        MATCH,
        RANGE_DISPATCH
    }
}
