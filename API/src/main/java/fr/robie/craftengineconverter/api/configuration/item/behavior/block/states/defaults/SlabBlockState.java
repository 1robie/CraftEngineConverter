package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.SlabTypeBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import net.momirealms.craftengine.core.block.property.type.SlabType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SlabBlockState extends AbstractDefaultBlockState {
    public SlabBlockState(
            @NotNull String baseBlock,
            @NotNull ModelConfiguration modelTop,
            @NotNull ModelConfiguration modelBottom,
            @NotNull ModelConfiguration modelDouble
    ) {
        SlabTypeBlockStateProperty typeProperty = new SlabTypeBlockStateProperty("type", SlabType.BOTTOM);
        BooleanBlockStateProperty waterloggedProperty = this.addWaterloggedProperty();

        this.addProperty(typeProperty);

        for (SlabType type : SlabType.values()) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                String typeName = type.name().toLowerCase(Locale.ROOT);
                String appearanceName = String.format("type=%s,waterlogged=%s", typeName, waterlogged);
                String state = String.format("%s[type=%s,waterlogged=%s]", baseBlock, typeName, waterlogged);

                ModelConfiguration model = switch (type) {
                    case TOP -> modelTop;
                    case BOTTOM -> modelBottom;
                    case DOUBLE -> modelDouble;
                };

                this.addAppearance(appearanceName, this.buildAppearance(state, model));

                BlockVariant variant = new BlockVariant(appearanceName)
                        .addVariantCondition(typeProperty, type)
                        .addVariantCondition(waterloggedProperty, waterlogged);

                if (waterlogged) {
                    this.applyWaterloggedSettings(variant);
                }
                this.addVariant(variant);
            }
        }
    }

    private fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance buildAppearance(String state, ModelConfiguration model) {
        return fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance.visualState(state, model).build();
    }
}
