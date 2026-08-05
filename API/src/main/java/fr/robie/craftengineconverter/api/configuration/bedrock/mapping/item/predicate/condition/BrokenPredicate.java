package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;


public class BrokenPredicate extends ConditionPredicate {

    public BrokenPredicate(boolean expected) {
        super(expected);
    }

    @Override
    protected String propertyName() {
        return "broken";
    }
}
