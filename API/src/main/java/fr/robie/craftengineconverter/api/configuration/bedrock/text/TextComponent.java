package fr.robie.craftengineconverter.api.configuration.bedrock.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * A Minecraft <b>Java</b> text component, which is what Geyser's {@code display_name} accepts.
 * <p>
 * Java rather than Bedrock on purpose. Geyser's mapping file is read on the Java side, and Geyser converts what it
 * finds into Bedrock rawtext itself when it sends the item. Bedrock's own rawtext — {@code {"rawtext": [...]}} with
 * {@code %%s} substitutions — is the format for commands and UI, and there is nowhere in an item mapping or a
 * resource pack to put it: {@code texts/*.lang} holds plain {@code key=value} strings, not JSON.
 * <p>
 * The point of modelling this at all is that a name is not just a string. CraftEngine writes
 * {@code <!i><red><bold>Flame Cane</bold></red>}, and the converter used to delete every tag with a regex, so every
 * custom item arrived in Bedrock as plain white text. A component keeps the colour.
 * <p>
 * <b>Deliberately missing:</b> {@code selector}, {@code score}, {@code keybind} and {@code nbt}. Each resolves per
 * player at the moment a message is sent, and an item's default name is registered once when Geyser starts, so they
 * would render empty. Refusing them is better than emitting something that silently shows nothing.
 */
public final class TextComponent {

    private String text;
    private String translate;
    private final List<TextComponent> with = new ArrayList<>();
    private final List<TextComponent> extra = new ArrayList<>();

    private String color;
    private String font;
    private Boolean bold;
    private Boolean italic;
    private Boolean underlined;
    private Boolean strikethrough;
    private Boolean obfuscated;

    private TextComponent() {}

    /** Literal text, with no styling of its own. */
    public static TextComponent literal(String text) {
        TextComponent component = new TextComponent();
        component.text = text;
        return component;
    }

    /** A translation key, resolved by the client against its language files. */
    public static TextComponent translatable(String key) {
        TextComponent component = new TextComponent();
        component.translate = key;
        return component;
    }

    public String text() { return this.text; }

    public String translate() { return this.translate; }

    public List<TextComponent> extra() { return this.extra; }

    public TextComponent withColor(String color) { this.color = color; return this; }
    public TextComponent withFont(String font) { this.font = font; return this; }
    public TextComponent withBold(Boolean bold) { this.bold = bold; return this; }
    public TextComponent withItalic(Boolean italic) { this.italic = italic; return this; }
    public TextComponent withUnderlined(Boolean underlined) { this.underlined = underlined; return this; }
    public TextComponent withStrikethrough(Boolean value) { this.strikethrough = value; return this; }
    public TextComponent withObfuscated(Boolean value) { this.obfuscated = value; return this; }

    /** A translation argument, filling one {@code %s} of the key. */
    public TextComponent addWith(TextComponent argument) { this.with.add(argument); return this; }

    /** A child, which inherits this component's styling unless it overrides it. */
    public TextComponent addExtra(TextComponent child) { this.extra.add(child); return this; }

    /**
     * Whether this is bare text carrying nothing else, in which case it serialises as a plain string.
     * <p>
     * Worth the check: the overwhelming majority of names are unstyled, and emitting {@code {"text": "Ruby"}} for
     * each would bloat the mapping file and make it harder to read against the previous one.
     */
    public boolean isPlainText() {
        return this.text != null && this.translate == null
                && this.extra.isEmpty() && this.with.isEmpty()
                && this.color == null && this.font == null
                && this.bold == null && this.italic == null && this.underlined == null
                && this.strikethrough == null && this.obfuscated == null;
    }

    public JsonElement serialize() {
        if (this.isPlainText()) return new JsonPrimitive(this.text);

        JsonObject json = new JsonObject();
        if (this.text != null) json.addProperty("text", this.text);
        if (this.translate != null) json.addProperty("translate", this.translate);

        if (!this.with.isEmpty()) {
            JsonArray array = new JsonArray();
            for (TextComponent argument : this.with) array.add(argument.serialize());
            json.add("with", array);
        }

        if (this.color != null) json.addProperty("color", this.color);
        if (this.font != null) json.addProperty("font", this.font);
        addFlag(json, "bold", this.bold);
        addFlag(json, "italic", this.italic);
        addFlag(json, "underlined", this.underlined);
        addFlag(json, "strikethrough", this.strikethrough);
        addFlag(json, "obfuscated", this.obfuscated);

        if (!this.extra.isEmpty()) {
            JsonArray array = new JsonArray();
            for (TextComponent child : this.extra) array.add(child.serialize());
            json.add("extra", array);
        }

        return json;
    }

    // Written only when set, so "not stated" stays distinct from "stated false" - the difference matters for
    // italic, where Java defaults a custom item name to italic and false has to be said out loud.
    private static void addFlag(JsonObject json, String name, Boolean value) {
        if (value != null) json.addProperty(name, value);
    }
}
