package fr.robie.craftengineconverter.api.configuration.conditions;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PermissionCondition extends AbstractCondition {
    private final String permission;

    public PermissionCondition(@NotNull String permission) {
        super("permission");
        this.permission = Objects.requireNonNull(permission, "permission cannot be null");
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        super.serialize(section);
        section.set("permission", this.permission);
    }
}
