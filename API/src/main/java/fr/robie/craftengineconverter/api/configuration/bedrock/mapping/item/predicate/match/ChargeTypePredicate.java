package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import fr.robie.craftengineconverter.api.configuration.item.models.range_dispatch.ChargeType;
import org.jetbrains.annotations.NotNull;

public class ChargeTypePredicate extends BedrockPredicate {
    private final ChargeType expected;

    public ChargeTypePredicate(@NotNull ChargeType expected) {
        this.expected = expected;
        this.withType(Type.MATCH);
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        if (this.expected == ChargeType.NONE) return;
        jsonObject.addProperty("value", this.expected.name().toLowerCase());
    }

    @Override
    protected String propertyName() {
        return "charge_type";
    }
}
