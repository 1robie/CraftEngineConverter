package fr.robie.craftengineconverter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pixel lattice is what makes an animated item render in 3D without baking any one frame's silhouette
 * into the model, so what matters is that it covers the same box as the extruded geometry, gives every pixel
 * its own cube, and maps each cube to exactly one distinct texel.
 */
class PixelLatticeGeometryTest {

    private static JsonObject latticeDescription(JsonObject root) {
        return root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject()
                .getAsJsonObject("description");
    }

    private static JsonArray latticeCubes(JsonObject root) {
        JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonArray bones = geo.getAsJsonArray("bones");
        assertEquals(1, bones.size(), "the lattice is one bone");
        return bones.get(0).getAsJsonObject().getAsJsonArray("cubes");
    }

    @Test
    void oneCubePerPixelCoveringTheItemBox() {
        JsonObject root = GeometryMapper.createPixelLatticeGeometry("lattice_16x16", 16, 16).serialize();

        JsonObject description = latticeDescription(root);
        assertEquals("geometry.lattice_16x16", description.get("identifier").getAsString());
        assertEquals(16, description.get("texture_width").getAsInt());
        assertEquals(16, description.get("texture_height").getAsInt());

        JsonArray cubes = latticeCubes(root);
        assertEquals(256, cubes.size(), "16x16 pixels is 256 cubes");

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject cube = cubes.get(i).getAsJsonObject();
            JsonArray origin = cube.getAsJsonArray("origin");
            JsonArray size = cube.getAsJsonArray("size");

            minX = Math.min(minX, origin.get(0).getAsFloat());
            maxX = Math.max(maxX, origin.get(0).getAsFloat() + size.get(0).getAsFloat());
            minY = Math.min(minY, origin.get(1).getAsFloat());
            maxY = Math.max(maxY, origin.get(1).getAsFloat() + size.get(1).getAsFloat());

            // Depth 1, centred on z=0, exactly as the extruded geometry does it.
            assertEquals(-0.5F, origin.get(2).getAsFloat(), 0.0001F);
            assertEquals(1.0F, size.get(2).getAsFloat(), 0.0001F);
        }

        // The same 16x16 box the extruded geometry occupies, so existing animations still fit.
        assertEquals(-8.0F, minX, 0.0001F);
        assertEquals(8.0F, maxX, 0.0001F);
        assertEquals(0.0F, minY, 0.0001F);
        assertEquals(16.0F, maxY, 0.0001F);
    }

    @Test
    void everyCubeMapsToItsOwnSinglePixelOnEverySide() {
        JsonArray cubes = latticeCubes(
                GeometryMapper.createPixelLatticeGeometry("lattice_16x16", 16, 16).serialize());

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < cubes.size(); i++) {
            JsonObject uv = cubes.get(i).getAsJsonObject().getAsJsonObject("uv");

            for (String face : new String[]{"north", "south", "east", "west", "up", "down"}) {
                JsonObject entry = uv.getAsJsonObject(face);
                assertNotNull(entry, "face " + face + " must be textured");
                assertEquals(1.0F, entry.getAsJsonArray("uv_size").get(0).getAsFloat(), 0.0001F,
                        face + " must span a single pixel horizontally");
            }

            JsonArray north = uv.getAsJsonObject("north").getAsJsonArray("uv");
            float u = north.get(0).getAsFloat();
            float v = north.get(1).getAsFloat();

            // Every side but "down" samples that same pixel; "down" samples it with V flipped.
            for (String face : new String[]{"south", "east", "west", "up"}) {
                JsonArray other = uv.getAsJsonObject(face).getAsJsonArray("uv");
                assertEquals(u, other.get(0).getAsFloat(), 0.0001F, face + " u");
                assertEquals(v, other.get(1).getAsFloat(), 0.0001F, face + " v");
            }
            JsonObject down = uv.getAsJsonObject("down");
            assertEquals(v + 1, down.getAsJsonArray("uv").get(1).getAsFloat(), 0.0001F);
            assertEquals(-1.0F, down.getAsJsonArray("uv_size").get(1).getAsFloat(), 0.0001F);

            assertTrue(seen.add(u + "/" + v), "pixel " + u + "," + v + " is claimed by two cubes");
        }
        assertEquals(256, seen.size(), "every pixel of the texture must be covered exactly once");
    }

    @Test
    void nonSquareTexturesScaleToTheSameBox() {
        JsonArray cubes = latticeCubes(
                GeometryMapper.createPixelLatticeGeometry("lattice_32x16", 32, 16).serialize());

        assertEquals(32 * 16, cubes.size());
        JsonArray size = cubes.get(0).getAsJsonObject().getAsJsonArray("size");
        assertEquals(0.5F, size.get(0).getAsFloat(), 0.0001F, "32 columns across 16 units");
        assertEquals(1.0F, size.get(1).getAsFloat(), 0.0001F, "16 rows across 16 units");
    }

    /**
     * The whole point of the lattice: nothing about it depends on any particular texture, so one instance is
     * shareable by every animated item of the same frame size.
     */
    @Test
    void isIndependentOfAnyTexture() {
        String first = GeometryMapper.createPixelLatticeGeometry("lattice_16x16", 16, 16).serialize().toString();
        String second = GeometryMapper.createPixelLatticeGeometry("lattice_16x16", 16, 16).serialize().toString();
        assertEquals(first, second);
    }
}
