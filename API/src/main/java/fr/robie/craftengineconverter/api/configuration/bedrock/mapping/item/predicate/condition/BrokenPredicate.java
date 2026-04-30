package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;

public class BrokenPredicate extends BedrockPredicate {

    public BrokenPredicate(boolean expected) {
        super(expected);
    }

    @Override
    protected String propertyName() {
        return "broken";
    }
}
