package fr.robie.craftengineconverter.api.configuration.conditions;

import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class WorldGuardRegionCondition extends AbstractCondition {
    private final int mode;
    private final List<String> regions;

    public WorldGuardRegionCondition(int mode, @NotNull List<String> regions) {
        super("worldguard:region");
        this.mode = mode;
        this.regions = Objects.requireNonNull(regions, "regions cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("mode", this.mode);
        section.set("regions", this.regions);
    }
}
