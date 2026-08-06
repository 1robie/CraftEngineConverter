package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bedrock has only two usable built-in block models — a full cube and a crossed plane — so what a block looks like
 * comes down to measuring its Java shape and either matching a built-in or shipping geometry of its own. These pin
 * that decision, using stubbed vanilla models rather than a download.
 */
class BlockStateMapperShapeTest {

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    /**
     * A pack with one blockstate pointing at one model, plus a stub of the vanilla parent it inherits.
     *
     * @return the emitted definition for the single state
     */
    private static JsonObject convert(String modelJson, String vanillaParentPath, String vanillaParentJson)
            throws Exception {
        Path root = Files.createTempDirectory("blockshape");
        Path assets = root.resolve("assets");

        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"), modelJson);

        JavaModelResolver resolver = new JavaModelResolver();
        if (vanillaParentPath != null) {
            write(root.resolve("vanilla/assets/minecraft/models/" + vanillaParentPath + ".json"), vanillaParentJson);
            resolver.withVanillaAssets(new VanillaAssets(root.resolve("cache"), root.resolve("vanilla")));
        }

        BlockStateMapper mapper = new BlockStateMapper().withModelResolver(resolver);
        mapper.addFromBlockstatesDirectory(
                assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        Path out = root.resolve("out");
        Files.createDirectories(out);
        mapper.save(out);

        JsonObject blocks = JsonParser.parseString(Files.readString(out.resolve("geyser_block_mappings.json")))
                .getAsJsonObject().getAsJsonObject("blocks");
        assertEquals(1, blocks.size());
        return blocks.getAsJsonObject(blocks.keySet().iterator().next());
    }

    private static String geometryOf(JsonObject definition) {
        return definition.getAsJsonObject("geometry").get("identifier").getAsString();
    }

    private static JsonObject defaultInstance(JsonObject definition) {
        return definition.getAsJsonObject("material_instances").getAsJsonObject("*");
    }

    /**
     * The mapping key is the Java block being overridden; {@code name} is the <b>Bedrock</b> custom block, which
     * Geyser prefixes with {@code geyser_custom:}. Putting a namespaced identifier in {@code name} produced
     * {@code geyser_custom:minecraft:anvil} - two colons, not a valid Bedrock identifier - and the client refused to
     * register the block, reported every geometry component on it as a missing asset, and crashed while writing the
     * 416th such error.
     */
    @Test
    void theBedrockNameCarriesNoNamespace() throws Exception {
        Path root = Files.createTempDirectory("blockname");
        Path assets = root.resolve("assets");
        write(assets.resolve("minecraft/blockstates/anvil.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"),
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}],"
                        + "\"textures\":{\"all\":\"block/custom/thing\"}}");

        BlockStateMapper mapper = new BlockStateMapper().withModelResolver(new JavaModelResolver());
        mapper.addFromBlockstatesDirectory(
                assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);
        Path out = Files.createDirectories(root.resolve("out"));
        mapper.save(out);

        JsonObject blocks = JsonParser.parseString(Files.readString(out.resolve("geyser_block_mappings.json")))
                .getAsJsonObject().getAsJsonObject("blocks");

        assertTrue(blocks.has("minecraft:anvil"),
                "the key is the Java block Geyser looks the override up by, got " + blocks.keySet());

        String name = blocks.getAsJsonObject("minecraft:anvil").get("name").getAsString();
        assertFalse(name.contains(":"),
                "Geyser prepends geyser_custom:, so a colon here yields an invalid two-colon identifier, got " + name);
        assertEquals("minecraft_anvil", name,
                "the namespace is folded in rather than dropped, so two namespaces stay distinct");
    }

    @Test
    void aCubeParentedBlockUsesTheBuiltInFullBlock() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                "block/cube_all",
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");

        assertEquals("minecraft:geometry.full_block", geometryOf(definition));

        JsonObject instance = defaultInstance(definition);
        assertEquals("opaque", instance.get("render_method").getAsString());
        // Both must be present and true. Geyser defaults them to false, so a full block that omits them renders
        // unshaded and flat beside its vanilla neighbours.
        assertTrue(instance.get("face_dimming").getAsBoolean());
        assertTrue(instance.get("ambient_occlusion").getAsBoolean());
    }

    @Test
    void aPlantUsesTheBuiltInCrossWithShadingOff() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/cross\",\"textures\":{\"cross\":\"block/custom/thing\"}}",
                "block/cross",
                "{\"elements\":["
                        + "{\"from\":[0.8,0,8],\"to\":[15.2,16,8],\"faces\":{"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"}}},"
                        + "{\"from\":[8,0,0.8],\"to\":[8,16,15.2],\"faces\":{"
                        + "\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"}}}]}");

        assertEquals("minecraft:geometry.cross", geometryOf(definition));

        JsonObject instance = defaultInstance(definition);
        // geometry.cross flickers without back-face culling, and shading a flat sheet by its facing darkens one
        // half of the cross. This is the setup vanilla crops use.
        assertEquals("alpha_test_single_sided", instance.get("render_method").getAsString());
        assertFalse(instance.get("face_dimming").getAsBoolean());
        assertFalse(instance.get("ambient_occlusion").getAsBoolean());
    }

    /**
     * Two <b>solid</b> elements is a stairway, not a cross — the distinction the old element-count rule could not
     * make. It has no built-in model, so it must ship geometry.
     */
    @Test
    void aPartialShapeGetsGeneratedGeometry() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/slab\",\"textures\":{\"side\":\"block/custom/thing\"}}",
                "block/slab",
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,8,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#side\"}}}]}");

        String geometry = geometryOf(definition);
        assertTrue(geometry.startsWith("geometry.blocks."), "expected generated geometry, got " + geometry);
        // Colons are invalid in a geometry identifier.
        assertFalse(geometry.contains(":"), geometry);
    }

    @Test
    void generatedGeometryKeepsTheJavaBounds() throws Exception {
        Path root = Files.createTempDirectory("blockshape");
        Path assets = root.resolve("assets");
        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"),
                "{\"textures\":{\"0\":\"block/custom/thing\"},\"elements\":["
                        + "{\"from\":[4,0,5],\"to\":[7,2,8],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#0\"}}}]}");

        BlockStateMapper mapper = new BlockStateMapper().withModelResolver(new JavaModelResolver());
        mapper.addFromBlockstatesDirectory(assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        var generated = mapper.getGeneratedGeometry();
        assertEquals(1, generated.size());

        JsonObject geo = generated.values().iterator().next().serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject bone = geo.getAsJsonArray("bones").get(0).getAsJsonObject();
        // A block bone must not carry the attachable's item-slot binding.
        assertFalse(bone.has("binding"), "block geometry should have no item-slot binding");

        JsonObject cube = bone.getAsJsonArray("cubes").get(0).getAsJsonObject();
        // x and z are centred on the block, y is untouched, and x <b>is</b> mirrored, because Bedrock reads geometry
        // mirrored relative to Java. So from [4,0,5] centres to [-4, 0, -3] and the x then reflects to +1.
        //
        // This used to assert -4, on the reasoning that mirroring "moved a door to the opposite half of its frame
        // and reversed its texture". Those were real symptoms of a mirror that moved cubes but never swapped the
        // east and west faces or flipped their U — a half mirror. Completing it fixed the doors and left the
        // mirror correct; keeping the mirror off instead was what put fence gates, stairs and trapdoors the wrong
        // way round, invisibly, because a closed gate is X-symmetric on all four facings.
        assertEquals(1.0, cube.getAsJsonArray("origin").get(0).getAsDouble(), 0.001);
        assertEquals(0.0, cube.getAsJsonArray("origin").get(1).getAsDouble(), 0.001);
        assertEquals(-3.0, cube.getAsJsonArray("origin").get(2).getAsDouble(), 0.001);
        assertEquals(3.0, cube.getAsJsonArray("size").get(0).getAsDouble(), 0.001);
        assertEquals(2.0, cube.getAsJsonArray("size").get(1).getAsDouble(), 0.001);

        // The face points at a material instance named after the Java texture variable.
        assertEquals("0", cube.getAsJsonObject("uv").getAsJsonObject("up").get("material_instance").getAsString());
    }

    /**
     * Several states of one host block routinely use different models — three pebble models on
     * {@code minecraft:tripwire} — so geometry has to be named after the model. Naming it after the block gave them
     * all one file and left every state wearing the last model's shape.
     */
    @Test
    void eachModelGetsItsOwnGeometryEvenOnOneBlock() throws Exception {
        Path root = Files.createTempDirectory("blockshape");
        Path assets = root.resolve("assets");
        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"age=0\":{\"model\":\"block/custom/one\"},"
                        + "\"age=1\":{\"model\":\"block/custom/two\"}}}");
        for (String name : new String[]{"one", "two"}) {
            write(assets.resolve("minecraft/models/block/custom/" + name + ".json"),
                    "{\"textures\":{\"0\":\"block/custom/" + name + "\"},\"elements\":["
                            + "{\"from\":[4,0,5],\"to\":[7,2,8],\"faces\":{"
                            + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#0\"}}}]}");
        }

        BlockStateMapper mapper = new BlockStateMapper().withModelResolver(new JavaModelResolver());
        mapper.addFromBlockstatesDirectory(assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        assertEquals(2, mapper.getGeneratedGeometry().size(), "two models must not share one geometry file");
    }

    /** A model Bedrock would refuse has to degrade to a full block rather than produce a pack the client drops. */
    @Test
    void aModelBeyondBedrocksLimitsFallsBackToAFullBlock() throws Exception {
        JsonObject definition = convert(
                "{\"textures\":{\"0\":\"block/custom/thing\"},\"elements\":["
                        + "{\"from\":[0,0,0],\"to\":[16,40,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#0\"}}}]}",
                null, null);

        // 40 units tall is past the 30-unit block model limit.
        assertEquals("minecraft:geometry.full_block", geometryOf(definition));
    }

    @Test
    void aModelThatResolvesToNothingStillMapsToAFullBlock() throws Exception {
        JsonObject definition = convert(
                "{\"parent\":\"block/not_shipped_anywhere\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                null, null);

        assertNotNull(definition);
        assertEquals("minecraft:geometry.full_block", geometryOf(definition));
    }
}
