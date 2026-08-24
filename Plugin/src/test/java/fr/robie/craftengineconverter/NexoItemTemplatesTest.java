package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.nexo.NexoItemTemplates;
import fr.robie.craftengineconverter.utils.ConfigFile;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nexo item templates, checked against how Nexo's own {@code ItemParser} resolves them: templates are looked up
 * globally, merged in declaration order, overridden by the inheriting item, and never emitted as items themselves.
 */
class NexoItemTemplatesTest {

    private static YamlConfiguration yaml(String content) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(content);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return config;
    }

    private static ConfigFile file(String name, String content) {
        return new ConfigFile(new File(name), new File("."), yaml(content));
    }

    private static ConfigurationSection resolve(NexoItemTemplates templates, ConfigFile file, String itemId) {
        return templates.resolve(itemId, file.config().getConfigurationSection(itemId));
    }

    @Test
    void inheritsFromTheTemplateAndLetsTheItemWin() {
        ConfigFile items = file("swords.yml", """
                base_sword:
                  material: IRON_SWORD
                  unbreakable: true
                  Components:
                    max_damage: 500
                    enchantment_glint_override: true
                ruby_sword:
                  template: base_sword
                  Components:
                    max_damage: 900
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        assertTrue(templates.isTemplate("base_sword"), "an item named by a template key is a template");
        assertFalse(templates.isTemplate("ruby_sword"));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals("IRON_SWORD", sword.getString("material"), "inherited from the template");
        assertTrue(sword.getBoolean("unbreakable"));
        assertEquals(900, sword.getInt("Components.max_damage"), "the item overrides the template");
        // Sibling keys inside a nested section survive: the merge is per key, not per section.
        assertTrue(sword.getBoolean("Components.enchantment_glint_override"));
    }

    @Test
    void doesNotTouchTheSourceConfiguration() {
        ConfigFile items = file("swords.yml", """
                base_sword:
                  material: IRON_SWORD
                ruby_sword:
                  template: base_sword
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        resolve(templates, items, "ruby_sword");

        assertNull(items.config().getString("ruby_sword.material"), "resolving must not write back into the file");
    }

    @Test
    void mergesSeveralTemplatesWithTheLastWinning() {
        ConfigFile items = file("swords.yml", """
                glowing:
                  Components:
                    enchantment_glint_override: true
                    max_damage: 100
                sturdy:
                  Components:
                    max_damage: 800
                ruby_sword:
                  template:
                    - glowing
                    - sturdy
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertTrue(sword.getBoolean("Components.enchantment_glint_override"), "kept from the first template");
        assertEquals(800, sword.getInt("Components.max_damage"), "the later template wins");
    }

    @Test
    void followsTemplateChains() {
        ConfigFile items = file("swords.yml", """
                metal:
                  material: IRON_SWORD
                  unbreakable: true
                sturdy:
                  template: metal
                  Components:
                    max_damage: 800
                ruby_sword:
                  template: sturdy
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals("IRON_SWORD", sword.getString("material"), "reached through sturdy's own template");
        assertTrue(sword.getBoolean("unbreakable"));
        assertEquals(800, sword.getInt("Components.max_damage"));
    }

    @Test
    void findsTemplatesDeclaredInAnotherFile() {
        ConfigFile bases = file("bases.yml", """
                base_sword:
                  material: IRON_SWORD
                """);
        ConfigFile swords = file("swords.yml", """
                ruby_sword:
                  template: base_sword
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(bases, swords));

        assertTrue(templates.isTemplate("base_sword"));
        assertEquals("IRON_SWORD", resolve(templates, swords, "ruby_sword").getString("material"));
    }

    @Test
    void acceptsThePluralSpelling() {
        ConfigFile items = file("swords.yml", """
                base_sword:
                  material: IRON_SWORD
                ruby_sword:
                  templates: base_sword
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        assertTrue(templates.isTemplate("base_sword"));
        assertEquals("IRON_SWORD", resolve(templates, items, "ruby_sword").getString("material"));
    }

    @Test
    void expandsIdAndModelPlaceholders() {
        ConfigFile items = file("swords.yml", """
                base_sword:
                  itemname: "<item_id_capitalized>"
                  Pack:
                    generate_model: true
                    parent_model: "<parent>"
                    model: "<model>"
                    texture: "<texture>"
                ruby_sword:
                  template: base_sword
                  Pack:
                    texture: item/ruby_sword
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals("Ruby Sword", sword.getString("itemname"));
        // No Pack.parent_model or Pack.parent on the item, so <parent> falls back to the vanilla generated model.
        assertEquals("minecraft:item/generated", sword.getString("Pack.parent_model"));
        // No Pack.model either, so <model> falls back to the item id.
        assertEquals("ruby_sword", sword.getString("Pack.model"));
        // The item's own texture wins over the template's expanded placeholder anyway, but it must not stay literal.
        assertEquals("item/ruby_sword", sword.getString("Pack.texture"));
    }

    @Test
    void expandsAListPlaceholderIntoSeveralEntries() {
        ConfigFile items = file("swords.yml", """
                base_sword:
                  lore:
                    - "<gray>A blade."
                    - "<lore>"
                    - "<gray>Forged in Nexo."
                ruby_sword:
                  template: base_sword
                  lore:
                    - "<red>Rare"
                    - "<red>Sharp"
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals(
                List.of("<gray>A blade.", "<red>Rare", "<red>Sharp", "<gray>Forged in Nexo."),
                sword.getStringList("lore"),
                "the item's own lore is spliced in where the template asked for it");
        // <lore> consumes the item's own lore, but only on the working copy.
        assertEquals(List.of("<red>Rare", "<red>Sharp"), items.config().getStringList("ruby_sword.lore"));
    }

    @Test
    void leavesAnItemWithoutTemplatesAlone() {
        ConfigFile items = file("swords.yml", """
                ruby_sword:
                  material: IRON_SWORD
                  Components:
                    max_damage: 900
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals("IRON_SWORD", sword.getString("material"));
        assertEquals(900, sword.getInt("Components.max_damage"));
    }

    @Test
    void survivesATemplateCycle() {
        ConfigFile items = file("swords.yml", """
                a:
                  template: b
                  material: IRON_SWORD
                b:
                  template: a
                  unbreakable: true
                ruby_sword:
                  template: a
                """);
        NexoItemTemplates templates = NexoItemTemplates.collect(List.of(items));

        // The point is that this terminates rather than blowing the stack; what it inherits on the way is incidental.
        ConfigurationSection sword = resolve(templates, items, "ruby_sword");
        assertEquals("IRON_SWORD", sword.getString("material"));
        assertTrue(sword.getBoolean("unbreakable"));
    }
}
