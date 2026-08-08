package fr.robie.craftengineconverter.api.configuration.bedrock.text;

import com.google.gson.JsonElement;

/**
 * What a custom item is called, and everything that follows from it.
 * <p>
 * A name reaches Bedrock by two routes at once and they answer different questions:
 * <ul>
 *   <li>{@code display_name} in the Geyser mapping — carries styling, and is what Geyser sends.</li>
 *   <li>{@code item.<identifier>.name} in the pack's {@code texts/*.lang} — the only route that localises, because
 *       the client resolves it in whatever language it is running.</li>
 * </ul>
 * The second is only possible when the name is a bare translation key, so {@link #translationKey()} exists to say
 * whether it is. A styled or literal name has none, and relies on {@code display_name} alone.
 * <p>
 * A type rather than a {@code String} because the string form is what went wrong: a field that was sometimes text
 * and sometimes a key, with no way to tell, so a key got written where text was expected and items were called
 * {@code item.default.flame_cane} in game.
 */
public final class ItemName {

    private final TextComponent component;
    private final String translationKey;

    private ItemName(TextComponent component, String translationKey) {
        this.component = component;
        this.translationKey = translationKey;
    }

    /** Plain text, exactly as written. */
    public static ItemName literal(String text) {
        return new ItemName(TextComponent.literal(text), null);
    }

    /** A bare translation key, which can also be localised through the pack's lang files. */
    public static ItemName translatable(String key) {
        return new ItemName(TextComponent.translatable(key), key);
    }

    /**
     * An arbitrary component — styled text, a coloured translation, several parts.
     * <p>
     * The translation key is recovered only from a component that is <b>nothing but</b> a {@code translate}. Once a
     * colour or a second part is involved the name cannot be expressed as a single lang entry, so it goes through
     * {@code display_name} alone and loses per-language resolution. That is a real trade, and it is the pack
     * author's to make: they chose the styling.
     */
    public static ItemName of(TextComponent component) {
        if (component == null) return null;
        boolean bareTranslate = component.translate() != null
                && component.text() == null
                && component.extra().isEmpty()
                && component.serialize().isJsonObject()
                && component.serialize().getAsJsonObject().size() == 1;
        return new ItemName(component, bareTranslate ? component.translate() : null);
    }

    /** The value for {@code display_name}: a plain string when it can be, an object when it must be. */
    public JsonElement toDisplayNameJson() {
        return this.component.serialize();
    }

    /** The key to register under {@code item.<identifier>.name}, or {@code null} when there is none. */
    public String translationKey() {
        return this.translationKey;
    }

    public TextComponent component() {
        return this.component;
    }
}
