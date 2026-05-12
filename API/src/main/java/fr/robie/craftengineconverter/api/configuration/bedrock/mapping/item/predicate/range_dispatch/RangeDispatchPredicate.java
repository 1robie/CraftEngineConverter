package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;

public abstract class RangeDispatchPredicate extends BedrockPredicate {
    private final double threshold;
    private final Float scale;
    private final Boolean normalize;

    public RangeDispatchPredicate(double threshold, Float scale, Boolean normalize) {
        super(true);
        this.threshold = threshold;
        this.scale = scale;
        this.normalize = normalize;
        this.withType(Type.RANGE_DISPATCH);
    }

    @Override
    protected void addExtraFields(JsonObject jsonObject) {
        jsonObject.addProperty("threshold", this.threshold);
        if (this.scale != null && this.scale != 1.0f) {
            jsonObject.addProperty("scale", this.scale);
        }
        if (this.normalize != null) {
            jsonObject.addProperty("normalize", this.normalize);
        }
    }

}
