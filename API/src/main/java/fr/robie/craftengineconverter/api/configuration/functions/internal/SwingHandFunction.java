package fr.robie.craftengineconverter.api.configuration.functions.internal;

import fr.robie.craftengineconverter.api.enums.Hand;

import java.util.Locale;
import java.util.Map;

public class SwingHandFunction extends AbstractEventFunction {
    private Hand hand;

    public SwingHandFunction() {
        super("swing_hand");
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        if (this.hand != null) {
            map.put("hand", this.hand.name().toLowerCase(Locale.ROOT));
        }
        return map;
    }
}
