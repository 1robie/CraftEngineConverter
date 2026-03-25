package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.BlockEntityConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.ItemDisplayRenderer;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import org.jetbrains.annotations.NotNull;

public class FenceBlockState extends AbstractDefaultBlockState {
    public FenceBlockState(
            @NotNull String baseBlock,
            @NotNull String fencePostItem,
            @NotNull String fenceSideItem,
            @NotNull ModelConfiguration model
    ) {
        BooleanBlockStateProperty northProperty = new BooleanBlockStateProperty("north", false);
        BooleanBlockStateProperty eastProperty = new BooleanBlockStateProperty("east", false);
        BooleanBlockStateProperty southProperty = new BooleanBlockStateProperty("south", false);
        BooleanBlockStateProperty westProperty = new BooleanBlockStateProperty("west", false);
        BooleanBlockStateProperty waterloggedProperty = this.addWaterloggedProperty();

        this.addProperty(northProperty);
        this.addProperty(eastProperty);
        this.addProperty(southProperty);
        this.addProperty(westProperty);

        for (boolean north : new boolean[]{false, true}) {
            for (boolean east : new boolean[]{false, true}) {
                for (boolean south : new boolean[]{false, true}) {
                    for (boolean west : new boolean[]{false, true}) {
                        for (boolean waterlogged : new boolean[]{false, true}) {
                            String appearanceName = String.format("east=%s,north=%s,south=%s,waterlogged=%s,west=%s",
                                    east, north, south, waterlogged, west);

                            String state = String.format("%s[east=%s,north=%s,south=%s,waterlogged=%s,west=%s]",
                                    baseBlock, east, north, south, waterlogged, west);

                            BlockEntityConfiguration blockEntity = new BlockEntityConfiguration();

                            // Post
                            blockEntity.addEntityRenderer(new ItemDisplayRenderer()
                                    .setItem(fencePostItem)
                                    .setRotation("180")
                                    .setScale("1.0003")
                                    .setTranslation("0,0.0001,0"));

                            // Sides
                            if (north) {
                                blockEntity.addEntityRenderer(new ItemDisplayRenderer()
                                        .setItem(fenceSideItem)
                                        .setRotation("180"));
                            }
                            if (east) {
                                blockEntity.addEntityRenderer(new ItemDisplayRenderer()
                                        .setItem(fenceSideItem)
                                        .setRotation("270"));
                            }
                            if (south) {
                                blockEntity.addEntityRenderer(new ItemDisplayRenderer()
                                        .setItem(fenceSideItem)
                                        .setRotation("0"));
                            }
                            if (west) {
                                blockEntity.addEntityRenderer(new ItemDisplayRenderer()
                                        .setItem(fenceSideItem)
                                        .setRotation("90"));
                            }

                            this.addAppearanceWithEntity(appearanceName, state, model, blockEntity);

                            BlockVariant variant = new BlockVariant(appearanceName)
                                    .addVariantCondition(northProperty, north)
                                    .addVariantCondition(eastProperty, east)
                                    .addVariantCondition(southProperty, south)
                                    .addVariantCondition(westProperty, west)
                                    .addVariantCondition(waterloggedProperty, waterlogged);

                            if (waterlogged) {
                                this.applyWaterloggedSettings(variant);
                            }

                            this.addVariant(variant);
                        }
                    }
                }
            }
        }
    }
}
