package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.api.configuration.template.TemplateEngine;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins down how an explicit {@code key: null} survives YAML loading.
 * <p>
 * It matters because CraftEngine uses {@code lore: null} to bind a template argument to nothing, which
 * makes the template drop that key. If the loader discards the key instead of keeping a null value, the
 * argument looks <i>unbound</i> — and an unbound argument is an error, which would skip the whole item.
 */
class NullArgumentYamlTest {

    private static ConfigurationSection load(String... lines) throws Exception {
        File file = Files.createTempFile("nullarg", ".yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            for (String line : lines) writer.println(line);
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("args");
        file.delete();
        return section;
    }

    @Test
    void explicitNullKeyIsErasedByTheYamlLayer() throws Exception {
        ConfigurationSection args = load("args:", "  name: hello", "  lore: null", "  tilde: ~");

        Map<String, Object> tree = ConfigurationTrees.toMap(args);
        assertTrue(tree.containsKey("name"));
        // Bukkit semantics: assigning null removes the key, so the information that the author wrote
        // "lore: null" is gone before the template engine ever sees it. This is why an unbound argument
        // has to be treated leniently — see ArgumentString.Placeholder.resolve.
        assertFalse(tree.containsKey("lore"), "documents the erasure that forces lenient resolution");
        assertFalse(tree.containsKey("tilde"), "'~' is YAML null and is erased the same way");
    }

    @Test
    void nulledTemplateArgumentStillProducesTheItem() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("icon", Map.of("material", "arrow", "item_name", "${name}", "lore", "${lore}"));

        // The real shape of CraftEngine's internal:exit — "lore: null" plus a template that references it.
        // The item must survive with its material intact; only the lore key goes.
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("template", "icon");
        item.put("arguments", Map.of("name", "Exit"));

        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) engine.resolve("internal:exit", item);
        assertEquals("arrow", out.get("material"));
        assertEquals("Exit", out.get("item_name"));
        // A null *value* keeps its key, as in CraftEngine — only a null *key* drops the entry. Either
        // way the key vanishes when the map becomes a ConfigurationSection, since assigning null there
        // removes it, so nothing downstream sees an empty lore.
        assertNull(out.get("lore"));
        assertFalse(ConfigurationTrees.toSection(out).contains("lore"));
    }
}
