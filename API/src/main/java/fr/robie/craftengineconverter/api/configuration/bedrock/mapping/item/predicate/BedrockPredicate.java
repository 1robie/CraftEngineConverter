package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public abstract class BedrockPredicate {
    protected final boolean expected;

    protected BedrockPredicate() {
        this(true);
    }

    protected BedrockPredicate(boolean expected) {
        this.expected = expected;
    }

    protected abstract String propertyName();

    protected void addExtraFields(JsonObject jsonObject) {
    }

    public @NotNull JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "condition");
        jsonObject.addProperty("property", this.propertyName());
        if (!this.expected) {
            jsonObject.addProperty("expected", false);
        }
        this.addExtraFields(jsonObject);
        return jsonObject;
    }
}
