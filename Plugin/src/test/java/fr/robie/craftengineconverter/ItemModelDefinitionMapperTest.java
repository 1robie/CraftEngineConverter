package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.item.models.condition.ConditionModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.select.SelectModelConfiguration;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.converter.bedrock.item.ItemModelDefinitionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parses the real item model definitions in the test pack, to prove the JSON adapter plus the existing
 * loader registry actually cover Java's format rather than just compiling.
 */
class ItemModelDefinitionMapperTest {

    private static ItemModelDefinitionMapper mapper;

    @BeforeAll
    static void setup() throws Exception {
        ClassLoader classLoader = CraftEngineConverterPlugin.class.getClassLoader();
        new RegistryHelper(classLoader).loadRegistries();

        var resource = classLoader.getResource(
                "bedrock-folder/bedrock/pack/resource_pack_unprotected/assets");
        assert resource != null;
        Path assets = new File(resource.toURI()).toPath();

        mapper = new ItemModelDefinitionMapper();
        for (String namespace : new String[]{"default", "internal", "minecraft"}) {
            File items = assets.resolve(namespace).resolve("items").toFile();
            if (items.isDirectory()) {
                mapper.addFromItemsDirectory(items, namespace, assets);
            }
        }
    }

    @Test
    void parsesTheWholeCorpus() {
        assertFalse(mapper.isEmpty());
        // 94 definition files live in the test pack across the three namespaces.
        assertEquals(94, mapper.size());
    }

    @Test
    void plainModelDefinitionResolvesIncludingItsPath() {
        var def = mapper.get("default:amethyst_standing_torch").orElseThrow();
        var model = assertInstanceOf(SimpleModelConfiguration.class, def.model());
        // Java spells the leaf key "model"; CraftEngine spells it "path". Both must land here.
        // SimpleModelConfiguration normalises an unqualified path onto the minecraft namespace.
        assertEquals("minecraft:block/custom/amethyst_torch", model.getModel());
    }

    @Test
    void conditionDefinitionResolvesWithBothBranches() {
        // drill.json: condition on "selected" with on_true / on_false branches.
        var def = mapper.get("default:drill").orElseThrow();
        var condition = assertInstanceOf(ConditionModelConfiguration.class, def.model());
        // Leaves matter: a tree whose branches are all null is worthless downstream.
        assertInstanceOf(SimpleModelConfiguration.class, condition.getOnTrue());
        assertInstanceOf(SimpleModelConfiguration.class, condition.getOnFalse());
    }

    @Test
    void chargeTypeSelectResolvesWithCaseModels() {
        // topaz_crossbow.json — charge_type is one of the two predicates Geyser can represent.
        var def = mapper.get("default:topaz_crossbow").orElseThrow();
        assertInstanceOf(SelectModelConfiguration.class, def.model());
        SelectModelConfiguration<?> select = (SelectModelConfiguration<?>) def.model();
        assertFalse(select.getCases().isEmpty());
        for (SelectModelConfiguration.Case caseEntry : select.getCases()) {
            assertInstanceOf(SimpleModelConfiguration.class, caseEntry.model(),
                    "every case must resolve its leaf model");
        }
    }

    @Test
    void trimMaterialSelectResolvesEveryCase() {
        // topaz_boots.json selects on trim_material across all 11 vanilla trim materials.
        var def = mapper.get("default:topaz_boots").orElseThrow();
        assertInstanceOf(SelectModelConfiguration.class, def.model());
        SelectModelConfiguration<?> select = (SelectModelConfiguration<?>) def.model();

        var materials = select.getCases().stream()
                .map(c -> c.when().toString())
                .toList();
        assertEquals(List.of(
                "minecraft:gold", "minecraft:emerald", "minecraft:quartz", "minecraft:redstone",
                "minecraft:lapis", "minecraft:diamond", "minecraft:amethyst", "minecraft:iron",
                "minecraft:copper", "minecraft:netherite", "minecraft:resin"), materials);
        // Trim values are resource locations, so they must survive verbatim for Geyser to match them.
        for (SelectModelConfiguration.Case caseEntry : select.getCases()) {
            assertInstanceOf(SimpleModelConfiguration.class, caseEntry.model());
        }
    }

    @Test
    void oversizedInGuiFlagIsRead() {
        assertTrue(mapper.get("default:topaz_bow").orElseThrow().oversizedInGui());
    }

    @Test
    void everyDefinitionIsIdentifiedByNamespaceAndPath() {
        assertTrue(mapper.get("default:topaz_bow").isPresent());
        assertTrue(mapper.get("default:nonexistent_item").isEmpty());
    }
}
