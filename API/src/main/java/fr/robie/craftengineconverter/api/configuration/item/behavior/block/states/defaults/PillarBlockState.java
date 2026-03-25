package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.defaults;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockAppearance;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.BlockVariant;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.MultiStateBlock;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.properties.AxisBlockStateProperty;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.GenerationConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import net.momirealms.craftengine.core.util.Direction;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class PillarBlockState extends MultiStateBlock {
    public PillarBlockState(@NotNull Plugins plugin, @NotNull String itemId, @NotNull CraftEngineBlockState axisYBlockStateType, @NotNull ModelConfiguration axisYModel, @NotNull CraftEngineBlockState axisXBlockStateType, @NotNull ModelConfiguration axisXModel, @NotNull CraftEngineBlockState axisZBlockStateType, @NotNull ModelConfiguration axisZModel) {
        AxisBlockStateProperty axisProperty = new AxisBlockStateProperty("axis", Direction.Axis.Y);
        this.addProperty(axisProperty);

        this.addAppearance("axisX", BlockAppearance.autoState(plugin, axisXBlockStateType, itemId, axisXModel).postProcessor(section -> {
            ConfigurationSection model = this.getOrCreateSection(section, "model");
            model.set("x", 90);
            model.set("y", 90);
        }).build());
        this.addAppearance("axisY", BlockAppearance.autoState(plugin, axisYBlockStateType, itemId, axisYModel).build());
        this.addAppearance("axisZ", BlockAppearance.autoState(plugin, axisZBlockStateType, itemId, axisZModel).postProcessor(section -> {
            ConfigurationSection model = this.getOrCreateSection(section, "model");
            model.set("x", 90);
        }).build());

        this.addVariant(new BlockVariant("axisX").addVariantCondition(axisProperty, Direction.Axis.X));
        this.addVariant(new BlockVariant("axisY").addVariantCondition(axisProperty, Direction.Axis.Y));
        this.addVariant(new BlockVariant("axisZ").addVariantCondition(axisProperty, Direction.Axis.Z));
    }

    public PillarBlockState(@NotNull String textureTopPath, @NotNull String textureSidePath, @NotNull String modelVerticalPath, @NotNull String modelHorizontalPath, @NotNull String itemId, @NotNull Plugins plugin) {
        this(plugin, itemId, CraftEngineBlockState.SOLID, buildModel(modelVerticalPath, textureTopPath, textureSidePath), CraftEngineBlockState.SOLID, buildModel(modelHorizontalPath, textureTopPath, textureSidePath), CraftEngineBlockState.SOLID, buildModel(modelHorizontalPath, textureTopPath, textureSidePath));
    }

    private static SimpleModelConfiguration buildModel(String modelPath, String textureTopPath, String textureSidePath) {
        SimpleModelConfiguration model = new SimpleModelConfiguration(modelPath);
        model.setGeneration(new GenerationConfiguration("minecraft:block/cube_column", "end", textureTopPath, "side", textureSidePath));
        return model;
    }
}
