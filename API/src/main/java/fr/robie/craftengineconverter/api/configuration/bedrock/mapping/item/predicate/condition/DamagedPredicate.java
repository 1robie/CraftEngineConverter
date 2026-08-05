package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;


public class DamagedPredicate extends ConditionPredicate {
    public DamagedPredicate() {
        super(true);
    }

    public DamagedPredicate(boolean expected) {
        super(expected);
    }

    @Override
    protected String propertyName() {
        return "damaged";
    }
}
