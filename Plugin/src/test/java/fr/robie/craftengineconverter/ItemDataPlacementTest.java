package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.CraftEngineItemsConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.components.FoodConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.components.ItemModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.components.MaxStackSizeConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.data.CustomNameConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.data.UnbreakableConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where converted item configuration has to land for CraftEngine to read it.
 *
 * <p>{@code AbstractItemManager} only feeds an item's {@code data} section to {@code ItemProcessors}, and raw vanilla
 * components live under {@code data.components}. A key written straight onto the item is parsed as YAML perfectly
 * happily and then ignored, which is the kind of mistake nothing downstream complains about - hence this test.
 */
class ItemDataPlacementTest {

    private static ConfigurationSection serialize(CraftEngineItemsConfiguration configuration) {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection itemSection = yaml.createSection("items.default:test_item");
        configuration.serialize(yaml, "items.default:test_item", itemSection);
        return itemSection;
    }

    private static CraftEngineItemsConfiguration configuration() {
        return new CraftEngineItemsConfiguration("default:test_item", Material.PAPER);
    }

    @Test
    void putsVanillaComponentsUnderDataComponents() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new FoodConfiguration(4, 2.5f));
        configuration.addItemConfiguration(new MaxStackSizeConfiguration(16));
        configuration.addItemConfiguration(new ItemModelConfiguration("default:test_item"));

        ConfigurationSection item = serialize(configuration);

        assertTrue(item.isConfigurationSection("data.components"), "components belong under data");
        assertFalse(item.contains("components"), "a top-level components section is never read by CraftEngine");
        assertEquals(16, item.getInt("data.components.minecraft:max_stack_size"));
        assertEquals("default:test_item", item.getString("data.components.minecraft:item_model"));
        assertEquals(4, item.getInt("data.components.minecraft:food.nutrition"));
    }

    @Test
    void keepsDedicatedDataEntriesOnDataItself() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new CustomNameConfiguration("<red>Test"));
        configuration.addItemConfiguration(new UnbreakableConfiguration(true));

        ConfigurationSection item = serialize(configuration);

        // These have their own processors, so they sit directly on data rather than among the raw components.
        assertEquals("<red>Test", item.getString("data.custom-name"));
        assertTrue(item.getBoolean("data.unbreakable"));
    }

    @Test
    void sharesOneDataSectionBetweenBothKinds() {
        CraftEngineItemsConfiguration configuration = configuration();
        configuration.addItemConfiguration(new CustomNameConfiguration("<red>Test"));
        configuration.addItemConfiguration(new FoodConfiguration(4, 2.5f));

        ConfigurationSection item = serialize(configuration);

        // One data section holding both, not two competing ones.
        assertEquals("<red>Test", item.getString("data.custom-name"));
        assertEquals(4, item.getInt("data.components.minecraft:food.nutrition"));
    }
}
