package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.converter.bedrock.pack.PackPathShortener;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeping every pack path under the 80 characters some Bedrock platforms fail on, and keeping the pack small.
 * <p>
 * The generated file names are safe to change because Bedrock scans those directories and indexes each entry by the
 * identifier inside the file. The names that are <b>not</b> safe are addressed by path, and the tests below pin that
 * distinction as much as they pin the shortening itself - getting it wrong yields a pack whose icons are right and
 * whose held models are untextured, which is a hard thing to notice.
 */
class PackPathShortenerTest {

    /** The worst real case: 85 characters, and the one Geyser named. */
    private static final String LONG_CONTROLLER =
            "controller.render.internal.previous_page_1.render_controllers.json";

    private static Path pack() throws IOException {
        Path pack = Files.createTempDirectory("pack");

        write(pack, "render_controllers/" + LONG_CONTROLLER);
        write(pack, "render_controllers/controller.render.craftengine.palm_fence.render_controllers.json");
        write(pack, "models/entity/geometry.craftengine.pixel_lattice_16x16.geo.json");
        write(pack, "models/blocks/craftengine_palm_fence_post.geo.json");
        write(pack, "animations/craftengine.palm_fence.animation.json");
        write(pack, "animation_controllers/craftengine.palm_fence.animation_controllers.json");
        write(pack, "attachables/craftengine.palm_fence.json");

        // Addressed by path, every one of them, and so out of bounds for the pass.
        write(pack, "manifest.json");
        write(pack, "pack_icon.png");
        write(pack, "textures/item_texture.json");
        write(pack, "textures/terrain_texture.json");
        write(pack, "textures/flipbook_textures.json");
        write(pack, "textures/craftengine/item/palm_fence.png");
        write(pack, "sounds/sound_definitions.json");
        write(pack, "sounds/craftengine/chime.ogg");
        write(pack, "texts/languages.json");
        write(pack, "texts/en_US.lang");
        write(pack, "ui/chest_screen.json");
        write(pack, "font/glyph_E0.png");
        return pack;
    }

    private static void write(Path pack, String relative) throws IOException {
        Path file = pack.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{}");
    }

    private static List<String> paths(Path pack) throws IOException {
        try (Stream<Path> walk = Files.walk(pack)) {
            return walk.filter(Files::isRegularFile)
                    .map(file -> pack.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    // ---------------------------------------------------------------- the limit

    @Test
    void nothingReachesTheLimitAfterThePass() throws Exception {
        Path pack = pack();
        assertTrue(("render_controllers/" + LONG_CONTROLLER).length() >= PackPathShortener.PATH_LIMIT,
                "the fixture has to actually contain an offending path");

        PackPathShortener.shorten(pack);

        for (String path : paths(pack)) {
            assertTrue(path.length() < PackPathShortener.PATH_LIMIT,
                    "still over the limit: " + path + " (" + path.length() + ")");
        }
    }

    @Test
    void reportLongPathsMeasuresTheLongest() throws Exception {
        Path pack = pack();
        int before = PackPathShortener.reportLongPaths(pack);
        PackPathShortener.shorten(pack);
        int after = PackPathShortener.reportLongPaths(pack);

        assertTrue(after < before, "the longest path should have got shorter, " + before + " -> " + after);
        assertTrue(after < PackPathShortener.PATH_LIMIT, "and be under the limit, got " + after);
    }

    // ---------------------------------------------------------------- scope

    @Test
    void filesAddressedByPathAreLeftAlone() throws Exception {
        Path pack = pack();
        PackPathShortener.shorten(pack);

        // Each of these is loaded by exact path, and a rename would silently drop the feature it carries.
        for (String untouched : new String[]{
                "manifest.json", "pack_icon.png",
                "textures/item_texture.json", "textures/terrain_texture.json", "textures/flipbook_textures.json",
                "textures/craftengine/item/palm_fence.png",
                "sounds/sound_definitions.json", "sounds/craftengine/chime.ogg",
                "texts/languages.json", "texts/en_US.lang",
                "ui/chest_screen.json", "font/glyph_E0.png"}) {
            assertTrue(Files.exists(pack.resolve(untouched)), untouched + " must keep its name");
        }
    }

    @Test
    void everyGeneratedFileSurvivesWithItsCompoundExtension() throws Exception {
        Path pack = pack();
        PackPathShortener.shorten(pack);

        // The extension is how Bedrock's scanner decides what kind of file it found, so it has to survive.
        assertEquals(2, countEndingWith(pack.resolve("render_controllers"), ".render_controllers.json"));
        assertEquals(1, countEndingWith(pack.resolve("models/entity"), ".geo.json"));
        assertEquals(1, countEndingWith(pack.resolve("models/blocks"), ".geo.json"));
        assertEquals(1, countEndingWith(pack.resolve("animations"), ".animation.json"));
        assertEquals(1, countEndingWith(pack.resolve("animation_controllers"), ".animation_controllers.json"));
        assertEquals(1, countEndingWith(pack.resolve("attachables"), ".json"));
    }

    private static int countEndingWith(Path dir, String extension) throws IOException {
        try (Stream<Path> list = Files.list(dir)) {
            return (int) list.filter(path -> path.getFileName().toString().endsWith(extension)).count();
        }
    }

    // ---------------------------------------------------------------- determinism

    /**
     * Two conversions of the same pack have to produce the same file names, or every client re-downloads the pack
     * after every conversion and no two packs can be diffed.
     */
    @Test
    void theSamePackShortensToTheSameNames() throws Exception {
        Path first = pack();
        Path second = pack();
        PackPathShortener.shorten(first);
        PackPathShortener.shorten(second);

        assertEquals(paths(first), paths(second), "the shortener must not introduce randomness");
    }

    @Test
    void twoNamesNeverCollapseOntoOne() throws Exception {
        Path pack = Files.createTempDirectory("pack-collide");
        write(pack, "animations/craftengine.one.animation.json");
        write(pack, "animations/craftengine.two.animation.json");
        write(pack, "animations/craftengine.three.animation.json");

        PackPathShortener.shorten(pack);

        assertEquals(3, countEndingWith(pack.resolve("animations"), ".animation.json"),
                "three animations in must be three animations out");
    }

    /** Running twice must not keep churning names - the second pass has nothing left to shorten. */
    @Test
    void aSecondPassIsANoop() throws Exception {
        Path pack = pack();
        PackPathShortener.shorten(pack);
        List<String> once = paths(pack);

        assertEquals(0, PackPathShortener.shorten(pack), "nothing should be left to rename");
        assertEquals(once, paths(pack));
    }

    @Test
    void anEmptyPackIsNotAnError() throws Exception {
        Path pack = Files.createTempDirectory("pack-empty");
        assertEquals(0, PackPathShortener.shorten(pack));
        assertEquals(0, PackPathShortener.reportLongPaths(pack));
    }

    // ---------------------------------------------------------------- size

    /**
     * Indentation was over two thirds of the emitted JSON, and JSON is over three quarters of the pack, so
     * pretty-printing what only Bedrock ever reads more than doubled the download.
     */
    @Test
    void generatedJsonIsWrittenCompact() throws Exception {
        Path file = Files.createTempDirectory("compact").resolve("out.json");
        JsonObject json = new JsonObject();
        JsonObject nested = new JsonObject();
        nested.addProperty("identifier", "geometry.craftengine.thing");
        json.add("minecraft:geometry", nested);

        FileCacheManager.saveJsonToFile(file, json);

        String written = Files.readString(file);
        assertFalse(written.contains("\n"), "generated JSON must be on one line, got:\n" + written);
        assertEquals("{\"minecraft:geometry\":{\"identifier\":\"geometry.craftengine.thing\"}}", written);
    }
}
