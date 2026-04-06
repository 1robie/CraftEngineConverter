package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate;

public class FishingRodCastPredicate extends BedrockPredicate {

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
