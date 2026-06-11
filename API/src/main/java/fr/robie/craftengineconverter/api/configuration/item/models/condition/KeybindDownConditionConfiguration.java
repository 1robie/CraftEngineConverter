package fr.robie.craftengineconverter.api.configuration.item.models.condition;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public class KeybindDownConditionConfiguration extends ConditionModelConfiguration {
    private final String keybind;

    public KeybindDownConditionConfiguration(@NotNull String keybind) {
        super("minecraft:keybind_down");
        this.keybind = keybind;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("keybind", this.keybind);
    }
}
