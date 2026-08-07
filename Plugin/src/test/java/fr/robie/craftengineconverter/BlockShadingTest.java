package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The two things that darken a converted block, both of which were previously forced on.
 *
 * <h2>light_dampening</h2>
 * Every block was shipped fully light-blocking, so a door, a gate and a sapling each shaded whatever they stood
 * against — two doors facing one another darkened each other's inner faces, and a gate cast a shadow onto the side
 * of the solid block beside it, across a gap you could walk through. Only a shape that fills its cube should stop
 * light.
 *
 * <h2>ambient_occlusion</h2>
 * Java's {@code ambientocclusion} says whether a shape should be shaded by its neighbours, and vanilla turns it off
 * for the models that would look wrong with it. It is <b>inherited</b>: a pack's door is a bare {@code parent} plus
 * {@code textures}, and the {@code false} lives one level up in {@code block/door_bottom_left}. The flag was parsed
 * but never read, and never inherited either.
 */
class BlockShadingTest {

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    /** Converts one block and returns its definition, given a model and an optional parent to inherit from. */
    private static JsonObject convert(String model, String parentName, String parentJson) throws Exception {
        Path root = Files.createTempDirectory("shading");
        Path assets = root.resolve("assets");

        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"), model);
        if (parentJson != null) {
            write(assets.resolve("minecraft/models/" + parentName + ".json"), parentJson);
        }

        BlockStateMapper mapper = new BlockStateMapper()
                .withModelResolver(new JavaModelResolver())
                .withTexturePipeline(new TexturePipeline());
        mapper.addFromBlockstatesDirectory(
                assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        Path out = root.resolve("out");
        Files.createDirectories(out);
        mapper.save(out);

        JsonObject blocks = JsonParser.parseString(Files.readString(out.resolve("geyser_block_mappings.json")))
                .getAsJsonObject().getAsJsonObject("blocks");
        return blocks.getAsJsonObject(blocks.keySet().iterator().next());
    }

    /** Absent means zero — the field is only written when it is non-zero. */
    private static int lightDampeningOf(JsonObject definition) {
        return definition.has("light_dampening") ? definition.get("light_dampening").getAsInt() : 0;
    }

    private static final String FULL_CUBE =
            "{\"textures\":{\"all\":\"block/custom/thing\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],"
                    + "\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}";

    /** A door-thin panel: you can see past it, so it must not stop light. */
    private static final String THIN_PANEL =
            "{\"textures\":{\"all\":\"block/custom/thing\"},\"elements\":[{\"from\":[0,0,0],\"to\":[3,16,16],"
                    + "\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}";

    @Test
    void aShapeYouCanSeePastDoesNotBlockLight() throws Exception {
        assertEquals(0, lightDampeningOf(convert(THIN_PANEL, null, null)),
                "a door or gate that dampened light cast a shadow through the gap beside it");
    }

    @Test
    void aFullCubeStillBlocksLight() throws Exception {
        assertEquals(15, lightDampeningOf(convert(FULL_CUBE, null, null)),
                "a solid block must keep stopping light, or caves and interiors light up through it");
    }

    /**
     * The inheritance that was missing. The child says nothing about smooth lighting; its parent says no.
     */
    @Test
    void ambientOcclusionIsInheritedFromTheParentModel() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/door_like\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                "block/door_like",
                "{\"ambientocclusion\":false,\"elements\":[{\"from\":[0,0,0],\"to\":[3,16,16],"
                        + "\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");

        String json = definition.toString();
        assertFalse(json.contains("\"ambient_occlusion\":true"),
                "the parent turned smooth lighting off, so two facing doors must not shade each other; got " + json);
    }
}
