package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.DoubleBlockHalfBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HingeBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import net.momirealms.craftengine.core.block.properties.type.DoubleBlockHalf;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import org.bukkit.block.data.type.Door;
import org.jetbrains.annotations.NotNull;

public class DoorBlockState extends AbstractDefaultBlockState {
    public DoorBlockState(
            @NotNull String baseBlock,
            @NotNull ModelConfiguration bottomLeft,
            @NotNull ModelConfiguration bottomLeftOpen,
            @NotNull ModelConfiguration bottomRight,
            @NotNull ModelConfiguration bottomRightOpen,
            @NotNull ModelConfiguration topLeft,
            @NotNull ModelConfiguration topLeftOpen,
            @NotNull ModelConfiguration topRight,
            @NotNull ModelConfiguration topRightOpen
    ) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);
        DoubleBlockHalfBlockStateProperty halfProperty = new DoubleBlockHalfBlockStateProperty("half", DoubleBlockHalf.LOWER);
        HingeBlockStateProperty hingeProperty = new HingeBlockStateProperty("hinge", Door.Hinge.LEFT);
        BooleanBlockStateProperty openProperty = new BooleanBlockStateProperty("open", false);
        BooleanBlockStateProperty poweredProperty = new BooleanBlockStateProperty("powered", false);

        this.addProperty(facingProperty);
        this.addProperty(halfProperty);
        this.addProperty(hingeProperty);
        this.addProperty(openProperty);
        this.addProperty(poweredProperty);

        for (HorizontalDirection facing : HorizontalDirection.values()) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                for (Door.Hinge hinge : Door.Hinge.values()) {
                    for (boolean open : new boolean[]{false, true}) {
                        String appearanceName = String.format("facing=%s,half=%s,hinge=%s,open=%s",
                                facing.name().toLowerCase(),
                                half.name().toLowerCase(),
                                hinge.name().toLowerCase(),
                                open);

                        String state = String.format("%s[facing=%s,half=%s,hinge=%s,open=%s,powered=true]",
                                baseBlock,
                                facing.name().toLowerCase(),
                                half.name().toLowerCase(),
                                hinge.name().toLowerCase(),
                                open);

                        ModelConfiguration model;
                        if (half == DoubleBlockHalf.LOWER) {
                            if (hinge == Door.Hinge.LEFT) {
                                model = open ? bottomLeftOpen : bottomLeft;
                            } else {
                                model = open ? bottomRightOpen : bottomRight;
                            }
                        } else {
                            if (hinge == Door.Hinge.LEFT) {
                                model = open ? topLeftOpen : topLeft;
                            } else {
                                model = open ? topRightOpen : topRight;
                            }
                        }

                        int baseRotation = switch (facing) {
                            case NORTH -> 270;
                            case SOUTH -> 90;
                            case WEST -> 180;
                            default -> 0; // EAST
                        };

                        int finalY;
                        if (open) {
                            if (hinge == Door.Hinge.LEFT) {
                                finalY = (baseRotation + 90) % 360;
                            } else {
                                finalY = (baseRotation + 270) % 360;
                            }
                        } else {
                            finalY = baseRotation;
                        }

                        this.addRotatedAppearance(appearanceName, state, model, 0, finalY, false);

                        this.addVariant(new BlockVariant(appearanceName)
                                .addVariantCondition(facingProperty, facing)
                                .addVariantCondition(halfProperty, half)
                                .addVariantCondition(hingeProperty, hinge)
                                .addVariantCondition(openProperty, open));
                    }
                }
            }
        }
    }
}
