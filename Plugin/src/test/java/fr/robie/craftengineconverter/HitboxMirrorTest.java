package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockGeometryBuilder;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A collision box is written in the same mirrored space as the geometry it wraps.
 * <p>
 * Block geometry is emitted mirrored along X, and a box is written in that same space, so the two only line up if
 * they are mirrored alike.
 * <p>
 * A door settles it, because it presents the question four different ways. Leaving the box in Java's coordinates
 * put it on the far side of the block for every state whose panel is thin along X — facing east and west closed,
 * facing north and south open — and left it correct for every state whose panel is thin along Z, where an X mirror
 * changes nothing. Wrong precisely where the mirror mattered, right precisely where it did not.
 * <p>
 * Every shape whose X extent is symmetric hides this completely, and most are: a full cube, a fence post, a closed
 * gate. So this uses a panel pushed hard against one face.
 */
class HitboxMirrorTest {

    /** A door-shaped panel: three units thick against the west face, full height and depth. */
    private static JavaBlockModel panelAgainstWest() {
        JavaBlockModel model = new JavaBlockModel(null, false);
        model.addTexture("all", "block/custom/door");
        JavaBlockModel.Element panel = new JavaBlockModel.Element(0, 0, 0, 3, 16, 16);
        panel.addFace("north", "#all", 0, 0, 3, 16, 0);
        model.addElement(panel);
        return model;
    }

    @Test
    void aCollisionBoxIsMirroredWithTheGeometry() {
        BlockGeometryBuilder.Boxes boxes = BlockGeometryBuilder.boxesFor(panelAgainstWest());

        assertEquals(1, boxes.collision().size(), "one solid element, one box");
        BlockDefinition.Box box = boxes.collision().getFirst();

        // Java's 0..3 mirrors to 13..16, which is +5..+8 once Bedrock's block space centres x on the block.
        assertEquals(5.0F, box.originX(), 0.001F,
                "the box mirrors with the shape it wraps; -8 here would be Java's own coordinates, unmirrored");
        assertEquals(3.0F, box.sizeX(), 0.001F, "and keeps its thickness");
    }

    @Test
    void theSelectionOutlineFollowsTheSameSide() {
        BlockDefinition.Box selection = BlockGeometryBuilder.boxesFor(panelAgainstWest()).selection();
        assertEquals(5.0F, selection.originX(), 0.001F);
        assertEquals(3.0F, selection.sizeX(), 0.001F);
    }

    /** Y and Z are untouched by an X mirror either way, so they pin that nothing else drifted. */
    @Test
    void theOtherAxesAreUnchanged() {
        BlockDefinition.Box box = BlockGeometryBuilder.boxesFor(panelAgainstWest()).collision().getFirst();
        assertEquals(0.0F, box.originY(), 0.001F, "y starts at the block floor");
        assertEquals(-8.0F, box.originZ(), 0.001F, "z is centred like x");
        assertEquals(16.0F, box.sizeY(), 0.001F);
        assertEquals(16.0F, box.sizeZ(), 0.001F);
    }
}
