package fr.robie.craftengineconverter.api.configuration.item.models.tints;

import java.util.Map;
import java.util.OptionalInt;

public interface TintConfiguration {
    Map<String, Object> serialize();

    /**
     * The one RGB colour this tint always resolves to, when it has one.
     * <p>
     * Needed by the pre-rendered inventory icon: Bedrock cannot render a model into a slot, so the sprite is
     * drawn at conversion time and the tint has to be multiplied in there — but a sprite is one fixed image,
     * so only a tint that does not depend on runtime state can be honoured. A dye or constant tint qualifies;
     * {@code grass}, {@code team}, {@code potion} and {@code firework} do not, and return empty so the caller
     * can leave the face untinted rather than invent a colour.
     */
    default OptionalInt constantColor() {
        return OptionalInt.empty();
    }
}
