package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class HorizontalFacingBlockState extends AbstractDefaultBlockState {
    public HorizontalFacingBlockState(
            @NotNull Plugins plugin,
            @NotNull String itemId,
            @NotNull CraftEngineBlockState blockState,
            @NotNull ModelConfiguration model
    ) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);
        this.addProperty(facingProperty);

        for (HorizontalDirection facing : HorizontalDirection.values()) {
            String facingName = facing.name().toLowerCase();

            int y = switch (facing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0; // NORTH
            };

            this.addAppearance(facingName, BlockAppearance.autoState(plugin, blockState, itemId, model).postProcessor(section -> {
                if (y != 0) {
                    ConfigurationSection modelSection = getOrCreateSection(section, "model");
                    modelSection.set("y", y);
                }
            }).build());

            this.addVariant(new BlockVariant(facingName)
                    .addVariantCondition(facingProperty, facing));
        }
    }
}
