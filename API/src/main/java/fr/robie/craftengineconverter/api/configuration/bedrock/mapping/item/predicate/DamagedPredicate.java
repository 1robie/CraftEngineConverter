package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

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
