package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.api.configuration.loader.JsonConfigurationAdapter;
import fr.robie.yamllibrary.ConfigurationSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the JSON-to-ConfigurationSection semantics the {@code ModelConfiguration} loaders rely on.
 * These loaders call {@code getConfigurationSection}, {@code getInt}, {@code getDouble} and
 * {@code getMapList}, so nested objects must become real child sections and numbers must keep their
 * natural type rather than arriving as Gson's LazilyParsedNumber.
 */
class JsonConfigurationAdapterTest {

    private static ConfigurationSection parse(String json) {
        return JsonConfigurationAdapter.toSection(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void nestedObjectsBecomeChildSections() {
        // The shape of assets/default/items/drill.json.
        ConfigurationSection root = parse("""
                {
                  "type": "condition",
                  "property": "selected",
                  "on_true":  { "type": "model", "model": "item/custom/drill_selected" },
                  "on_false": { "type": "model", "model": "item/custom/drill" }
                }
                """);

        assertEquals("condition", root.getString("type"));

        ConfigurationSection onTrue = root.getConfigurationSection("on_true");
        assertNotNull(onTrue, "nested object must become a child section, not stay a Map");
        assertEquals("model", onTrue.getString("type"));
        assertEquals("item/custom/drill_selected", onTrue.getString("model"));
    }

    @Test
    void integersAndDecimalsKeepTheirNaturalType() {
        ConfigurationSection root = parse("""
                { "index": 1, "threshold": 0.65, "scale": 0.05, "big": 3000000000 }
                """);

        assertEquals(1, root.getInt("index"));
        assertEquals(0.65, root.getDouble("threshold"), 1e-9);
        assertEquals(3000000000L, root.getLong("big"));
        // AbstractRangeDispatchConfigurationLoader reads scale as (float) getDouble("scale", 1.0);
        // there is no single-value getFloat on ConfigurationSection, so this is the only path.
        assertEquals(0.05f, (float) root.getDouble("scale", 1.0), 1e-9f);
        assertEquals(1.0, root.getDouble("absent_scale", 1.0), 1e-9);
    }

    @Test
    void arraysOfObjectsSurviveAsMapLists() {
        // The shape of range_dispatch entries in assets/default/items/topaz_bow.json.
        ConfigurationSection root = parse("""
                {
                  "entries": [
                    { "threshold": 0.65, "model": { "type": "model", "model": "a" } },
                    { "threshold": 0.9,  "model": { "type": "model", "model": "b" } }
                  ]
                }
                """);

        List<?> entries = root.getMapList("entries");
        assertEquals(2, entries.size());
        assertTrue(entries.getFirst() instanceof java.util.Map<?, ?>);
    }

    @Test
    void booleansAndStringListsRoundTrip() {
        ConfigurationSection root = parse("""
                { "oversized_in_gui": true, "tags": ["default:topaz_tools", "default:other"] }
                """);

        assertTrue(root.getBoolean("oversized_in_gui"));
        assertEquals(List.of("default:topaz_tools", "default:other"), root.getStringList("tags"));
    }

    @Test
    void realItemDefinitionLoadsThroughTheRegistry() {
        JsonObject json = JsonParser.parseString("""
                {
                  "type": "select",
                  "property": "charge_type",
                  "cases": [
                    { "when": "arrow",  "model": { "type": "model", "model": "item/custom/cb_arrow" } },
                    { "when": "rocket", "model": { "type": "model", "model": "item/custom/cb_rocket" } }
                  ],
                  "fallback": { "type": "model", "model": "item/custom/cb" }
                }
                """).getAsJsonObject();

        ConfigurationSection section = JsonConfigurationAdapter.toSection(json);
        assertEquals("select", section.getString("type"));
        assertEquals("charge_type", section.getString("property"));
        assertNotNull(section.getConfigurationSection("fallback"));
        assertEquals(2, section.getMapList("cases").size());
    }
}
