package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.BlockEntity;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.MultiStateBlock;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDefaultBlockState extends MultiStateBlock {

    protected BooleanBlockStateProperty addWaterloggedProperty() {
        BooleanBlockStateProperty waterloggedProperty = new BooleanBlockStateProperty("waterlogged", false);
        this.addProperty(waterloggedProperty);
        return waterloggedProperty;
    }

    protected void applyWaterloggedSettings(@NotNull BlockVariant variant) {
        variant.getBlockSettings()
                .setResistance(1200.0f)
                .setBurnable(false)
                .setFluidState("water");
    }

    protected void addRotatedAppearance(@NotNull String appearanceName, @NotNull String state, @NotNull ModelConfiguration model, int x, int y, boolean uvlock) {
        this.addAppearance(appearanceName, BlockAppearance.visualState(state, model).postProcessor(section -> {
            if (x != 0 || y != 0 || uvlock) {
                ConfigurationSection modelSection = this.getOrCreateSection(section, "model");
                if (x != 0) {
                    modelSection.set("x", x);
                }
                if (y != 0) {
                    modelSection.set("y", y);
                }
                if (uvlock) {
                    modelSection.set("uvlock", true);
                }
            }
        }).build());
    }

    protected void addAppearanceWithEntity(@NotNull String appearanceName, @NotNull String state, @NotNull ModelConfiguration model, @Nullable BlockEntity blockEntity) {
        BlockAppearance.Builder builder = BlockAppearance.visualState(state, model);
        if (blockEntity != null) {
            builder.blockEntity(blockEntity);
        }
        this.addAppearance(appearanceName, builder.build());
    }
}
