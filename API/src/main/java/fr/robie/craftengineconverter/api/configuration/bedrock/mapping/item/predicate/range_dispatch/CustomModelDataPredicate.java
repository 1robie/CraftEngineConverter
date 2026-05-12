package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch;

public class CustomModelDataPredicate extends RangeDispatchPredicate {

    public CustomModelDataPredicate(double threshold, Float scale) {
        super(threshold, scale, null);
    }

    @Override
    protected String propertyName() {
        return "custom_model_data";
    }
}
