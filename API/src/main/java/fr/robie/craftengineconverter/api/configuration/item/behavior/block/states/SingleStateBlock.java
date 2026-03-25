package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class SingleStateBlock implements StateBlock {
    private final BlockAppearance blockAppearance;

    public SingleStateBlock(@NotNull Plugins requiredPlugin, @NotNull CraftEngineBlockState autoState, @NotNull String itemId, @NotNull ModelConfiguration model) {
        this.blockAppearance = BlockAppearance.autoState(requiredPlugin, autoState, itemId, model).build();
    }

    public SingleStateBlock(String visualState, @NotNull ModelConfiguration model) {
        this.blockAppearance = BlockAppearance.visualState(visualState, model).build();
    }

    @Override
    public void serialize(@NotNull ConfigurationSection blockBehaviorSection) {
        ConfigurationSection state = this.getOrCreateSection(blockBehaviorSection, "state");
        this.blockAppearance.serialize(state);
    }
}
