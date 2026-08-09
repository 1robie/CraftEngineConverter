package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.converter.bedrock.block.BlockGeometryBuilder;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the direction a blockstate's {@code x}/{@code y} rotation turns a block, because getting the sign wrong is
 * invisible on two of the four facings and shows the block's back on the other two.
 */
class BakedBlockRotationTest {

    /** Vanilla's {@code block/stairs}: a bottom slab plus a step on the <b>east</b> half. */
    private static JavaBlockModel stairs() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        model.addTexture("side", "block/oak_planks");
        JavaBlockModel.Element slab = new JavaBlockModel.Element(0, 0, 0, 16, 8, 16);
        slab.addFace("up", "#side", 0, 0, 16, 16, 0);
        JavaBlockModel.Element step = new JavaBlockModel.Element(8, 8, 0, 16, 16, 16);
        step.addFace("east", "#side", 0, 0, 16, 16, 0);
        step.addFace("west", "#side", 0, 0, 16, 16, 0);
        model.addElement(slab);
        model.addElement(step);
        return model;
    }

    /**
     * {@code block/stairs} unrotated is the {@code facing=east} variant and {@code facing=south} is {@code y: 90}, so
     * {@code y: 90} has to carry the step from the east half to the south half. Passing the angle through
     * un-negated sent it north instead.
     */
    @Test
    void yNinetyCarriesTheStepFromEastToSouth() {
        JavaBlockModel rotated = GeometryMapper.rotateModel(stairs(), 0, 90);
        JavaBlockModel.Element step = rotated.elements().get(1);

        assertEquals(0.0F, step.fromX(), 0.001F, "the step should now span the whole width");
        assertEquals(16.0F, step.toX(), 0.001F);
        assertEquals(8.0F, step.fromZ(), 0.001F, "the step should sit on the south half");
        assertEquals(16.0F, step.toZ(), 0.001F);
        assertEquals(8.0F, step.fromY(), 0.001F, "y is untouched by a y rotation");
    }

    /** The face that pointed east now points south, and its UV is untouched — which is what {@code uvlock} means. */
    @Test
    void aFaceIsCarriedRoundKeepingItsUv() {
        JavaBlockModel.Element step = GeometryMapper.rotateModel(stairs(), 0, 90).elements().get(1);

        JavaBlockModel.Face south = step.faces().stream()
                .filter(f -> f.direction().equals("south")).findFirst().orElseThrow();
        JavaBlockModel.Face north = step.faces().stream()
                .filter(f -> f.direction().equals("north")).findFirst().orElseThrow();

        assertEquals(0.0F, south.u0(), 0.001F);
        assertEquals(16.0F, south.u1(), 0.001F);
        assertEquals(0.0F, north.u0(), 0.001F);
    }

    /** {@code x: 90} takes the top face to the north, matching vanilla's {@code rotationYXZ(-y, -x, 0)}. */
    @Test
    void xNinetyTakesTheTopToTheNorth() {
        JavaBlockModel model = new JavaBlockModel(null, true);
        JavaBlockModel.Element plate = new JavaBlockModel.Element(0, 14, 0, 16, 16, 16);
        plate.addFace("up", "#t", 0, 0, 16, 16, 0);
        model.addElement(plate);
        model.addTexture("t", "block/stone");

        JavaBlockModel.Element rotated = GeometryMapper.rotateModel(model, 90, 0).elements().getFirst();

        assertEquals(0.0F, rotated.fromZ(), 0.001F, "the plate should be against the north wall");
        assertEquals(2.0F, rotated.toZ(), 0.001F);
        assertEquals(0.0F, rotated.fromY(), 0.001F, "and span the full height");
        assertEquals(16.0F, rotated.toY(), 0.001F);
        assertEquals("north", rotated.faces().getFirst().direction());
    }

    /** A full turn is the identity, which catches a sign error that is self-consistent but wrong. */
    @Test
    void fourQuarterTurnsReturnTheModel() {
        JavaBlockModel once = GeometryMapper.rotateModel(stairs(), 0, 90);
        JavaBlockModel twice = GeometryMapper.rotateModel(once, 0, 90);
        JavaBlockModel thrice = GeometryMapper.rotateModel(twice, 0, 90);
        JavaBlockModel full = GeometryMapper.rotateModel(thrice, 0, 90);

        JavaBlockModel.Element step = full.elements().get(1);
        assertEquals(8.0F, step.fromX(), 0.001F);
        assertEquals(16.0F, step.toX(), 0.001F);
        assertEquals(0.0F, step.fromZ(), 0.001F);
        assertEquals(16.0F, step.toZ(), 0.001F);
    }

    /** The boxes follow the drawn shape, because they are measured from the rotated model rather than rotated again. */
    @Test
    void boxesFollowTheRotation() {
        BlockGeometryBuilder.Boxes boxes = BlockGeometryBuilder.boxesFor(stairs(), 0, 90);

        assertEquals(2, boxes.collision().size(), "a stair collides as two parts, not as one full cube");
        // Origin is x/z centred on the block, y from its floor.
        BlockDefinitionBox slab = box(boxes, 0);
        BlockDefinitionBox step = box(boxes, 1);
        assertEquals(8.0F, slab.sizeY, 0.001F);
        assertEquals(16.0F, step.sizeX, 0.001F, "the step spans the width once turned");
        assertEquals(8.0F, step.sizeZ, 0.001F);
        assertEquals(0.0F, step.originZ, 0.001F, "and starts at the block's middle, on the south half");
        assertEquals(8.0F, step.originY, 0.001F);
    }

    /** Geometry is produced for the rotated shape, so the emitted cubes and the boxes come from the same turn. */
    @Test
    void rotatedGeometryIsBuiltFromTheTurnedCubes() {
        Set<String> instances = new LinkedHashSet<>();
        BedrockGeometry geometry = new GeometryMapper()
                .mapRotatedBlockGeometry("blocks.test_x0y90", stairs(), instances, 0, 90);

        assertTrue(!geometry.hasNoCubes(), "the rotated model still has cubes");
        assertTrue(instances.contains("side"), "material instances are still named after the Java texture variable");
    }

    private record BlockDefinitionBox(float originX, float originY, float originZ,
                                      float sizeX, float sizeY, float sizeZ) {}

    private static BlockDefinitionBox box(BlockGeometryBuilder.Boxes boxes, int index) {
        var b = boxes.collision().get(index);
        return new BlockDefinitionBox(b.originX(), b.originY(), b.originZ(), b.sizeX(), b.sizeY(), b.sizeZ());
    }
}
