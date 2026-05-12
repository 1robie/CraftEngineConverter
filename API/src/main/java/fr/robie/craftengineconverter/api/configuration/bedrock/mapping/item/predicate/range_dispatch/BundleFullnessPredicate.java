package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch;

public class BundleFullnessPredicate extends RangeDispatchPredicate {


    public BundleFullnessPredicate(double threshold, Float scale) {
        super(threshold, scale, null);
    }

    @Override
    protected String propertyName() {
        return "bundle_fullness";
    }
}
