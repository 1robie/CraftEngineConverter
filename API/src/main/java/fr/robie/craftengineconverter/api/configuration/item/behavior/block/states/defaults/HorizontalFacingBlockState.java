package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.HorizontalDirectionBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.utils.HorizontalDirection;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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
            String facingName = facing.name().toLowerCase(Locale.ROOT);

            int y = switch (facing) {
                case SOUTH -> 180;
                case WEST -> 270;
                case EAST -> 90;
                default -> 0;
            };

            this.addAppearance(facingName, BlockAppearance.autoState(plugin, blockState, itemId, model).postProcessor(section -> {
                if (y != 0) {
                    ConfigurationSection modelSection = this.getOrCreateSection(section, "model");
                    modelSection.set("y", y);
                }
            }).build());

            this.addVariant(new BlockVariant(facingName)
                    .addVariantCondition(facingProperty, facing));
        }
    }

    public HorizontalFacingBlockState(
            @NotNull Plugins plugin,
            @NotNull String itemId,
            @NotNull CraftEngineBlockState northBlockState,
            @NotNull ModelConfiguration northModel,
            @NotNull CraftEngineBlockState eastBlockState,
            @NotNull ModelConfiguration eastModel,
            @NotNull CraftEngineBlockState southBlockState,
            @NotNull ModelConfiguration southModel,
            @NotNull CraftEngineBlockState westBlockState,
            @NotNull ModelConfiguration westModel
    ) {
        HorizontalDirectionBlockStateProperty facingProperty = new HorizontalDirectionBlockStateProperty("facing", HorizontalDirection.NORTH);
        this.addProperty(facingProperty);

        this.addAppearance("north", BlockAppearance.autoState(plugin, northBlockState, itemId, northModel).build());
        this.addAppearance("east", BlockAppearance.autoState(plugin, eastBlockState, itemId, eastModel).build());
        this.addAppearance("south", BlockAppearance.autoState(plugin, southBlockState, itemId, southModel).build());
        this.addAppearance("west", BlockAppearance.autoState(plugin, westBlockState, itemId, westModel).build());

        this.addVariant(new BlockVariant("north").addVariantCondition(facingProperty, HorizontalDirection.NORTH));
        this.addVariant(new BlockVariant("east").addVariantCondition(facingProperty, HorizontalDirection.EAST));
        this.addVariant(new BlockVariant("south").addVariantCondition(facingProperty, HorizontalDirection.SOUTH));
        this.addVariant(new BlockVariant("west").addVariantCondition(facingProperty, HorizontalDirection.WEST));
    }
}
