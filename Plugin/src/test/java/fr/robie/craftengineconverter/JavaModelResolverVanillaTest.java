package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A pack's models inherit their shape from vanilla parents that ship in the game jar. These pin the resolution
 * order: the pack wins, then the vanilla assets, and only with neither does the shape get guessed from the
 * parent's name.
 */
class JavaModelResolverVanillaTest {

    private static Path write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
        return file;
    }

    /** A pack whose one model inherits {@code block/cactus} and ships nothing else. */
    private static Path packWithCactusChild(Path root) throws Exception {
        write(root.resolve("assets/default/models/block/custom/coil.json"),
                "{\"parent\":\"block/cactus\",\"textures\":{\"top\":\"block/custom/coil\","
                        + "\"bottom\":\"block/custom/coil\",\"side\":\"block/custom/coil_side\"}}");
        return root.resolve("assets");
    }

    /** Vanilla's own cactus: two elements, unlike the single cube a guess would produce. */
    private static VanillaAssets vanillaWithCactus(Path root) throws Exception {
        write(root.resolve("vanilla/assets/minecraft/models/block/cactus.json"),
                "{\"parent\":\"block/block\",\"elements\":["
                        + "{\"from\":[1,0,0],\"to\":[15,16,16],\"faces\":{"
                        + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#side\"}}},"
                        + "{\"from\":[0,0,1],\"to\":[16,16,15],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,16,16],\"texture\":\"#top\"}}}]}");
        return new VanillaAssets(root.resolve("cache"), root.resolve("vanilla"));
    }

    @Test
    void aVanillaParentSuppliesItsRealShape() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        Path assets = packWithCactusChild(root);

        JavaBlockModel model = new JavaModelResolver()
                .withVanillaAssets(vanillaWithCactus(root))
                .load("default:block/custom/coil", assets);

        assertNotNull(model);
        // Two elements is cactus's actual shape; the name-based guess can only ever make one cube, so this is
        // what proves the real parent was used.
        assertEquals(2, model.elements().size());
        assertTrue(model.textures().containsKey("side"));
    }

    @Test
    void withoutVanillaAssetsTheShapeIsGuessed() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        Path assets = packWithCactusChild(root);

        JavaBlockModel model = new JavaModelResolver().load("default:block/custom/coil", assets);

        assertNotNull(model);
        // One cube from VanillaBlockShapes: worse than the truth, far better than the flat square an
        // element-less model falls back to.
        assertEquals(1, model.elements().size());
        assertEquals(6, model.elements().getFirst().faces().size());
    }

    /**
     * Every item naming the same unresolvable parent must get the shape, not just the first. The failed lookup is
     * cached as null, so keying the rebuild on the cache rather than on the parent left every later item flat.
     */
    @Test
    void everyModelSharingAnUnresolvableParentGetsTheShape() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        Path assets = packWithCactusChild(root);
        write(root.resolve("assets/default/models/block/custom/coil2.json"),
                "{\"parent\":\"block/cactus\",\"textures\":{\"side\":\"block/custom/other\"}}");

        JavaModelResolver resolver = new JavaModelResolver();
        JavaBlockModel first = resolver.load("default:block/custom/coil", assets);
        JavaBlockModel second = resolver.load("default:block/custom/coil2", assets);

        assertEquals(1, first.elements().size());
        assertEquals(1, second.elements().size(), "the second model naming the same parent also needs its shape");
    }

    @Test
    void aPackShippingItsOwnCopyOfAVanillaModelWins() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        Path assets = packWithCactusChild(root);
        // The pack overrides block/cactus with a single-element shape of its own.
        write(root.resolve("assets/minecraft/models/block/cactus.json"),
                "{\"elements\":[{\"from\":[0,0,0],\"to\":[8,8,8],\"faces\":{"
                        + "\"up\":{\"uv\":[0,0,8,8],\"texture\":\"#top\"}}}]}");

        JavaBlockModel model = new JavaModelResolver()
                .withVanillaAssets(vanillaWithCactus(root))
                .load("default:block/custom/coil", assets);

        assertNotNull(model);
        assertEquals(1, model.elements().size(), "the pack's own model must take precedence over vanilla's");
        assertEquals(8.0F, model.elements().getFirst().toX(), 0.001F);
    }

    @Test
    void anUnknownParentWithNoSourceLeavesTheModelAlone() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        // A parent that is neither shipped nor a shape VanillaBlockShapes knows.
        write(root.resolve("assets/default/models/item/thing.json"),
                "{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"item/custom/thing\"}}");

        JavaBlockModel model = new JavaModelResolver().load("default:item/thing", root.resolve("assets"));

        assertNotNull(model);
        // item/generated is a flat sprite, and inventing a cube for it would be worse than leaving it be.
        assertTrue(model.elements().isEmpty());
    }

    @Test
    void amissingModelStillResolvesToNull() throws Exception {
        Path root = Files.createTempDirectory("resolver");
        Files.createDirectories(root.resolve("assets"));

        assertNull(new JavaModelResolver().load("default:block/custom/nope", root.resolve("assets")));
    }
}
