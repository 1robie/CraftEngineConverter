package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.BooleanBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.IntBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import org.jetbrains.annotations.NotNull;

public class LeavesBlockState extends AbstractDefaultBlockState {
    public LeavesBlockState(@NotNull Plugins plugin, @NotNull String itemId, @NotNull ModelConfiguration defaultModel, @NotNull ModelConfiguration waterloggedModel) {
        BooleanBlockStateProperty waterloggedProperty = this.addWaterloggedProperty();
        BooleanBlockStateProperty persistentProperty = new BooleanBlockStateProperty("persistent", true);
        IntBlockStateProperty distanceProperty = new IntBlockStateProperty("distance", 7, 1, 7);

        this.addProperty(persistentProperty);
        this.addProperty(distanceProperty);

        this.addAppearance("default", BlockAppearance.autoState(plugin, CraftEngineBlockState.LEAVES, itemId, defaultModel).build());
        this.addAppearance("waterlogged", BlockAppearance.autoState(plugin, CraftEngineBlockState.WATERLOGGED_LEAVES, itemId, waterloggedModel).build());

        this.addVariant(new BlockVariant("default").addVariantCondition(waterloggedProperty, false));

        BlockVariant waterloggedVariant = new BlockVariant("waterlogged");
        waterloggedVariant.addVariantCondition(waterloggedProperty, true);
        this.applyWaterloggedSettings(waterloggedVariant);
        this.addVariant(waterloggedVariant);

        BlockVariant decayVariant = new BlockVariant(null);
        decayVariant.addVariantCondition(distanceProperty, 7);
        decayVariant.addVariantCondition(persistentProperty, false);
        decayVariant.getBlockSettings().setRandomlyTicking(true);
        this.addVariant(decayVariant);
    }

    public LeavesBlockState(@NotNull String texturePath, @NotNull String modelPath, @NotNull String itemId, @NotNull Plugins plugin) {
        this(plugin, itemId, buildDefaultModel(modelPath, texturePath), buildWaterloggedModel(modelPath));
    }

    private static SimpleModelConfiguration buildDefaultModel(String modelPath, String texturePath) {
        SimpleModelConfiguration model = new SimpleModelConfiguration(modelPath);
        model.setGeneration(new GenerationConfiguration("minecraft:block/leaves", "all", texturePath));
        return model;
    }

    private static SimpleModelConfiguration buildWaterloggedModel(String modelPath) {
        return new SimpleModelConfiguration(modelPath);
    }
}
