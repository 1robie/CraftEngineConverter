package fr.robie.craftengineconverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockStateMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaModelResolver;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which alpha-testing method a see-through block draws with, which decides whether you can see its own far side
 * through it.
 * <p>
 * Bedrock's two transparent methods differ only in backface visibility, and vanilla splits its blocks accordingly
 * ({@code bedrock-wiki/blocks/block-visuals-intro}, "Render Methods"):
 * <pre>
 * alpha_test              backfaces visible    Ladder, Monster Spawner, Vines
 * alpha_test_single_sided backfaces hidden     Doors, Saplings, Trapdoors
 * </pre>
 * Using {@code alpha_test} for everything meant looking through a door's window and seeing the inside of its far
 * side, which Java never shows because it culls backfaces. A flat sheet needs the opposite: a ladder or a cross
 * <b>is</b> its own backface and would be invisible from one side without them.
 */
class RenderMethodBackfaceTest {

    private static void write(Path file, String contents) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, contents);
    }

    /** A texture with a see-through pixel, so the block is classed as transparent at all. */
    private static void writeTransparentTexture(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) image.setRGB(x, y, 0xFF808080);
        image.setRGB(0, 0, 0x00000000);
        ImageIO.write(image, "PNG", file.toFile());
    }

    /** Converts one block whose model is the given elements, and returns the render method it was given. */
    private static String renderMethodOf(String elements) throws Exception {
        Path root = Files.createTempDirectory("render-method");
        Path assets = root.resolve("assets");

        write(assets.resolve("minecraft/blockstates/test_block.json"),
                "{\"variants\":{\"\":{\"model\":\"block/custom/thing\"}}}");
        write(assets.resolve("minecraft/models/block/custom/thing.json"),
                "{\"textures\":{\"all\":\"block/custom/thing\"},\"elements\":" + elements + "}");
        writeTransparentTexture(assets.resolve("minecraft/textures/block/custom/thing.png"));

        // The texture pipeline is what answers hasTransparency; without it every block reads as opaque and the
        // question under test never even arises.
        BlockStateMapper mapper = new BlockStateMapper()
                .withModelResolver(new JavaModelResolver())
                .withTexturePipeline(new fr.robie.craftengineconverter.converter.bedrock.texture.TexturePipeline());
        mapper.addFromBlockstatesDirectory(
                assets.resolve("minecraft/blockstates").toFile(), "minecraft", assets);

        Path out = root.resolve("out");
        Files.createDirectories(out);
        mapper.save(out);

        JsonObject blocks = JsonParser.parseString(Files.readString(out.resolve("geyser_block_mappings.json")))
                .getAsJsonObject().getAsJsonObject("blocks");
        JsonObject entry = blocks.getAsJsonObject(blocks.keySet().iterator().next());
        JsonObject instances = entry.has("material_instances")
                ? entry.getAsJsonObject("material_instances")
                : entry.getAsJsonObject("state_overrides").entrySet().iterator().next()
                        .getValue().getAsJsonObject().getAsJsonObject("material_instances");
        JsonObject first = instances.entrySet().iterator().next().getValue().getAsJsonObject();
        return first.get("render_method").getAsString();
    }

    /** A door is three units thick, so its far side must not show through the window in its near side. */
    @Test
    void aShapeWithThicknessHidesItsBackfaces() throws Exception {
        assertEquals("alpha_test_single_sided", renderMethodOf(
                        "[{\"from\":[0,0,0],\"to\":[3,16,16],\"faces\":{"
                                + "\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"},"
                                + "\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]"),
                "a door seen through its own window must not show the inside of its far side");
    }

    /** A cross or ladder is one sheet: hide its backfaces and it disappears from one side. */
    @Test
    void aFlatSheetKeepsItsBackfacesVisible() throws Exception {
        assertEquals("alpha_test", renderMethodOf(
                        "[{\"from\":[0,0,8],\"to\":[16,16,8],\"faces\":{"
                                + "\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#all\"}}}]"),
                "a sheet is its own backface, so hiding those would leave it invisible from behind");
    }
}
