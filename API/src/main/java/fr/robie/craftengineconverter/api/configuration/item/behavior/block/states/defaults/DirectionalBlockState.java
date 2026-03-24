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
}
