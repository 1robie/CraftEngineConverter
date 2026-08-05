package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;


public class FishingRodCastPredicate extends ConditionPredicate {

    public FishingRodCastPredicate() {
        super(true);
    }

    public FishingRodCastPredicate(boolean expected) {
        super(expected);
    }

    @Override
    protected String propertyName() {
        return "fishing_rod_cast";
    }
}
