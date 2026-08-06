package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import fr.robie.craftengineconverter.converter.bedrock.geometry.VanillaBlockShapes;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a full-cube block's faces actually say.
 * <p>
 * Bedrock's built-in unit cube maps every face to its whole texture at rotation zero, and
 * {@code material_instances} only chooses <b>which</b> texture a face uses. So a block whose appearance depends on
 * per-face rotation — the glazed-terracotta family, and any chessboard-like pattern built on it — cannot be
 * expressed that way and must get real geometry instead. These pin that, and the shape rebuild that feeds it.
 */
class BlockTextureTest {

    private record Converted(JsonObject definition, Map<String, BedrockGeometry> geometry) {}

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    /** A pack with one blockstate pointing at one model, plus a stub of the vanilla parent it inherits. */
    private static Converted convert(String modelJson, String vanillaParentPath, String vanillaParentJson)
            throws Exception {
        Path root = Files.createTempDirectory("blocktexture");
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
        return new Converted(blocks.getAsJsonObject(blocks.keySet().iterator().next()),
                mapper.getGeneratedGeometry());
    }

    private static String geometryOf(JsonObject definition) {
        return definition.getAsJsonObject("geometry").get("identifier").getAsString();
    }

    private static JsonObject facesOf(BedrockGeometry geometry) {
        return geometry.serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject()
                .getAsJsonArray("cubes").get(0).getAsJsonObject()
                .getAsJsonObject("uv");
    }

    private static int uvRotationOf(JsonObject faces, String direction) {
        JsonObject face = faces.getAsJsonObject(direction);
        assertNotNull(face, "no " + direction + " face");
        return face.has("uv_rotation") ? face.get("uv_rotation").getAsInt() : 0;
    }

    // ---------------------------------------------------------------- routing

    /** A plain cube keeps the cheap built-in shape and ships no geometry file. */
    @Test
    void aPlainFullCubeStillUsesTheBuiltInUnitCube() throws Exception {
        Converted converted = convert(
                "{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                "block/cube_all",
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");

        assertEquals("minecraft:geometry.full_block", geometryOf(converted.definition()));
        assertTrue(converted.geometry().isEmpty(), "a plain cube needs no geometry file");
    }

    /**
     * The chessboard case. A full cube whose faces carry rotations must get real geometry, because the unit cube
     * cannot turn a face's texture at all.
     */
    @Test
    void aFullCubeWithFaceRotationsGetsRealGeometryCarryingThem() throws Exception {
        Converted converted = convert(
                "{\"parent\":\"block/template_glazed_terracotta\",\"textures\":{\"pattern\":\"block/custom/thing\"}}",
                "block/template_glazed_terracotta",
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"down\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\"},"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\"},"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\",\"rotation\":90},"
                        + "\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\",\"rotation\":270},"
                        + "\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\",\"rotation\":0},"
                        + "\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#pattern\",\"rotation\":180}}}]}");

        assertFalse("minecraft:geometry.full_block".equals(geometryOf(converted.definition())),
                "a rotated-face cube must not be flattened onto the unit cube");
        assertEquals(1, converted.geometry().size(), "it needs a geometry file of its own");

        // Block geometry is mirrored along X, so the two side faces trade places: what Java authored on the east
        // arrives on the west and vice versa, each keeping its own rotation. North and south straddle the mirror
        // plane and stay put. Distinct rotations per face are what make the swap visible here at all.
        JsonObject faces = facesOf(converted.geometry().values().iterator().next());
        assertEquals(90, uvRotationOf(faces, "north"));
        assertEquals(270, uvRotationOf(faces, "south"));
        assertEquals(180, uvRotationOf(faces, "west"), "Java's east face, rotated 180, lands on the west");
        assertEquals(0, uvRotationOf(faces, "east"), "Java's west face, rotated 0, lands on the east");
    }

    /** Partial UVs are the other thing a unit cube cannot sample. */
    @Test
    void aFullCubeWithPartialUvsGetsRealGeometry() throws Exception {
        Converted converted = convert(
                "{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                "block/cube_all",
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,8,8],\"texture\":\"#all\"}}}]}");

        assertFalse("minecraft:geometry.full_block".equals(geometryOf(converted.definition())),
                "a cube sampling part of its texture must not use the unit cube");
    }

    /**
     * Bedrock renders a custom block in hand and in the inventory from the block's own geometry, posed by
     * {@code item_display_transforms} — so a block with a Java {@code display} block needs a geometry file for the
     * pose to live in, even when its shape is a plain cube.
     */
    @Test
    void aFullCubeWithADisplayBlockGetsGeometryCarryingItemDisplayTransforms() throws Exception {
        Converted converted = convert(
                "{\"parent\":\"block/cube_all\",\"textures\":{\"all\":\"block/custom/thing\"}}",
                "block/cube_all",
                "{\"display\":{\"thirdperson_righthand\":{\"rotation\":[75,45,0],"
                        + "\"translation\":[0,2.5,0],\"scale\":[0.375,0.375,0.375]}},"
                        + "\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");

        assertFalse("minecraft:geometry.full_block".equals(geometryOf(converted.definition())),
                "a posed block needs geometry the pose can live in");
        assertEquals(1, converted.geometry().size());

        JsonObject root = converted.geometry().values().iterator().next().serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertTrue(root.has("item_display_transforms"),
                "the block's Java display block must reach the geometry");
        assertNotNull(root.getAsJsonObject("item_display_transforms").get("thirdperson_righthand"));
    }

    // ---------------------------------------------------------------- the shape rebuild

    /**
     * When the vanilla parent is not cached the cube is rebuilt from the parent's name. That rebuild used to
     * hardcode rotation zero on all six faces, which is exactly what silently dropped the glazed-terracotta
     * rotations — right in Blockbench, which has the real parent, wrong in game.
     */
    @Test
    void theNameBasedRebuildCarriesTheGlazedTerracottaRotations() {
        JavaBlockModel model = new JavaBlockModel("block/template_glazed_terracotta", true);
        model.addTexture("pattern", "block/custom/chessboard");

        assertTrue(VanillaBlockShapes.addCube(model, "block/template_glazed_terracotta"));

        Map<String, Integer> rotations = new java.util.HashMap<>();
        for (JavaBlockModel.Face face : model.elements().getFirst().faces()) {
            rotations.put(face.direction(), face.rotation());
        }
        assertEquals(90, rotations.get("north"), "north");
        assertEquals(270, rotations.get("south"), "south");
        assertEquals(0, rotations.get("west"), "west");
        assertEquals(180, rotations.get("east"), "east");
        assertEquals(0, rotations.get("up"), "up");
        assertEquals(0, rotations.get("down"), "down");
    }

    @Test
    void otherRebuiltShapesStayUnrotated() {
        JavaBlockModel model = new JavaBlockModel("block/cube_all", true);
        model.addTexture("all", "block/custom/thing");

        assertTrue(VanillaBlockShapes.addCube(model, "block/cube_all"));
        for (JavaBlockModel.Face face : model.elements().getFirst().faces()) {
            assertEquals(0, face.rotation(), face.direction() + " should be unrotated");
        }
    }

    // ---------------------------------------------------------------- baked variant rotation

    /**
     * Turning a block about Y carries the four sides onto one another, each keeping its texture upright — but the
     * axis runs through the top and bottom, so there the turn happens within the face's own plane and shows as a
     * quarter turn of the texture. Without this, all four facings of a patterned block show the same top.
     */
    @Test
    void bakingAVariantRotationTurnsOnlyTheTopAndBottomTextures() {
        assertEquals(90, GeometryMapper.rotatedFaceRotation("up", 0, 90), "up follows the block round");
        assertEquals(270, GeometryMapper.rotatedFaceRotation("down", 0, 90), "down turns the other way");

        for (String side : new String[]{"north", "south", "east", "west"}) {
            assertEquals(0, GeometryMapper.rotatedFaceRotation(side, 0, 90),
                    side + " keeps its texture upright as it moves");
        }
    }

    @Test
    void bakedFaceRotationAddsToTheFacesOwnAndStaysInRange() {
        assertEquals(0, GeometryMapper.rotatedFaceRotation("up", 270, 90), "270 + 90 wraps to 0");
        assertEquals(180, GeometryMapper.rotatedFaceRotation("up", 90, 90));
        assertEquals(0, GeometryMapper.rotatedFaceRotation("down", 90, 90), "90 - 90");
        assertEquals(270, GeometryMapper.rotatedFaceRotation("down", 0, 90), "0 - 90 wraps positive");
    }
}
