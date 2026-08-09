package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.ConversionContext;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Which file a held item's geometry is built from.
 * <p>
 * The bug this guards: {@code registerGeometry} was being handed a <b>texture</b> reference where it expects a
 * <b>model</b> path, because the caller had already resolved the model into its textures. That silently worked for
 * packs naming a model and its texture alike, and otherwise either found nothing — falling through to a flat
 * extruded sprite — or found a completely different model that happened to share the texture's name. The inventory
 * icon is rendered from the real model and was unaffected, which is why an item could look right in the inventory
 * and wrong in the hand.
 */
class HeldGeometrySourceTest {

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    private static ConversionContext context(Path root) throws Exception {
        Path out = root.resolve("out");
        Files.createDirectories(out);
        return new ConversionContext(out, out, out).withJavaAssetsDir(root.resolve("assets"));
    }

    private static JsonArray cubesOf(BedrockGeometry geometry) {
        return geometry.serialize()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonArray("bones").get(0).getAsJsonObject()
                .getAsJsonArray("cubes");
    }

    /**
     * The decoy case, which is what made {@code palm_button} render as {@code palm_planks}: the button's first
     * texture variable is {@code block/custom/palm_planks}, and a model of that name exists.
     */
    @Test
    void aModelNamedAfterTheTextureIsNotUsedInsteadOfTheRealModel() throws Exception {
        Path root = Files.createTempDirectory("held-geometry");

        // The real model: a small box, like a button.
        write(root.resolve("assets/minecraft/models/block/custom/thing.json"),
                "{\"textures\":{\"all\":\"block/custom/thing_texture\"},"
                        + "\"elements\":[{\"from\":[5,0,6],\"to\":[11,2,10],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");
        // The decoy: a model whose name matches the texture reference above, and a full cube.
        write(root.resolve("assets/minecraft/models/block/custom/thing_texture.json"),
                "{\"textures\":{\"all\":\"block/custom/thing_texture\"},"
                        + "\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]}");

        ConversionContext ctx = context(root);
        ctx.registerGeometry("minecraft:block/custom/thing", "default:thing", 16, 16);

        BedrockGeometry geometry = ctx.collectedGeometry().get("default:thing");
        assertNotNull(geometry, "the real model must produce geometry");

        JsonArray cubes = cubesOf(geometry);
        assertEquals(1, cubes.size());
        JsonArray size = cubes.get(0).getAsJsonObject().getAsJsonArray("size");
        assertEquals(6.0F, size.get(0).getAsFloat(), 0.001F, "the button's own width, not the decoy's 16");
        assertEquals(2.0F, size.get(1).getAsFloat(), 0.001F, "the button's own height");

        // And the old behaviour, pinned so the difference is unambiguous: handing the texture reference in gives
        // the decoy's full cube. This is what shipped, and why a button rendered as a block of planks in hand.
        ctx.registerGeometry("minecraft:block/custom/thing_texture", "default:decoy", 16, 16);
        JsonArray decoy = cubesOf(ctx.collectedGeometry().get("default:decoy"));
        assertEquals(16.0F, decoy.get(0).getAsJsonObject().getAsJsonArray("size").get(0).getAsFloat(), 0.001F,
                "the texture-named model is a full cube - the wrong shape the bug produced");
    }

    /** A null model path — a {@code texture:}-only item — must be tolerated, not crash. */
    @Test
    void aTextureOnlyItemHasNoModelToConvert() throws Exception {
        Path root = Files.createTempDirectory("held-geometry-none");
        ConversionContext ctx = context(root);

        ctx.registerGeometry(null, "default:sprite", 16, 16);

        assertFalse(ctx.collectedGeometry().containsKey("default:sprite"),
                "no model means no geometry, and the caller substitutes an extruded sprite");
    }

    // ---------------------------------------------------------------- against the real vanilla tree

    private static Path vanillaAssets() {
        for (Path candidate : new Path[]{
                Path.of("Minecraft-default-assets-latest"),
                Path.of("..", "Minecraft-default-assets-latest")}) {
            if (Files.isDirectory(candidate.resolve("assets/minecraft/models/block"))) return candidate;
        }
        return null;
    }

    /**
     * The two items actually reported. Their parents — {@code block/anvil} and {@code block/button_inventory} — are
     * not cube shapes, so {@link fr.robie.craftengineconverter.converter.bedrock.geometry.VanillaBlockShapes} rightly
     * cannot rebuild them from a name; they need the real vanilla tree. With it, they must come out as their own
     * multi-box shapes rather than as extruded sprites.
     * <p>
     * Skipped when the assets are absent, so a normal checkout still builds.
     */
    @Test
    void anvilAndButtonBecomeRealShapesWhenTheirVanillaParentResolves() throws Exception {
        Path assets = vanillaAssets();
        assumeTrue(assets != null, "no vanilla assets tree available - skipping");

        Path root = Files.createTempDirectory("held-geometry-vanilla");
        write(root.resolve("assets/minecraft/models/block/custom/my_anvil.json"),
                "{\"parent\":\"block/anvil\",\"textures\":{\"top\":\"block/custom/anvil_top\","
                        + "\"body\":\"block/custom/anvil\"}}");
        write(root.resolve("assets/minecraft/models/block/custom/my_button.json"),
                "{\"parent\":\"block/button_inventory\",\"textures\":{\"texture\":\"block/custom/planks\"}}");

        JavaModelResolver resolver = new JavaModelResolver()
                .withVanillaAssets(new VanillaAssets(root.resolve("cache"), assets));

        for (String name : new String[]{"my_anvil", "my_button"}) {
            JavaBlockModel model = resolver.load("minecraft:block/custom/" + name, root.resolve("assets"));
            assertNotNull(model, name + " should resolve");
            assertFalse(model.elements().isEmpty(),
                    name + " must inherit its shape from the vanilla parent, not arrive empty");

            BedrockGeometry geometry = new GeometryMapper().mapGeometry(name, model, 16, 16);
            assertFalse(geometry.hasNoCubes(), name + " must produce real cubes");

            // An extruded sprite is a stack of zero-or-one-thickness slabs at z = -0.5. A real shape is not.
            JsonArray cubes = cubesOf(geometry);
            boolean allFlat = true;
            for (int i = 0; i < cubes.size(); i++) {
                if (cubes.get(i).getAsJsonObject().getAsJsonArray("size").get(2).getAsFloat() > 1.0F) {
                    allFlat = false;
                    break;
                }
            }
            assertFalse(allFlat, name + " came out as a flat extruded sprite, cubes=" + cubes.size());
        }
    }

    /** The anvil really does use two texture variables, which is what the atlas exists for. */
    @Test
    void theAnvilShapeUsesTwoTextureVariables() throws Exception {
        Path assets = vanillaAssets();
        assumeTrue(assets != null, "no vanilla assets tree available - skipping");

        Path root = Files.createTempDirectory("held-geometry-anvil-textures");
        write(root.resolve("assets/minecraft/models/block/custom/my_anvil.json"),
                "{\"parent\":\"block/anvil\",\"textures\":{\"top\":\"block/custom/anvil_top\","
                        + "\"body\":\"block/custom/anvil\"}}");

        JavaBlockModel model = new JavaModelResolver()
                .withVanillaAssets(new VanillaAssets(root.resolve("cache"), assets))
                .load("minecraft:block/custom/my_anvil", root.resolve("assets"));
        assertNotNull(model);

        java.util.Set<String> used = new java.util.HashSet<>();
        for (JavaBlockModel.Element element : model.elements()) {
            for (JavaBlockModel.Face face : element.faces()) used.add(face.texture());
        }
        assertTrue(used.size() > 1, "the anvil's faces should reference more than one texture, got " + used);
    }
}
