package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.CraftEngineItemsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.components.PotionContentsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.data.TrimConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.data.WrittenBookContentConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two shapes Nexo's {@code trim_material}/{@code trim_pattern} and {@code PotionEffects} convert into.
 *
 * <p>They land in different places on purpose: CraftEngine registers a {@code trim} processor, so a trim sits directly
 * on {@code data}, while nothing handles potion contents and it has to go through {@code data.components} as a raw
 * vanilla component.
 */
class TrimAndPotionContentsTest {

    private static ConfigurationSection serialize(CraftEngineItemsConfiguration configuration) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection itemSection = yaml.createSection("items.default:test_item");
        configuration.serialize(yaml, "items.default:test_item", itemSection);
        return itemSection;
    }

    private static CraftEngineItemsConfiguration configuration() {
        return new CraftEngineItemsConfiguration("default:test_item", Material.PAPER);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> effects(ConfigurationSection item) {
        return (List<Map<String, Object>>) item.getList("data.components.minecraft:potion_contents.custom_effects");
    }

    @Test
    void writesATrimOnDataWhereTheProcessorLooksForIt() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new TrimConfiguration("minecraft:gold", "minecraft:coast"));

        ConfigurationSection item = serialize(configuration);

        assertEquals("minecraft:gold", item.getString("data.trim.material"));
        assertEquals("minecraft:coast", item.getString("data.trim.pattern"));
        assertFalse(item.contains("data.components.minecraft:trim"), "the dedicated processor takes precedence");
    }

    @Test
    void writesPotionEffectsAsARawComponent() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new PotionContentsConfiguration()
                .addEffect(new PotionContentsConfiguration.CustomEffect("minecraft:speed", 200, 1, null, null, null)));

        ConfigurationSection item = serialize(configuration);

        List<Map<String, Object>> effects = effects(item);
        assertEquals(1, effects.size());
        assertEquals("minecraft:speed", effects.getFirst().get("id"));
        assertEquals(200, effects.getFirst().get("duration"));
        assertEquals(1, effects.getFirst().get("amplifier"));
        // Nothing was configured beyond the essentials, so no flags are written and vanilla's defaults stand.
        assertEquals(3, effects.getFirst().size());
    }

    @Test
    void keepsOnlyTheFlagsThatWereActuallySet() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new PotionContentsConfiguration()
                .addEffect(new PotionContentsConfiguration.CustomEffect("minecraft:speed", 200, 0, true, false, null)));

        ConfigurationSection item = serialize(configuration);

        Map<String, Object> effect = effects(item).getFirst();
        assertEquals(Boolean.TRUE, effect.get("ambient"));
        assertEquals(Boolean.FALSE, effect.get("show_particles"));
        assertFalse(effect.containsKey("show_icon"), "an unset flag must not be invented");
    }

    @Test
    void carriesEveryEffectInOrder() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new PotionContentsConfiguration()
                .addEffect(new PotionContentsConfiguration.CustomEffect("minecraft:speed", 200, 0, null, null, null))
                .addEffect(new PotionContentsConfiguration.CustomEffect("minecraft:strength", 400, 2, null, null, null)));

        List<Map<String, Object>> effects = effects(serialize(configuration));
        assertEquals(List.of("minecraft:speed", "minecraft:strength"), effects.stream().map(e -> e.get("id")).toList());
    }

    @Test
    void carriesTheBasePotionAndItsTint() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new PotionContentsConfiguration()
                .setPotion("minecraft:swiftness")
                .setCustomColor(0xFF0000));

        ConfigurationSection item = serialize(configuration);

        assertEquals("minecraft:swiftness", item.getString("data.components.minecraft:potion_contents.potion"));
        assertEquals(0xFF0000, item.getInt("data.components.minecraft:potion_contents.custom_color"));
        // A base potion on its own is a complete component; there is nothing to list under custom_effects.
        assertFalse(item.contains("data.components.minecraft:potion_contents.custom_effects"));
    }

    @Test
    void writesBookContentOnDataWithVanillaGenerationNumbers() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new WrittenBookContentConfiguration(
                "Field Notes", "A. Cartographer",
                WrittenBookContentConfiguration.GENERATION_COPY_OF_ORIGINAL,
                List.of("page one", "page two")));

        ConfigurationSection item = serialize(configuration);

        assertEquals("Field Notes", item.getString("data.written_book_content.title"));
        assertEquals("A. Cartographer", item.getString("data.written_book_content.author"));
        assertEquals(1, item.getInt("data.written_book_content.generation"));
        assertEquals(List.of("page one", "page two"), item.getStringList("data.written_book_content.pages"));
    }

    @Test
    void leavesOutBookFieldsThatWereNotConfigured() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new WrittenBookContentConfiguration(
                null, "  ", WrittenBookContentConfiguration.GENERATION_ORIGINAL, List.of()));

        ConfigurationSection item = serialize(configuration);

        assertFalse(item.contains("data.written_book_content.title"));
        assertFalse(item.contains("data.written_book_content.author"), "a blank author is not an author");
        assertFalse(item.contains("data.written_book_content.generation"), "0 is the processor's own default");
        assertFalse(item.contains("data.written_book_content.pages"));
    }

    @Test
    void writesNothingWhenThereAreNoEffects() {
        CraftEngineItemsConfiguration configuration = configuration();
        PotionContentsConfiguration potionContents = new PotionContentsConfiguration();
        assertFalse(potionContents.hasContents());
        configuration.addItemConfiguration(potionContents);

        ConfigurationSection item = serialize(configuration);
        assertFalse(item.contains("data.components.minecraft:potion_contents"));
        assertTrue(item.contains("material"), "the item itself is still written");
    }
}
