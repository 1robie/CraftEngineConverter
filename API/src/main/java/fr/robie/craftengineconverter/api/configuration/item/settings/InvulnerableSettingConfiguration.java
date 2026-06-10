package fr.robie.craftengineconverter.api.configuration.item.settings;

import fr.robie.craftengineconverter.api.configuration.item.ItemConfigurationSerializable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InvulnerableSettingConfiguration implements ItemConfigurationSerializable {
    private final Set<InvulnerableType> invulnerableTypes;
    private final boolean mergeInvulnerableTypes;

    public InvulnerableSettingConfiguration(Set<InvulnerableType> invulnerableTypes, boolean mergeInvulnerableTypes) {
        this.invulnerableTypes = invulnerableTypes;
        this.mergeInvulnerableTypes = mergeInvulnerableTypes;
    }

    public InvulnerableSettingConfiguration(Set<InvulnerableType> invulnerableTypes) {
        this(invulnerableTypes, false);
    }

    @Override
    public void serialize(@NotNull YamlConfiguration yamlConfiguration, @NotNull String path, @NotNull ConfigurationSection itemSection, @NotNull String itemId) {
        if (this.invulnerableTypes == null || this.invulnerableTypes.isEmpty()) {
            return;
        }

        ConfigurationSection settings = this.getOrCreateSection(itemSection, "settings");
        if (this.mergeInvulnerableTypes) {
            List<String> invulnerable = settings.getStringList("invulnerable");
            for (InvulnerableType type : this.invulnerableTypes) {
                String typeName = type.name().toLowerCase(Locale.ROOT);
                if (!invulnerable.contains(typeName)) {
                    invulnerable.add(typeName);
                }
            }
            settings.set("invulnerable", invulnerable);
        } else {
            List<String> invulnerable = this.invulnerableTypes.stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList();
            settings.set("invulnerable", invulnerable);
        }
    }

    public enum InvulnerableType {
        LAVA,
        FIRE,
        FIRE_TICK,
        BLOCK_EXPLOSION,
        ENTITY_EXPLOSION,
        LIGHTNING,
        CONTACT
    }
}
