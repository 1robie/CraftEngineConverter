package fr.robie.craftengineconverter.common.utils.yaml;

import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import fr.robie.craftengineconverter.common.utils.yaml.file.YamlConfiguration;
import fr.robie.craftengineconverter.common.utils.yaml.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlConfigurationTest {

    @Nested
    @DisplayName("Index-based Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should retrieve basic types via index path")
        void testBasicIndexRetrieval() throws Exception {
            String yaml = """
                    items:
                      - name: "Item 0"
                        count: 64
                        active: true
                        price: 10.5
                      - name: "Item 1"
                        count: 1
                        active: false
                        price: 5.0
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("Item 0", config.getString("items.0.name"));
            assertEquals(64, config.getInt("items.0.count"));
            assertTrue(config.getBoolean("items.0.active"));
            assertEquals(10.5, config.getDouble("items.0.price"));

            assertEquals("Item 1", config.getString("items.1.name"));
            assertEquals(1, config.getInt("items.1.count"));
            assertFalse(config.getBoolean("items.1.active"));
            assertEquals(5.0, config.getDouble("items.1.price"));
        }

        @Test
        @DisplayName("Should handle all primitive list types in sections within lists")
        void testAllListTypes() throws Exception {
            String yaml = """
                    data:
                      - strings: [a, b, c]
                        ints: [1, 2, 3]
                        booleans: [true, false]
                        doubles: [1.1, 2.2]
                        longs: [100, 200]
                        bytes: [1, 2]
                        chars: [x, y]
                        shorts: [10, 20]
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals(List.of("a", "b", "c"), config.getStringList("data.0.strings"));
            assertEquals(List.of(1, 2, 3), config.getIntegerList("data.0.ints"));
            assertEquals(List.of(true, false), config.getBooleanList("data.0.booleans"));
            assertEquals(List.of(1.1, 2.2), config.getDoubleList("data.0.doubles"));
            assertEquals(List.of(100L, 200L), config.getLongList("data.0.longs"));
            assertEquals(List.of((byte) 1, (byte) 2), config.getByteList("data.0.bytes"));
            assertEquals(List.of('x', 'y'), config.getCharacterList("data.0.chars"));
            assertEquals(List.of((short) 10, (short) 20), config.getShortList("data.0.shorts"));
        }

        @Test
        @DisplayName("Should handle invalid indices gracefully")
        void testInvalidIndices() throws Exception {
            String yaml = """
                    list: [a, b]
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertNull(config.get("list.2"));   // Out of bounds
            assertNull(config.get("list.-1"));  // Negative
            assertNull(config.get("list.abc")); // Not a number
        }

        @Test
        @DisplayName("Should support nested lists and sections")
        void testDeepNesting() throws Exception {
            String yaml = """
                    root:
                      - nested_list:
                          - - val: "deep"
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertEquals("deep", config.getString("root.0.nested_list.0.0.val"));
            assertTrue(config.isConfigurationSection("root.0.nested_list.0.0"));
        }
    }

    @Nested
    @DisplayName("Existence and Type Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should correctly identify sections and presence via index")
        void testExistenceChecks() throws Exception {
            String yaml = """
                    list:
                      - key: value
                    """;
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            assertTrue(config.contains("list.0"));
            assertTrue(config.contains("list.0.key"));
            assertFalse(config.contains("list.1"));
            assertFalse(config.contains("list.0.missing"));

            assertTrue(config.isConfigurationSection("list.0"));
            assertFalse(config.isConfigurationSection("list.0.key"));
            assertTrue(config.isString("list.0.key"));
        }
    }

    @Nested
    @DisplayName("Manual Modification and Ambiguity Tests")
    class ModificationTests {

        @Test
        @DisplayName("Should create section with numeric name if parent is not a list")
        void testNumericKeyAsSection() {
            YamlConfiguration config = new YamlConfiguration();
            config.createSection("a.99").set("val", "foo");

            assertFalse(config.isList("a"), "Parent should be a section, not a list");
            assertTrue(config.isConfigurationSection("a.99"), "Should be a section named 99");
            assertEquals("foo", config.getString("a.99.val"));
        }

        @Test
        @DisplayName("Should treat numeric key as index if parent is already a list")
        void testNumericKeyAsIndex() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("list", new ArrayList<>());

            config.createSection("list.0").set("name", "first");
            config.set("list.2.name", "third"); // Should pad with null at index 1

            List<?> list = config.getList("list");
            assertEquals(3, list.size());
            assertInstanceOf(ConfigurationSection.class, list.get(0));
            assertNull(list.get(1));
            assertInstanceOf(ConfigurationSection.class, list.get(2));

            assertEquals("first", config.getString("list.0.name"));
            assertEquals("third", config.getString("list.2.name"));
        }

        @Test
        @DisplayName("Should remove elements or values via index path")
        void testRemoval() {
            YamlConfiguration config = new YamlConfiguration();
            config.set("list", new ArrayList<>(List.of("a", "b", "c")));

            config.set("list.1", null); // Should set element at index 1 to null
            List<?> list = config.getList("list");
            assertNull(list.get(1));
        }
    }

    @Nested
    @DisplayName("Serialization and Round-trip Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should preserve list-section structure during save and load")
        void testRoundTrip() throws Exception {
            YamlConfiguration config = new YamlConfiguration();
            config.set("items", new ArrayList<>());
            ConfigurationSection item0 = config.createSection("items.0");
            item0.set("id", "stone");
            item0.set("amount", 64);

            String saved = config.saveToString();

            YamlConfiguration reloaded = new YamlConfiguration();
            reloaded.loadFromString(saved);

            assertTrue(reloaded.isConfigurationSection("items.0"));
            assertEquals("stone", reloaded.getString("items.0.id"));
            assertEquals(64, reloaded.getInt("items.0.amount"));
        }

        @Test
        @DisplayName("Should handle ConfigurationSerializable within lists")
        void testSerializableInList() throws Exception {
            ConfigurationSerialization.registerClass(TestSerializable.class);

            String yaml = """
                    items:
                      - ==: TestSerializable
                        val: "foo"
                    """;

            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);

            List<?> list = config.getList("items");
            assertInstanceOf(TestSerializable.class, list.getFirst());
            assertEquals("foo", ((TestSerializable) list.getFirst()).val);

            ConfigurationSerialization.unregisterClass(TestSerializable.class);
        }
    }

    @SerializableAs("TestSerializable")
    public static class TestSerializable implements ConfigurationSerializable {
        public String val;

        public TestSerializable(Map<String, Object> map) {
            this.val = (String) map.get("val");
        }

        @Override
        public @NotNull Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("val", this.val);
            return map;
        }

    }
}
