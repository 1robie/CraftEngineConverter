package fr.robie.craftengineconverter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.configuration.bedrock.text.ItemName;
import fr.robie.craftengineconverter.api.configuration.bedrock.text.MiniMessageToComponent;
import fr.robie.craftengineconverter.api.configuration.bedrock.text.TextComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An item's name, from the MiniMessage a pack writes to the {@code display_name} Geyser reads.
 * <p>
 * Two things used to be lost. A name given as {@code <lang:key>} was written as that literal string, so items were
 * called {@code item.default.flame_cane} in game; and every colour was deleted by a regex that stripped all tags,
 * so a red name arrived white.
 */
class TextComponentNameTest {

    private static JsonElement parsed(String miniMessage) {
        TextComponent component = MiniMessageToComponent.parse(miniMessage);
        assertNotNull(component, "should have parsed: " + miniMessage);
        return component.serialize();
    }

    /** Unstyled text stays a plain string, so the mapping file does not fill with {"text": ...} wrappers. */
    @Test
    void plainTextStaysAString() {
        JsonElement json = parsed("Flame Cane");
        assertTrue(json.isJsonPrimitive());
        assertEquals("Flame Cane", json.getAsString());
    }

    @Test
    void aLangTagBecomesATranslateComponent() {
        JsonObject json = parsed("<lang:item.default.flame_cane>").getAsJsonObject();
        assertEquals("item.default.flame_cane", json.get("translate").getAsString());
        assertEquals(1, json.size(), "a bare key should carry nothing else; got " + json);
    }

    /**
     * The whole reason a bare key is kept bare: only that shape can also be written to the pack's lang files, which
     * is the one route that localises per client.
     */
    @Test
    void aBareKeyIsRecoverableForTheLangAlias() {
        assertEquals("item.default.flame_cane",
                ItemName.of(MiniMessageToComponent.parse("<lang:item.default.flame_cane>")).translationKey());
        assertNull(ItemName.literal("Flame Cane").translationKey(),
                "literal text has no key to register");
    }

    /**
     * Java shows a custom item name in italics by default and Bedrock does not, which is why nearly every
     * CraftEngine name opens with {@code <!i>}. It has to survive as an explicit false.
     */
    @Test
    void theItalicOffTagIsCarriedThrough() {
        JsonObject json = parsed("<!i>Flame Cane").getAsJsonObject();
        assertFalse(json.get("italic").getAsBoolean());
        assertEquals("Flame Cane", json.get("text").getAsString());
    }

    @Test
    void colourAndStyleAreKeptRatherThanStripped() {
        JsonObject json = parsed("<red><bold>Flame Cane").getAsJsonObject();
        assertEquals("red", json.get("color").getAsString());
        assertTrue(json.get("bold").getAsBoolean());
    }

    @Test
    void hexColoursAreSupported() {
        assertEquals("#ff8800", parsed("<#ff8800>Ember").getAsJsonObject().get("color").getAsString());
    }

    /** The real shape of a CraftEngine name: italics off, then a colour, then a key. */
    @Test
    void aStyledTranslatableNameKeepsBoth() {
        JsonObject json = parsed("<!i><gold><lang:item.default.flame_cane>").getAsJsonObject();
        assertEquals("item.default.flame_cane", json.get("translate").getAsString());
        assertEquals("gold", json.get("color").getAsString());
        assertFalse(json.get("italic").getAsBoolean());

        // Styled, so it can no longer be one lang entry - display_name carries it alone.
        assertNull(ItemName.of(MiniMessageToComponent.parse("<!i><gold><lang:item.default.flame_cane>"))
                .translationKey());
    }

    @Test
    void severalPartsBecomeExtra() {
        JsonObject json = parsed("<red>Flame<blue>Cane").getAsJsonObject();
        assertEquals(2, json.getAsJsonArray("extra").size());
    }

    /**
     * A tag this cannot read must not be guessed at. Returning null lets the caller fall back to plain text, which
     * costs the formatting and keeps the name — the opposite trade is far worse.
     */
    @Test
    void anUnknownTagIsRefusedRatherThanGuessed() {
        assertNull(MiniMessageToComponent.parse("<gradient:red:blue>Flame Cane"));
        assertNull(MiniMessageToComponent.parse("<rainbow>Flame Cane"));
        assertNull(MiniMessageToComponent.parse("<click:run_command:'/say hi'>Flame Cane"));
    }

    /** A stray closing tag is tolerated: refusing a whole name over one is not worth it. */
    @Test
    void unbalancedClosingTagsDoNotRefuseTheName() {
        assertNotNull(MiniMessageToComponent.parse("Flame Cane</bold>"));
    }

    @Test
    void closingTagEndsOnlyItsOwnStyling() {
        JsonObject json = parsed("<red>Flame</red>Cane").getAsJsonObject();
        assertEquals("red", json.getAsJsonArray("extra").get(0).getAsJsonObject().get("color").getAsString());
        assertTrue(json.getAsJsonArray("extra").get(1).isJsonPrimitive(), "the tail should be unstyled");
    }
}
