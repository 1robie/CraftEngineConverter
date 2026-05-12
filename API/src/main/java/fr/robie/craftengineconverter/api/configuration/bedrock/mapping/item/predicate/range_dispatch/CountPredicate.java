package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch;

public class CountPredicate extends RangeDispatchPredicate {

    public CountPredicate(double threshold, Float scale, Boolean normalize) {
        super(threshold, scale, normalize);
    }

    @Override
    protected String propertyName() {
        return "count";
    }
}
