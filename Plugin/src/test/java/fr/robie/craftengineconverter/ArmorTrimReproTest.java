package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.converter.bedrock.item.ItemModelDefinitionMapper;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Temporary reproduction probe: why does {@code topaz_armor.yml} contribute nothing to a conversion?
 * <p>
 * {@code BedrockItemLoader.load()} returns {@code null} — silently, no logging — when it can find no model for an
 * item. For an armour piece the model has to come from {@code assets/default/items/topaz_helmet.json}, which is a
 * {@code select} on {@code trim_material}. If that file does not parse, the item vanishes with no diagnostic and
 * its armour attachable, worn texture and 12 trim variants are never emitted.
 */
class ArmorTrimReproTest {

    /** The real dev-pack assets, so this probes the actual file rather than a hand-written approximation. */
    private static Path itemsDir() {
        return Path.of("src/test/resources/bedrock-folder/bedrock/pack/resource_pack_unprotected/assets/default/items");
    }

    @Test
    void theTrimSelectItemDefinitionParses() throws Exception {
        // Without this the loader registry is empty and every model parses to null — the exact state DevConvert
        // runs in, and the reason a conversion outside the plugin drops armour silently.
        new RegistryHelper(CraftEngineConverterPlugin.class.getClassLoader()).loadRegistries();

        Path items = itemsDir();
        assertTrue(Files.isDirectory(items), "dev pack items dir must exist at " + items.toAbsolutePath());

        Path helmet = items.resolve("topaz_helmet.json");
        assertTrue(Files.isRegularFile(helmet), "topaz_helmet.json must exist");
        System.out.println("[repro] topaz_helmet.json first 200 chars: "
                + Files.readString(helmet).substring(0, 200));

        ItemModelDefinitionMapper mapper = new ItemModelDefinitionMapper();
        mapper.addFromItemsDirectory(new File(items.toString()), "default", items.getParent());

        // The control: a plain "type": "model" item that we know converts fine.
        Optional<ItemModelDefinitionMapper.ItemDefinition> sword = mapper.get("default:topaz_sword");
        System.out.println("[repro] default:topaz_sword present=" + sword.isPresent()
                + " model=" + sword.map(d -> String.valueOf(d.model())).orElse("-"));

        // The subject: the trim select.
        Optional<ItemModelDefinitionMapper.ItemDefinition> helmetDef = mapper.get("default:topaz_helmet");
        System.out.println("[repro] default:topaz_helmet present=" + helmetDef.isPresent()
                + " model=" + helmetDef.map(d -> String.valueOf(d.model())).orElse("-"));

        assertTrue(helmetDef.isPresent(),
                "the helmet definition must be registered, else BedrockItemLoader.load() returns null and the "
                        + "whole armour item is dropped");
        ModelConfiguration model = helmetDef.get().model();
        assertNotNull(model,
                "the trim select must parse to a model; a null model is what makes the armour item vanish");
    }
}
