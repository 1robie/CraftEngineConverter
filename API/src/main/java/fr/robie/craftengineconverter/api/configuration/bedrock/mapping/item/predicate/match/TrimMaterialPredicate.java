package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.match;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;
import org.jetbrains.annotations.NotNull;

public class TrimMaterialPredicate extends BedrockPredicate {
    private final String material;

    public TrimMaterialPredicate(@NotNull String material) {
        this(material, true);
    }

    public TrimMaterialPredicate(@NotNull String material, boolean expected) {
        super(expected);
        this.material = material;
        this.withType(Type.MATCH);
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        jsonObject.addProperty("value", this.material);
    }

    @Override
    protected String propertyName() {
        return "trim_material";
    }
}
