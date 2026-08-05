package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.condition;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.predicate.BedrockPredicate;

/**
 * Base for the boolean predicates Geyser groups under {@code "type": "condition"}.
 * <p>
 * Mirrors {@code RangeDispatchPredicate}, which tags its type in the constructor rather than leaving it
 * to every subclass. Without the tag the serialized predicate carries only a {@code property} and
 * Geyser cannot tell which predicate family it belongs to.
 */
public abstract class ConditionPredicate extends BedrockPredicate {

    protected ConditionPredicate(boolean expected) {
        super(expected);
        this.withType(Type.CONDITION);
    }
}
