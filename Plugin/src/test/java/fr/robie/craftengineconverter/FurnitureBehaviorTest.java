package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.CraftEngineItemsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.FurnitureConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.SimpleStorageFurnitureBehavior;
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
 * The {@code behavior} list a furniture definition carries. CraftEngine reads it as a list of maps keyed by
 * {@code type}, not as a section per behaviour.
 */
class FurnitureBehaviorTest {

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> behaviors(FurnitureConfiguration furniture) {
        CraftEngineItemsConfiguration configuration = new CraftEngineItemsConfiguration("default:chair", Material.PAPER);
        configuration.addItemConfiguration(furniture);

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection itemSection = yaml.createSection("items.default:chair");
        configuration.serialize(yaml, "items.default:chair", itemSection);

        return (List<Map<String, Object>>) itemSection.getList("behavior.furniture.behavior");
    }

    @Test
    void writesStorageAsAnEntryInTheBehaviorList() {
        FurnitureConfiguration furniture = new FurnitureConfiguration();
        furniture.addBehavior(new SimpleStorageFurnitureBehavior(
                3, "Chest", "minecraft:block.chest.open", "minecraft:block.chest.close"));

        List<Map<String, Object>> behaviors = behaviors(furniture);
        assertEquals(1, behaviors.size());

        Map<String, Object> storage = behaviors.getFirst();
        assertEquals("simple_storage_furniture", storage.get("type"));
        assertEquals(3, storage.get("rows"));
        assertEquals("Chest", storage.get("title"));
        assertEquals(Map.of("open", "minecraft:block.chest.open", "close", "minecraft:block.chest.close"),
                storage.get("sounds"));
    }

    @Test
    void leavesOutSoundsThatWereNotConfigured() {
        FurnitureConfiguration furniture = new FurnitureConfiguration();
        furniture.addBehavior(new SimpleStorageFurnitureBehavior(6, null, null, null));

        Map<String, Object> storage = behaviors(furniture).getFirst();
        assertEquals(6, storage.get("rows"));
        assertFalse(storage.containsKey("title"));
        assertFalse(storage.containsKey("sounds"), "an empty sounds map is noise, not configuration");
    }

    @Test
    void writesNoBehaviorKeyWhenThereAreNone() {
        CraftEngineItemsConfiguration configuration = new CraftEngineItemsConfiguration("default:chair", Material.PAPER);
        configuration.addItemConfiguration(new FurnitureConfiguration());

        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection itemSection = yaml.createSection("items.default:chair");
        configuration.serialize(yaml, "items.default:chair", itemSection);

        assertTrue(itemSection.contains("behavior.furniture"), "the furniture itself is still written");
        assertFalse(itemSection.contains("behavior.furniture.behavior"));
    }
}
