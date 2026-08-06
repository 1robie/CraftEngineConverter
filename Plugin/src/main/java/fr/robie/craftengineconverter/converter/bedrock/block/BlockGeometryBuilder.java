package fr.robie.craftengineconverter.converter.bedrock.block;

import fr.robie.craftengineconverter.api.configuration.bedrock.mapping.block.BlockDefinition;
import fr.robie.craftengineconverter.converter.bedrock.geometry.BedrockGeometry;
import fr.robie.craftengineconverter.converter.bedrock.geometry.GeometryMapper;
import fr.robie.craftengineconverter.converter.bedrock.geometry.JavaBlockModel;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Turns a Java block model into Bedrock block geometry plus the material instances that texture it.
 * <p>
 * Needed because Bedrock offers only three built-in block models — a full cube, a crossed plane, and a deprecated
 * cube — so a slab, a stairway, an anvil or a torch can only keep its shape by shipping geometry of its own.
 * Geyser's block mapping accepts any geometry identifier, and the resource pack carries the model, so this is the
 * supported route rather than a workaround.
 */
public final class BlockGeometryBuilder {

    /** A custom block model may not exceed this on any axis. */
    private static final float MAX_SIZE = 30.0F;

    private static final float BLOCK_UNIT = 16.0F;

    /** Geyser's documented range for a collision box, which reaches above the block. */
    private static final float MAX_COLLISION_HEIGHT = 24.0F;

    private BlockGeometryBuilder() {
        throw new UnsupportedOperationException("BlockGeometryBuilder is a utility class and cannot be instantiated.");
    }

    /**
     * @param geometry  the model to write into the pack
     * @param instances the material instances the mapping must declare alongside it
     */
    public record Result(@NotNull BedrockGeometry geometry, @NotNull BlockDefinition.MaterialInstances instances) {}

    /**
     * @param identifier       geometry identifier, without the {@code geometry.} prefix; must contain no colon
     * @param model            the resolved Java model, which must have elements
     * @param shortnameOf      maps a Java texture variable ({@code "end"}) to its {@code terrain_texture.json}
     *                         shortname; may return {@code null} when it cannot be resolved
     * @param renderMethod     one render method for every instance, as the format requires
     * @param blockName        used only in log messages
     * @return the geometry and its materials, or {@code null} when this model cannot be represented — the caller
     *         should fall back to a full block
     */
    @Nullable
    public static Result build(@NotNull String identifier, @NotNull JavaBlockModel model,
                               @NotNull Function<String, String> shortnameOf, @NotNull String renderMethod,
                               @NotNull String blockName, int rotX, int rotY) {
        if (model.elements().isEmpty()) return null;
        if (!withinLimits(model, blockName)) return null;

        Set<String> instanceNames = new LinkedHashSet<>();
        BedrockGeometry geometry = new GeometryMapper()
                .mapRotatedBlockGeometry(identifier, model, instanceNames, rotX, rotY);
        if (geometry.hasNoCubes()) return null;

        for (var entry : model.display().entrySet()) {
            geometry.withDisplay(entry.getKey(), entry.getValue());
        }

        BlockDefinition.MaterialInstances.Builder instances = new BlockDefinition.MaterialInstances.Builder();
        boolean any = false;
        for (String name : instanceNames) {
            String shortname = shortnameOf.apply(name);
            if (shortname == null) continue;
            // Face dimming and ambient occlusion on, matching a vanilla non-light-emitting block.
            instances.withInstance(name, shortname, renderMethod, true, true);
            any = true;
        }
        if (!any) return null;

        // "*" catches any face the model left without an instance, so a missing one cannot render untextured.
        String fallback = firstResolvable(instanceNames, shortnameOf);
        if (fallback != null) instances.withInstance("*", fallback, renderMethod, true, true);

        return new Result(geometry, instances.build());
    }

    /** A geometry identifier for a block. Colons are invalid in the format, so the namespace separator goes. */
    @NotNull
    public static String identifierFor(@NotNull String bedrockBlockName) {
        return "blocks." + bedrockBlockName.toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replaceAll("[^a-z0-9_.]", "_");
    }

    @Nullable
    private static String firstResolvable(Set<String> names, Function<String, String> shortnameOf) {
        for (String name : names) {
            String shortname = shortnameOf.apply(name);
            if (shortname != null) return shortname;
        }
        return null;
    }

    /**
     * Bedrock rejects a block model larger than 30 units on an axis, or one that does not reach into the block's
     * own 16-unit cube. Checking here means a model that would be refused falls back to a full block with a
     * readable reason, instead of producing a pack the client silently drops.
     */
    private static boolean withinLimits(JavaBlockModel model, String blockName) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (JavaBlockModel.Element element : model.elements()) {
            minX = Math.min(minX, Math.min(element.fromX(), element.toX()));
            minY = Math.min(minY, Math.min(element.fromY(), element.toY()));
            minZ = Math.min(minZ, Math.min(element.fromZ(), element.toZ()));
            maxX = Math.max(maxX, Math.max(element.fromX(), element.toX()));
            maxY = Math.max(maxY, Math.max(element.fromY(), element.toY()));
            maxZ = Math.max(maxZ, Math.max(element.fromZ(), element.toZ()));
        }

        if (maxX - minX > MAX_SIZE || maxY - minY > MAX_SIZE || maxZ - minZ > MAX_SIZE) {
            Logger.warn("Block " + blockName + " spans " + (maxX - minX) + "x" + (maxY - minY) + "x" + (maxZ - minZ)
                    + " units, past Bedrock's " + (int) MAX_SIZE + "-unit block model limit"
                    + " - falling back to a full block");
            return false;
        }

        // The model has to reach into its own block, but not by any particular amount: a pressed pressure plate is
        // half a unit tall and perfectly legal. Demanding a whole unit of overlap rejected it and left a solid cube
        // underfoot.
        if (overlapWithUnit(minX, maxX) <= 0
                || overlapWithUnit(minY, maxY) <= 0
                || overlapWithUnit(minZ, maxZ) <= 0) {
            Logger.warn("Block " + blockName + " sits entirely outside its own block space on at least one axis,"
                    + " which Bedrock does not allow - falling back to a full block");
            return false;
        }

        return true;
    }

    private static float overlapWithUnit(float min, float max) {
        return Math.min(max, BLOCK_UNIT) - Math.max(min, 0.0F);
    }

    /**
     * The boxes for a model whose rotation has been baked into its cubes.
     * <p>
     * Measured from the rotated model rather than from rotated boxes, so the box cannot disagree with the geometry:
     * one rotation, done once, in {@link GeometryMapper#rotateModel}. Left unrotated, a trapdoor's hitbox stayed flat
     * on the floor while the open door stood upright.
     *
     * @param rotX degrees about X from the blockstate variant
     * @param rotY degrees about Y from the blockstate variant
     */
    @NotNull
    public static Boxes boxesFor(@NotNull JavaBlockModel model, int rotX, int rotY) {
        return boxesFor(GeometryMapper.rotateModel(model, rotX, rotY));
    }

    /**
     * The collision and selection boxes matching a model's own bounds.
     * <p>
     * Geyser infers both from the <b>host</b> Java block when they are absent, which is the wrong shape whenever the
     * pack's model is not that block: an anvil-shaped model on {@code minecraft:anvil} keeps the vanilla anvil's box,
     * and a stair keeps a full cube. Both are expressed in the same block space as the geometry — x and z centred on
     * the block, y from its floor — and clamped to the ranges Geyser documents so an oversized model still yields a
     * legal box.
     */
    @NotNull
    public static Boxes boxesFor(@NotNull JavaBlockModel model) {
        // One collision box per element, not one around all of them. A stair's slab and step union to the whole
        // cube, so a single box gave every stair a full-block hitbox — which is what was reported.
        List<BlockDefinition.Box> collision = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (JavaBlockModel.Element element : model.elements()) {
            // Mirrored along X to follow the geometry. Block geometry is emitted mirrored, because Bedrock reads it
            // that way round, and a box left in Java's coordinates then sits on the opposite side of the block from
            // the shape it is meant to wrap — a stair you could walk through on one half and bump into thin air on
            // the other. Java's model space is 0..16, so mirroring is 16 minus the coordinate, and the min/max swap
            // because negating reverses their order.
            float ex0 = JavaBlockModel.UV_SPACE - Math.max(element.fromX(), element.toX());
            float ex1 = JavaBlockModel.UV_SPACE - Math.min(element.fromX(), element.toX());
            float ey0 = Math.min(element.fromY(), element.toY());
            float ez0 = Math.min(element.fromZ(), element.toZ());
            float ey1 = Math.max(element.fromY(), element.toY());
            float ez1 = Math.max(element.fromZ(), element.toZ());

            // A zero-thickness element — a plant's sheet — has nothing to stand on.
            if (ex1 > ex0 && ey1 > ey0 && ez1 > ez0) {
                collision.add(box(ex0, ey0, ez0, ex1, ey1, ez1, MAX_COLLISION_HEIGHT));
            }

            minX = Math.min(minX, ex0); minY = Math.min(minY, ey0); minZ = Math.min(minZ, ez0);
            maxX = Math.max(maxX, ex1); maxY = Math.max(maxY, ey1); maxZ = Math.max(maxZ, ez1);
        }

        // The outline is a single box in Geyser's schema, so it stays the union.
        BlockDefinition.Box selection = box(minX, minY, minZ, maxX, maxY, maxZ, BLOCK_UNIT);
        return new Boxes(collision, selection);
    }

    /**
     * @param collision one box per solid element, so a shape made of several parts collides as those parts
     * @param selection the union, because Geyser's {@code selection_box} takes only one
     */
    public record Boxes(@NotNull List<BlockDefinition.Box> collision, @NotNull BlockDefinition.Box selection) {}

    /** @param maxHeight the top of the legal range on Y — 24 for a collision box, 16 for a selection box */
    private static BlockDefinition.Box box(float minX, float minY, float minZ,
                                           float maxX, float maxY, float maxZ, float maxHeight) {
        // x and z are centred on the block, matching the geometry; y already starts at the block floor.
        float originX = clamp(minX - 8.0F, -8.0F, 8.0F);
        float originY = clamp(minY, 0.0F, maxHeight);
        float originZ = clamp(minZ - 8.0F, -8.0F, 8.0F);

        float sizeX = clamp(maxX - minX, 0.0F, 8.0F - originX);
        float sizeY = clamp(maxY - minY, 0.0F, maxHeight - originY);
        float sizeZ = clamp(maxZ - minZ, 0.0F, 8.0F - originZ);

        return new BlockDefinition.Box(originX, originY, originZ, sizeX, sizeY, sizeZ);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
