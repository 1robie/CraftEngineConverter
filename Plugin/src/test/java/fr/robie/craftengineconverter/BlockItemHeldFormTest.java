package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.ItemMapping;
import fr.robie.craftengineconverter.api.configuration.loader.ConfigurationTrees;
import fr.robie.craftengineconverter.converter.bedrock.BedrockItemLoader;
import fr.robie.craftengineconverter.converter.bedrock.ConversionContext;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.yamllibrary.ConfigurationSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a block item is drawn.
 *
 * <h2>Never with {@code minecraft:block_placer}</h2>
 * That component was used to get the block's 3D look in hand, but it names a block by identifier and a converted
 * CraftEngine block is not a Bedrock block of its own — it is a state override on the vanilla block it replaces, so
 * {@code geyser_block_mappings.json} registers {@code minecraft_oak_leaves} and never anything named after the item.
 * Pointing it at the item's own identifier named a block that does not exist and Bedrock drew <b>nothing at all</b>:
 * measured, 33 of the sample pack's items were invisible in the inventory because of it.
 *
 * <h2>With real geometry only when one texture is involved</h2>
 * An attachable binds one texture per render pass and its geometry's UVs index into that single image, so faces can
 * differ by region but never by texture — per-face {@code material_instances} are a block concept resolved against
 * {@code terrain_texture.json}. One texture costs nothing and the held form matches the placed block; several would
 * put one texture on every face, which is worse than a recognisable sprite, so those keep the 2D icon.
 */
class BlockItemHeldFormTest {

    @BeforeAll
    static void loadRegistries() {
        new RegistryHelper(CraftEngineConverterPlugin.class.getClassLoader()).loadRegistries();
    }

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    /**
     * Converts one {@code block_item} whose model names {@code textureCount} textures.
     *
     * @return the mapping, so the caller can inspect its components
     */
    private static Result convert(int textureCount) throws Exception {
        Path root = Files.createTempDirectory("block-item");
        Path assets = root.resolve("assets");

        StringBuilder textures = new StringBuilder();
        for (int layer = 0; layer < textureCount; layer++) {
            if (layer > 0) textures.append(',');
            // Distinct texture per face key, which is what a multi-textured block model looks like.
            textures.append("\"face").append(layer).append("\":\"block/custom/face").append(layer).append('"');
        }
        write(assets.resolve("default/models/block/custom/thing.json"),
                "{\"textures\":{" + textures + "},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#face0\"}}}]}");
        write(assets.resolve("default/items/thing.json"),
                "{\"model\":{\"type\":\"model\",\"model\":\"default:block/custom/thing\"}}");

        Path out = root.resolve("out");
        Files.createDirectories(out);
        ConversionContext context = new ConversionContext(out, out, out).withJavaAssetsDir(assets);
        context.addItemsDirectory(assets.resolve("default/items").toFile(), "default");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("material", "stone");
        item.put("behavior", Map.of("type", "block_item"));
        ConfigurationSection section = ConfigurationTrees.toSection(item);

        ItemMapping mapping = new BedrockItemLoader("default:thing", section, context).load();
        assertNotNull(mapping, "the item must convert at all");
        return new Result(mapping, context);
    }

    private record Result(ItemMapping mapping, ConversionContext context) {
        boolean hasHeldGeometry() {
            // registerPipelineArtifacts only records geometry when a held model is being emitted.
            return !this.context.collectedGeometry().isEmpty();
        }

        String serialised() {
            return this.mapping.serialize().toString();
        }
    }

    @Test
    void aBlockItemNeverGetsBlockPlacer() throws Exception {
        for (int textureCount : new int[]{1, 6}) {
            String json = convert(textureCount).serialised();
            assertFalse(json.contains("block_placer"),
                    "block_placer names a block that a converted pack never registers, and Bedrock then draws"
                            + " nothing; got " + json);
        }
    }

    @Test
    void aSingleTextureBlockItemGetsRealHeldGeometry() throws Exception {
        assertTrue(convert(1).hasHeldGeometry(),
                "one texture means the attachable's single render pass is enough, so the held form can match the"
                        + " placed block");
    }

    @Test
    void aMultiTextureBlockItemFallsBackToItsIcon() throws Exception {
        assertFalse(convert(6).hasHeldGeometry(),
                "six textures cannot survive one render pass; a cube with the wrong texture on five faces is worse"
                        + " than a sprite, so no held geometry is emitted");
    }
}
