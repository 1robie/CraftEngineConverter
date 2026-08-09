package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.common.utils.yaml.directive.PluginKeyDirective;
import fr.robie.craftengineconverter.common.utils.yaml.directive.VersionKeyDirective;
import fr.robie.craftengineconverter.converter.bedrock.item.EquipmentAssetRegistry;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.directive.KeyDirectiveRegistry;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the real {@code equipments:} block from the test pack.
 * <p>
 * That block is wrapped in {@code $$>=1.21.2} / {@code $$<1.21.2} variants, so this also confirms the
 * version directive leaves the asset reachable — if the gate swallowed it, armour would silently render
 * untextured.
 */
class EquipmentAssetRegistryTest {

    @BeforeAll
    static void setup() {
        // The converter registers these at plugin startup; a bare test JVM has to do it too, or the
        // $$-prefixed keys are never resolved.
        KeyDirectiveRegistry.register(new VersionKeyDirective());
        KeyDirectiveRegistry.register(new PluginKeyDirective());
    }

    @Test
    void readsTheArmourAssetThroughItsVersionGate() throws Exception {
        URL resource = this.getClass().getClassLoader().getResource(
                "bedrock-folder/bedrock/items/default_assets/configuration/items/topaz_armor.yml");
        assertNotNull(resource);

        // Load exactly as the converter does, so this measures the production path.
        YamlConfiguration yaml = fr.robie.craftengineconverter.api.manager.FileCacheManager.getYamlCache()
                .getEntryFile(new File(resource.toURI()).toPath()).orElseThrow().getData();
        ConfigurationSection equipments = yaml.getConfigurationSection("equipments");
        assertNotNull(equipments, "the $$-gated equipments block must survive YAML loading");

        // The version gate is NOT applied by the YAML layer today: the key survives literally and, because
        // '.' is the path separator, "$$>=1.21.2" is split into nested sections. Asserted so that if the
        // YAML layer starts resolving it, this test says so rather than the registry quietly changing shape.
        assertEquals(java.util.Set.of("$$>=1", "$$<1"), equipments.getKeys(false),
                "unresolved version gates split on '.'; the registry must tolerate both shapes");
        EquipmentAssetRegistry registry = new EquipmentAssetRegistry();
        registry.addFromEquipmentsSection(equipments, null);

        assertEquals(1, registry.size());
        var asset = registry.get("default:topaz").orElseThrow();

        // humanoid covers helmet/chestplate/boots (layer 1); leggings get their own texture (layer 2).
        assertEquals("minecraft:entity/equipment/humanoid/topaz", asset.textureFor("feet"));
        assertEquals("minecraft:entity/equipment/humanoid/topaz", asset.textureFor("head"));
        assertEquals("minecraft:entity/equipment/humanoid_leggings/topaz", asset.textureFor("legs"));
        assertEquals(1, asset.layerFor("feet"));
        assertEquals(2, asset.layerFor("legs"));
    }

    @Test
    void armourItemNamesItsAssetAfterTemplateResolution() throws Exception {
        URL resource = this.getClass().getClassLoader().getResource(
                "bedrock-folder/bedrock/items/default_assets/configuration/items/topaz_armor.yml");
        assertNotNull(resource);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(resource.toURI()));

        // The asset id only exists as "${__NAMESPACE__}:${equipment_material}" inside the template, so this
        // is really checking that the placeholder is what detectArmorItem will read.
        ConfigurationSection template = yaml.getConfigurationSection("templates.default:armor/topaz");
        assertNotNull(template, "the armour template must be reachable");
        assertTrue(template.getString("settings.equipment.asset_id", "").contains("${"),
                "asset_id is templated, so it must go through the engine before being read");
    }
}
