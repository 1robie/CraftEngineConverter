package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.SingleBlockHalfBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.StairsShapeBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import net.momirealms.craftengine.core.block.properties.type.SingleBlockHalf;
import net.momirealms.craftengine.core.block.properties.type.StairsShape;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import org.jetbrains.annotations.NotNull;

public class StairsBlockState extends AbstractDefaultBlockState {
    public StairsBlockState(
            @NotNull String baseBlock,
            @NotNull ModelConfiguration modelStairs,
            @NotNull ModelConfiguration modelInner,
            @NotNull ModelConfiguration modelOuter
    ) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);
        SingleBlockHalfBlockStateProperty halfProperty = new SingleBlockHalfBlockStateProperty("half", SingleBlockHalf.BOTTOM);
        StairsShapeBlockStateProperty shapeProperty = new StairsShapeBlockStateProperty("shape", StairsShape.STRAIGHT);
        BooleanBlockStateProperty waterloggedProperty = this.addWaterloggedProperty();

        this.addProperty(facingProperty);
        this.addProperty(halfProperty);
        this.addProperty(shapeProperty);

        for (HorizontalDirection facing : HorizontalDirection.values()) {
            for (SingleBlockHalf half : SingleBlockHalf.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    for (boolean waterlogged : new boolean[]{false, true}) {
                        String appearanceName = String.format("facing=%s,half=%s,shape=%s,waterlogged=%s",
                                facing.name().toLowerCase(),
                                half.name().toLowerCase(),
                                shape.name().toLowerCase(),
                                waterlogged);

                        String state = String.format("%s[facing=%s,half=%s,shape=%s,waterlogged=%s]",
                                baseBlock,
                                facing.name().toLowerCase(),
                                half.name().toLowerCase(),
                                shape.name().toLowerCase(),
                                waterlogged);

                        ModelConfiguration model = switch (shape) {
                            case INNER_LEFT, INNER_RIGHT -> modelInner;
                            case OUTER_LEFT, OUTER_RIGHT -> modelOuter;
                            default -> modelStairs;
                        };

                        int baseRotation = switch (facing) {
                            case NORTH -> 270;
                            case SOUTH -> 90;
                            case WEST -> 180;
                            default -> 0;
                        };

                        int x = (half == SingleBlockHalf.TOP) ? 180 : 0;
                        int y = baseRotation;

                        if (half == SingleBlockHalf.BOTTOM) {
                            if (shape == StairsShape.INNER_LEFT || shape == StairsShape.OUTER_LEFT) {
                                y = (y + 270) % 360;
                            }
                        } else {
                            if (shape == StairsShape.INNER_RIGHT || shape == StairsShape.OUTER_RIGHT) {
                                y = (y + 90) % 360;
                            }
                        }

                        this.addRotatedAppearance(appearanceName, state, model, x, y, true);

                        BlockVariant variant = new BlockVariant(appearanceName)
                                .addVariantCondition(facingProperty, facing)
                                .addVariantCondition(halfProperty, half)
                                .addVariantCondition(shapeProperty, shape)
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
