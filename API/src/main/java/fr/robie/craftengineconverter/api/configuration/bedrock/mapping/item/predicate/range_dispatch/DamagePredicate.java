package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.range_dispatch;

public class DamagePredicate extends RangeDispatchPredicate {
    
    public DamagePredicate(double threshold, Float scale, Boolean normalize) {
        super(threshold, scale, normalize);
    }

    @Override
    protected String propertyName() {
        return "damage";
    }
}
