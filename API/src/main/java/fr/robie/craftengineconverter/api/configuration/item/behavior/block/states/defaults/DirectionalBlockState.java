package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.DirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class DirectionalBlockState extends AbstractDefaultBlockState {
    public DirectionalBlockState(
            @NotNull Plugins plugin,
            @NotNull String itemId,
            @NotNull CraftEngineBlockState blockState,
            @NotNull ModelConfiguration model
    ) {
        DirectionBlockStateProperty facingProperty = new DirectionBlockStateProperty("facing", Direction.NORTH);
        this.addProperty(facingProperty);

        for (Direction facing : Direction.values()) {
            String facingName = facing.name().toLowerCase();

            int x = 0;
            int y = 0;

            switch (facing) {
                case DOWN -> x = 90;
                case UP -> x = 270;
                case SOUTH -> y = 180;
                case WEST -> y = 270;
                case EAST -> y = 90;
                default -> {
                } // NORTH
            }

            final int finalX = x;
            final int finalY = y;

            this.addAppearance(facingName, BlockAppearance.autoState(plugin, blockState, itemId, model).postProcessor(section -> {
                if (finalX != 0 || finalY != 0) {
                    ConfigurationSection modelSection = this.getOrCreateSection(section, "model");
                    if (finalX != 0) {
                        modelSection.set("x", finalX);
                    }
                    if (finalY != 0) {
                        modelSection.set("y", finalY);
                    }
                }
            }).build());

            this.addVariant(new BlockVariant(facingName)
                    .addVariantCondition(facingProperty, facing));
        }
    }

    public DirectionalBlockState(
            @NotNull Plugins plugin,
            @NotNull String itemId,
            @NotNull CraftEngineBlockState northBlockState,
            @NotNull ModelConfiguration northModel,
            @NotNull CraftEngineBlockState eastBlockState,
            @NotNull ModelConfiguration eastModel,
            @NotNull CraftEngineBlockState southBlockState,
            @NotNull ModelConfiguration southModel,
            @NotNull CraftEngineBlockState westBlockState,
            @NotNull ModelConfiguration westModel,
            @NotNull CraftEngineBlockState upBlockState,
            @NotNull ModelConfiguration upModel,
            @NotNull CraftEngineBlockState downBlockState,
            @NotNull ModelConfiguration downModel
    ) {
        DirectionBlockStateProperty facingProperty = new DirectionBlockStateProperty("facing", Direction.NORTH);
        this.addProperty(facingProperty);

        this.addAppearance("north", BlockAppearance.autoState(plugin, northBlockState, itemId, northModel).build());
        this.addAppearance("east", BlockAppearance.autoState(plugin, eastBlockState, itemId, eastModel).build());
        this.addAppearance("south", BlockAppearance.autoState(plugin, southBlockState, itemId, southModel).build());
        this.addAppearance("west", BlockAppearance.autoState(plugin, westBlockState, itemId, westModel).build());
        this.addAppearance("up", BlockAppearance.autoState(plugin, upBlockState, itemId, upModel).build());
        this.addAppearance("down", BlockAppearance.autoState(plugin, downBlockState, itemId, downModel).build());

        this.addVariant(new BlockVariant("north").addVariantCondition(facingProperty, Direction.NORTH));
        this.addVariant(new BlockVariant("east").addVariantCondition(facingProperty, Direction.EAST));
        this.addVariant(new BlockVariant("south").addVariantCondition(facingProperty, Direction.SOUTH));
        this.addVariant(new BlockVariant("west").addVariantCondition(facingProperty, Direction.WEST));
        this.addVariant(new BlockVariant("up").addVariantCondition(facingProperty, Direction.UP));
        this.addVariant(new BlockVariant("down").addVariantCondition(facingProperty, Direction.DOWN));
    }
}
