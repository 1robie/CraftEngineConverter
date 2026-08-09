package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.template.TemplateEngine;
import fr.robie.craftengineconverter.api.configuration.template.TemplateException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the template merge semantics. Packs are authored against CraftEngine's exact behaviour, so these
 * assert the rules rather than merely that resolution runs.
 */
class TemplateEngineTest {

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            out.put((String) keyValues[i], keyValues[i + 1]);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    // ---- placeholders -------------------------------------------------------

    @Test
    void substitutesPlaceholdersInValuesAndKeys() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("material", "chainmail_${part}", "${part}_key", "v"));

        Map<String, Object> out = asMap(engine.resolve("ns:id",
                map("template", "t", "arguments", map("part", "boots"))));

        assertEquals("chainmail_boots", out.get("material"));
        assertEquals("v", out.get("boots_key"));
    }

    @Test
    void usesFallbackWhenArgumentUnbound() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("a", "${missing:-fallback}", "b", "${missing:-}"));

        Map<String, Object> out = asMap(engine.resolve("ns:id", map("template", "t")));
        assertEquals("fallback", out.get("a"));
        assertEquals("", out.get("b"));
    }

    /**
     * A quoted fallback loses its quotes. Configs write {@code ${fence_type:-"oak"}_fence} meaning the block
     * {@code oak_fence}; inside a YAML plain scalar those quotes are ordinary characters, so keeping them produced
     * {@code "oak"_fence}, an id no block has — which is what made the whole fence resolve to nothing.
     */
    @Test
    void aQuotedFallbackIsUnquoted() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map(
                "double", "${missing:-\"oak\"}_fence",
                "single", "${missing:-'oak'}_fence",
                "bare", "${missing:-oak}_fence",
                "inner", "${missing:-say \"hi\"}"));

        Map<String, Object> out = asMap(engine.resolve("ns:id", map("template", "t")));
        assertEquals("oak_fence", out.get("double"));
        assertEquals("oak_fence", out.get("single"));
        assertEquals("oak_fence", out.get("bare"));
        // Only a matching outer pair goes; quotes inside the text are content.
        assertEquals("say \"hi\"", out.get("inner"));
    }

    @Test
    void unboundPlaceholderDropsItsEntry() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("kept", "yes", "a", "${nope}"));

        // Lenient by necessity: the YAML layer erases an explicit "lore: null", so a deliberately
        // nulled argument is indistinguishable from a missing one. Dropping the entry matches what
        // CraftEngine produces for the nulled case; failing would discard the whole item.
        Map<String, Object> out = asMap(engine.resolve("ns:id", map("template", "t")));
        assertEquals("yes", out.get("kept"));
        assertNull(out.get("a"));
    }

    @Test
    void appliesCasingModifiers() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("cap", "${p^}", "upper", "${p^^}", "plain", "${p}"));

        Map<String, Object> out = asMap(engine.resolve("ns:id",
                map("template", "t", "arguments", map("p", "topaz"))));

        assertEquals("Topaz", out.get("cap"));
        assertEquals("TOPAZ", out.get("upper"));
        assertEquals("topaz", out.get("plain"));
    }

    @Test
    void escapedDollarStaysLiteral() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("a", "\\${not_a_placeholder}", "b", "cost: 5\\$"));

        Map<String, Object> out = asMap(engine.resolve("ns:id", map("template", "t")));
        assertEquals("${not_a_placeholder}", out.get("a"));
        assertEquals("cost: 5$", out.get("b"));
    }

    @Test
    void unclosedPlaceholderIsTreatedAsText() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("a", "${unclosed"));
        // A malformed string should not abort a conversion.
        assertEquals("${unclosed", asMap(engine.resolve("ns:id", map("template", "t"))).get("a"));
    }

    @Test
    void nullArgumentDropsItsEntry() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("kept", "yes", "${gone}", "value", "also_gone", "${gone}"));

        Map<String, Object> args = map("gone", null);
        Map<String, Object> out = asMap(engine.resolve(
                map("template", "t", "arguments", args), Map.of()));

        assertEquals("yes", out.get("kept"));
        assertFalse(out.containsKey("value"), "a null key must drop the entry entirely");
        assertNull(out.get("also_gone"));
    }

    @Test
    void injectsNamespaceAndIdArguments() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("asset", "${__NAMESPACE__}:${__ID__}"));

        assertEquals("default:topaz_boots",
                asMap(engine.resolve("default:topaz_boots", map("template", "t"))).get("asset"));
    }

    // ---- merge semantics ----------------------------------------------------

    @Test
    void multipleTemplatesDeepMergeInOrder() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("a", map("shared", "from_a", "only_a", 1, "nested", map("x", 1, "y", 1)));
        engine.register("b", map("shared", "from_b", "only_b", 2, "nested", map("y", 2, "z", 2)));

        Map<String, Object> out = asMap(engine.resolve("ns:id", map("templates", List.of("a", "b"))));

        assertEquals("from_b", out.get("shared"), "later template wins");
        assertEquals(1, out.get("only_a"));
        assertEquals(2, out.get("only_b"));
        assertEquals(map("x", 1, "y", 2, "z", 2), out.get("nested"), "nested maps merge, not replace");
    }

    @Test
    void singleTemplateKeyAcceptsBothSpellings() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("k", "v"));
        assertEquals("v", asMap(engine.resolve("ns:id", map("template", "t"))).get("k"));
        assertEquals("v", asMap(engine.resolve("ns:id", map("templates", "t"))).get("k"));
    }

    @Test
    void overridesReplaceAndMergesGoDeep() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("nested", map("keep", 1, "replace", 1)));

        Map<String, Object> overridden = asMap(engine.resolve("ns:id",
                map("template", "t", "overrides", map("nested", map("replace", 2)))));
        assertEquals(map("replace", 2), overridden.get("nested"),
                "overrides replaces the whole value at the top level");

        Map<String, Object> merged = asMap(engine.resolve("ns:id",
                map("template", "t", "merges", map("nested", map("replace", 2)))));
        assertEquals(map("keep", 1, "replace", 2), merged.get("nested"),
                "merges recurses into the value");
    }

    @Test
    void strayKeysBesideTemplateAreMerged() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("a", 1));
        // CraftEngine tolerates config mixed in beside the template rather than discarding it.
        Map<String, Object> out = asMap(engine.resolve("ns:id", map("template", "t", "b", 2)));
        assertEquals(1, out.get("a"));
        assertEquals(2, out.get("b"));
    }

    @Test
    void parentArgumentsWinOverNested() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("inner", map("value", "${shared}"));
        engine.register("outer", map("template", "inner", "arguments", map("shared", "inner_default")));

        // The caller's value must survive an inner template re-declaring the same argument.
        Map<String, Object> out = asMap(engine.resolve("ns:id",
                map("template", "outer", "arguments", map("shared", "from_caller"))));
        assertEquals("from_caller", out.get("value"));
    }

    @Test
    void doubleDollarKeyForcesReplacement() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("a", map("nested", map("keep", 1)));
        engine.register("b", map("$$nested", map("only", 2)));

        Map<String, Object> out = asMap(engine.resolve("ns:id", map("templates", List.of("a", "b"))));
        assertEquals(map("only", 2), out.get("nested"), "$$ discards what was inherited");
        assertFalse(out.containsKey("$$nested"));
    }

    @Test
    void listTemplatesConcatenateAndOverrideReplaces() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("a", List.of(1, 2));
        engine.register("b", List.of(3));

        assertEquals(List.of(1, 2, 3), engine.resolve("ns:id", map("templates", List.of("a", "b"))));
        assertEquals(List.of(9), engine.resolve("ns:id",
                map("template", "a", "overrides", List.of(9))));
        assertEquals(List.of(1, 2, 9), engine.resolve("ns:id",
                map("template", "a", "merges", List.of(9))));
    }

    @Test
    void unknownTemplateFails() {
        TemplateEngine engine = new TemplateEngine();
        assertThrows(TemplateException.class, () -> engine.resolve("ns:id", map("template", "nope")));
    }

    @Test
    void conditionArgumentPicksBranch() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("t", map("value", "${branch}"));

        Map<String, Object> condition =
                map("type", "condition", "condition", "true", "on_true", "yes", "on_false", "no");
        assertEquals("yes", asMap(engine.resolve("ns:id",
                map("template", "t", "arguments", map("branch", condition)))).get("value"));

        condition.put("condition", "false");
        assertEquals("no", asMap(engine.resolve("ns:id",
                map("template", "t", "arguments", map("branch", condition)))).get("value"));
    }

    @Test
    void treeWithoutTemplatesStillResolvesPlaceholders() {
        TemplateEngine engine = new TemplateEngine();
        Object out = engine.resolve("default:thing", map("id", "${__ID__}", "nested", map("ns", "${__NAMESPACE__}")));
        assertEquals("thing", asMap(out).get("id"));
        assertEquals("default", asMap(asMap(out).get("nested")).get("ns"));
        assertTrue(engine.isEmpty());
    }
}
