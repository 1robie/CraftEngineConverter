package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.SingleBlockHalfBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.utils.HorizontalDirection;
import net.momirealms.craftengine.core.block.property.type.SingleBlockHalf;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class TrapdoorBlockState extends AbstractDefaultBlockState {
    public TrapdoorBlockState(@NotNull String baseBlock, @NotNull ModelConfiguration modelBottom, @NotNull ModelConfiguration modelTop, @NotNull ModelConfiguration modelOpen) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);
        SingleBlockHalfBlockStateProperty halfProperty = new SingleBlockHalfBlockStateProperty("half", SingleBlockHalf.BOTTOM);
        BooleanBlockStateProperty openProperty = new BooleanBlockStateProperty("open", false);
        BooleanBlockStateProperty waterloggedProperty = this.addWaterloggedProperty();
        BooleanBlockStateProperty poweredProperty = new BooleanBlockStateProperty("powered", false);

        this.addProperty(facingProperty);
        this.addProperty(halfProperty);
        this.addProperty(openProperty);
        this.addProperty(poweredProperty);

        for (HorizontalDirection facing : HorizontalDirection.values()) {
            for (SingleBlockHalf half : SingleBlockHalf.values()) {
                for (boolean open : new boolean[]{false, true}) {
                    for (boolean waterlogged : new boolean[]{false, true}) {
                        String appearanceName = String.format("facing=%s,half=%s,open=%s,waterlogged=%s",
                                facing.name().toLowerCase(Locale.ROOT),
                                half.name().toLowerCase(Locale.ROOT),
                                open,
                                waterlogged);

                        String state = String.format("%s[facing=%s,half=%s,open=%s,powered=true,waterlogged=%s]",
                                baseBlock,
                                facing.name().toLowerCase(Locale.ROOT),
                                half.name().toLowerCase(Locale.ROOT),
                                open,
                                waterlogged);

                        ModelConfiguration model = open ? modelOpen : (half == SingleBlockHalf.BOTTOM ? modelBottom : modelTop);

                        int x = (half == SingleBlockHalf.TOP && open) ? 180 : 0;
                        int y = switch (facing) {
                            case EAST -> 90;
                            case SOUTH -> 180;
                            case WEST -> 270;
                            default -> 0;
                        };

                        if (half == SingleBlockHalf.TOP && open) {
                            y = (y + 180) % 360;
                        }

                        this.addRotatedAppearance(appearanceName, state, model, x, y, false);

                        BlockVariant variant = new BlockVariant(appearanceName)
                                .addVariantCondition(facingProperty, facing)
                                .addVariantCondition(halfProperty, half)
                                .addVariantCondition(openProperty, open)
                                .addVariantCondition(waterloggedProperty, waterlogged);

                        if (waterlogged) {
                            if (facing == HorizontalDirection.WEST && half == SingleBlockHalf.TOP && open) {
                                variant.getBlockSettings().setFluidState("water");
                            } else {
                                this.applyWaterloggedSettings(variant);
                            }
                        }
                        this.addVariant(variant);
                    }
                }
            }
        }
    }
}
