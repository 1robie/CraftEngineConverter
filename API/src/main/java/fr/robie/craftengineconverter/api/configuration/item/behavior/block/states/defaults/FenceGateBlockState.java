package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.utils.HorizontalDirection;
import org.jetbrains.annotations.NotNull;

public class FenceGateBlockState extends AbstractDefaultBlockState {
    public FenceGateBlockState(
            @NotNull String baseBlock,
            @NotNull ModelConfiguration modelGate,
            @NotNull ModelConfiguration modelGateOpen,
            @NotNull ModelConfiguration modelGateWall,
            @NotNull ModelConfiguration modelGateWallOpen
    ) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.SOUTH);
        BooleanBlockStateProperty inWallProperty = new BooleanBlockStateProperty("in_wall", false);
        BooleanBlockStateProperty openProperty = new BooleanBlockStateProperty("open", false);
        BooleanBlockStateProperty poweredProperty = new BooleanBlockStateProperty("powered", false);

        this.addProperty(facingProperty);
        this.addProperty(inWallProperty);
        this.addProperty(openProperty);
        this.addProperty(poweredProperty);

        for (HorizontalDirection facing : HorizontalDirection.values()) {
            for (boolean inWall : new boolean[]{false, true}) {
                for (boolean open : new boolean[]{false, true}) {
                    String appearanceName = String.format("facing=%s,in_wall=%s,open=%s",
                            facing.name().toLowerCase(),
                            inWall,
                            open);

                    String state = String.format("%s[facing=%s,in_wall=%s,open=%s,powered=true]",
                            baseBlock,
                            facing.name().toLowerCase(),
                            inWall,
                            open);

                    ModelConfiguration model;
                    if (inWall) {
                        model = open ? modelGateWallOpen : modelGateWall;
                    } else {
                        model = open ? modelGateOpen : modelGate;
                    }

                    int y = switch (facing) {
                        case EAST -> 270;
                        case NORTH -> 180;
                        case WEST -> 90;
                        default -> 0;
                    };

                    this.addRotatedAppearance(appearanceName, state, model, 0, y, true);

                    this.addVariant(new BlockVariant(appearanceName)
                            .addVariantCondition(facingProperty, facing)
                            .addVariantCondition(inWallProperty, inWall)
                            .addVariantCondition(openProperty, open)
                            .addVariantCondition(poweredProperty, false));

                    this.addVariant(new BlockVariant(appearanceName)
                            .addVariantCondition(facingProperty, facing)
                            .addVariantCondition(inWallProperty, inWall)
                            .addVariantCondition(openProperty, open)
                            .addVariantCondition(poweredProperty, true));
                }
            }
        }
    }
}
