package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;

public class DamagedPredicate extends BedrockPredicate {
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
