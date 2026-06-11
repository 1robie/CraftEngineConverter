package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public interface StateBlock extends SectionProvider {
    void serialize(@NotNull ConfigurationSection blockBehaviorSection);
}
