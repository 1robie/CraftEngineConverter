package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

public class BrokenPredicate extends BedrockPredicate {

    public BrokenPredicate(boolean expected) {
        super(expected);
    }

    @Override
    protected String propertyName() {
        return "broken";
    }
}
