package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.template.TemplateEngine;
import fr.robie.craftengineconverter.converter.bedrock.item.ConfigFactoryExpander;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@code config_factory} expansion against the shape CraftEngine's own {@code tree.yml} uses.
 * <p>
 * That file declares one factory with {@code tree_type: palm} and a blueprint that generates roughly twenty
 * ids — {@code default:palm_log}, {@code default:palm_planks} and so on. None is declared literally, which
 * is why a converter reading only literal {@code items:} sections finds no palm items at all even though the
 * resource pack ships all their models and textures.
 */
class ConfigFactoryExpanderTest {

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) out.put((String) keyValues[i], keyValues[i + 1]);
        return out;
    }

    private static ConfigFactoryExpander expander() {
        return new ConfigFactoryExpander(new TemplateEngine());
    }

    @Test
    void recognisesEverySpellingAndIgnoresTheLabel() {
        for (String key : new String[]{"config_factory", "config-factory", "config_factories",
                "config-factories", "config_factory#basic", "config-factory#extra"}) {
            assertTrue(ConfigFactoryExpander.isFactoryKey(key), key + " must be recognised");
        }
        assertFalse(ConfigFactoryExpander.isFactoryKey("items"));
        // The label after '#' is only there so one file can hold several sections of a type.
        assertEquals("config_factory", ConfigFactoryExpander.sectionType("config_factory#basic"));
        assertEquals("items", ConfigFactoryExpander.sectionType("items"));
    }

    @Test
    void expandsOneInstanceIntoTheWholeFamily() {
        // Trimmed to the essentials of tree.yml's factory.
        Map<String, Object> file = map("config_factory#basic", map(
                "instances", List.of(map(
                        "namespace", "default",
                        "tree_type", "palm",
                        "asset_path_prefix", "minecraft:block/custom/")),
                "blueprint", map("items", map(
                        "${namespace}:${tree_type}_log", map(
                                "model", "${asset_path_prefix}${tree_type}_log",
                                "settings", map("fuel_time", "${wood_fuel_time:-300}")),
                        "${namespace}:stripped_${tree_type}_log", map(
                                "model", "${asset_path_prefix}stripped_${tree_type}_log"),
                        "${namespace}:${tree_type}_planks", map(
                                "model", "${asset_path_prefix}${tree_type}_planks")))));

        List<Map<String, Object>> items = expander().expand(file).get("items");
        assertEquals(1, items.size(), "one instance yields one items section");

        Map<String, Object> generated = items.getFirst();
        assertEquals(List.of("default:palm_log", "default:stripped_palm_log", "default:palm_planks"),
                List.copyOf(generated.keySet()));

        @SuppressWarnings("unchecked")
        Map<String, Object> log = (Map<String, Object>) generated.get("default:palm_log");
        assertEquals("minecraft:block/custom/palm_log", log.get("model"));
        // A fallback inside a blueprint must resolve too — tree.yml relies on that heavily.
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) log.get("settings");
        assertEquals("300", String.valueOf(settings.get("fuel_time")));
    }

    @Test
    void eachInstanceProducesItsOwnSection() {
        Map<String, Object> file = map("config_factory", map(
                "instances", List.of(
                        map("namespace", "default", "tree_type", "palm"),
                        map("namespace", "default", "tree_type", "willow")),
                "blueprint", map("items", map("${namespace}:${tree_type}_log", map("a", 1)))));

        List<Map<String, Object>> items = expander().expand(file).get("items");
        assertEquals(2, items.size());
        assertTrue(items.get(0).containsKey("default:palm_log"));
        assertTrue(items.get(1).containsKey("default:willow_log"));
    }

    @Test
    void singleInstanceWithoutAListIsAccepted() {
        // CraftEngine's "instance" alias allows one map rather than a list.
        Map<String, Object> file = map("config_factory", map(
                "instance", map("tree_type", "palm"),
                "blueprint", map("items", map("default:${tree_type}_log", map("a", 1)))));
        assertTrue(expander().expand(file).get("items").getFirst().containsKey("default:palm_log"));
    }

    @Test
    void factoryInsideAVersionGateIsStillFound() {
        // tree.yml puts its slab/stairs factory under "$$>=1.20.3", declaring its own full instances rather
        // than inheriting anything. The YAML layer leaves that gate unresolved and '.' splits the key, so the
        // factory arrives buried under $$>=1 -> 20 -> 3 — exactly the shape reproduced here.
        Map<String, Object> factory = map(
                "instances", List.of(map("namespace", "default", "tree_type", "palm")),
                "blueprint", map("items", map("${namespace}:${tree_type}_slab", map("a", 1))));

        Map<String, Object> file = map("$$>=1", map("20", map("3", map("config_factory#extra", factory))));

        List<Map<String, Object>> items = expander().expand(file).get("items");
        assertEquals(1, items.size(), "a gated factory must not be missed");
        assertTrue(items.getFirst().containsKey("default:palm_slab"));
    }

    @Test
    void ordinaryConfigIsNotSearchedForFactories() {
        // Only version-gate fragments are descended into; a map that merely contains a factory-shaped value
        // deeper down must be left alone, or normal config could be expanded by accident.
        Map<String, Object> file = map("items", map("default:thing",
                map("config_factory", map(
                        "instances", List.of(map("t", "x")),
                        "blueprint", map("items", map("default:${t}", map("a", 1)))))));
        assertTrue(expander().expand(file).isEmpty());
    }

    @Test
    void severalFactoriesInOneFileAllExpand() {
        Map<String, Object> file = map(
                "config_factory#a", map(
                        "instances", List.of(map("t", "palm")),
                        "blueprint", map("items", map("default:${t}_log", map("a", 1)))),
                "config_factory#b", map(
                        "instances", List.of(map("t", "willow")),
                        "blueprint", map("items", map("default:${t}_log", map("a", 1)))));

        List<Map<String, Object>> items = expander().expand(file).get("items");
        assertEquals(2, items.size());
    }

    @Test
    void blueprintSectionsOtherThanItemsAreKeptSeparate() {
        Map<String, Object> file = map("config_factory", map(
                "instances", List.of(map("t", "palm")),
                "blueprint", map(
                        "items", map("default:${t}_log", map("a", 1)),
                        "categories", map("default:${t}_tree", map("hidden", true)),
                        "recipes", map("default:${t}_planks", map("type", "shapeless")))));

        var expanded = expander().expand(file);
        assertEquals(List.of("items", "categories", "recipes").size(), expanded.size());
        assertTrue(expanded.get("categories").getFirst().containsKey("default:palm_tree"));
        assertTrue(expanded.get("recipes").getFirst().containsKey("default:palm_planks"));
    }

    @Test
    void malformedFactoryIsSkippedRatherThanFatal() {
        Map<String, Object> noBlueprint = map("config_factory", map("instances", List.of(map("t", "palm"))));
        assertTrue(expander().expand(noBlueprint).isEmpty());

        Map<String, Object> noInstances = map("config_factory", map("blueprint", map("items", map())));
        assertTrue(expander().expand(noInstances).isEmpty());
    }
}
