package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.converter.bedrock.block.BlockGeometryBuilder;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A collision box must sit where the block is <b>drawn</b>, which is not where its geometry is written.
 * <p>
 * Block geometry is emitted mirrored along X, because Bedrock mirrors it back when it draws — the shape therefore
 * lands where Java put it. A collision box is not drawn: it is a world-space volume the engine takes literally. So
 * the geometry mirrors and the box must not, even though they describe the same block.
 * <p>
 * Mirroring the box too was measured on a door panel: it renders at x -8..-5 and its box sat at +5..+8, the far side
 * of the block. The door could be walked through and the empty half could not. Every shape whose X extent is
 * symmetric hides this completely, which is most of them — a full cube, a fence post, a closed gate — so this uses
 * a panel pushed hard against one face.
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
    void aCollisionBoxStaysOnTheSideJavaDrewIt() {
        BlockGeometryBuilder.Boxes boxes = BlockGeometryBuilder.boxesFor(panelAgainstWest());

        assertEquals(1, boxes.collision().size(), "one solid element, one box");
        BlockDefinition.Box box = boxes.collision().getFirst();

        // Bedrock's block space centres x and z on the block, so Java's 0..3 is -8..-5.
        assertEquals(-8.0F, box.originX(), 0.001F,
                "the box must wrap the panel where it is drawn; +5 here would be the opposite face");
        assertEquals(3.0F, box.sizeX(), 0.001F, "and keep its thickness");
    }

    @Test
    void theSelectionOutlineFollowsTheSameSide() {
        BlockDefinition.Box selection = BlockGeometryBuilder.boxesFor(panelAgainstWest()).selection();
        assertEquals(-8.0F, selection.originX(), 0.001F);
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
