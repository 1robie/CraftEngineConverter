package fr.robie.craftengineconverter.common.utils.yaml;

import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import fr.robie.craftengineconverter.common.utils.yaml.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlListSectionTest {

    @Test
    public void testListSections() {
        String yaml = """
                list:
                  - key: value1
                    nested:
                      foo: bar
                  - key: value2
                """;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            fail("Failed to load YAML: " + e.getMessage());
        }

        List<?> list = config.getList("list");
        assertNotNull(list);
        assertEquals(2, list.size());

        // Verify elements are ConfigurationSections
        assertInstanceOf(ConfigurationSection.class, list.get(0), "First element should be a ConfigurationSection");
        assertInstanceOf(ConfigurationSection.class, list.get(1), "Second element should be a ConfigurationSection");

        ConfigurationSection sec0 = (ConfigurationSection) list.get(0);
        assertEquals("value1", sec0.getString("key"));
        assertEquals("bar", sec0.getString("nested.foo"));

        // Verify index-based path access
        assertEquals("value1", config.getString("list.0.key"));
        assertEquals("bar", config.getString("list.0.nested.foo"));
        assertEquals("value2", config.getString("list.1.key"));

        // Verify getConfigurationSection with index
        ConfigurationSection secIndex0 = config.getConfigurationSection("list.0");
        assertNotNull(secIndex0);
        assertEquals("value1", secIndex0.getString("key"));

        config.getSectionList("list").forEach(section -> {
            assertInstanceOf(ConfigurationSection.class, section, "Each list element should be a ConfigurationSection");
        });

        // Verify contains
        assertTrue(config.contains("list.0.key"));
        assertTrue(config.contains("list.1"));
        assertFalse(config.contains("list.2"));
    }

    @Test
    public void testGetMapList() {
        String yaml = """
                list:
                  - key: value1
                """;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            fail("Failed to load YAML: " + e.getMessage());
        }

        List<Map<?, ?>> mapList = config.getMapList("list");
        assertNotNull(mapList);
        // If my hypothesis is correct, this might fail because elements are ConfigurationSections, not Maps.
        assertEquals(1, mapList.size(), "getMapList should return elements even if they are stored as ConfigurationSections");
        assertEquals("value1", mapList.get(0).get("key"));
    }

    @Test
    public void testNestedLists() {
        String yaml = """
                nested_list:
                  - - item: 0-0
                    - item: 0-1
                  - - item: 1-0
                """;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            fail("Failed to load YAML: " + e.getMessage());
        }

        assertEquals("0-0", config.getString("nested_list.0.0.item"));
        assertEquals("0-1", config.getString("nested_list.0.1.item"));
        assertEquals("1-0", config.getString("nested_list.1.0.item"));
    }
}
