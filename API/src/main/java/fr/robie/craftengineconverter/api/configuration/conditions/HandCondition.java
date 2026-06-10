package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.craftengineconverter.api.enums.Hand;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class HandCondition extends AbstractCondition {
    private final Hand hand;

    public HandCondition(@NotNull Hand hand) {
        super("hand");
        this.hand = Objects.requireNonNull(hand, "hand cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("hand", this.hand.name().toLowerCase(Locale.ROOT));
    }
}
