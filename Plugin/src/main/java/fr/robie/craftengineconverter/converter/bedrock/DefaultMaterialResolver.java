package fr.robie.craftengineconverter.converter.bedrock;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;

/**
 * Works out which Java item a CraftEngine item falls back to when it declares no {@code material:}.
 * <p>
 * CraftEngine reads this from its own config (`item.default-material`, defaulting to {@code nether_brick})
 * and applies it to every item without an explicit material — which in a typical pack is most block items
 * and anything driven by a template that sets the material elsewhere. Guessing wrong silently maps those
 * items onto the wrong Java item, so the value has to come from the same place CraftEngine got it.
 *
 * <h2>Precedence</h2>
 * <ol>
 *   <li>The converter's own {@code default-material}, when set to a real material — an explicit setting
 *       always wins.</li>
 *   <li>{@code item.default-material} from CraftEngine's {@code config.yml}, when that file can be found.</li>
 *   <li>{@code nether_brick}, CraftEngine's built-in default.</li>
 * </ol>
 * The converter's key defaults to the sentinel {@value #AUTO} rather than a material, because
 * {@code Configuration} writes its defaults back into its own file: after one run the key is always
 * present, so "did the user set this?" cannot otherwise be answered.
 */
final class DefaultMaterialResolver {
    private static final Material FALLBACK = Material.NETHER_BRICK;

    private DefaultMaterialResolver() {
        throw new UnsupportedOperationException("DefaultMaterialResolver is a utility class and cannot be instantiated.");
    }

    static Material resolve(@Nullable File pluginFolder) {
        Object configured = Configuration.get(ConfigurationKey.DEFAULT_MATERIAL);

        if (configured instanceof Material material) {
            Logger.info("Default item material: " + material.name().toLowerCase(Locale.ROOT)
                    + " (from the converter's config)");
            return material;
        }

        Material fromCraftEngine = readFromCraftEngine(pluginFolder);
        if (fromCraftEngine != null) {
            Logger.info("Default item material: " + fromCraftEngine.name().toLowerCase(Locale.ROOT)
                    + " (from CraftEngine's config.yml)");
            return fromCraftEngine;
        }

        Logger.info("Default item material: " + FALLBACK.name().toLowerCase(Locale.ROOT)
                + " (CraftEngine's built-in default; set default-material if your server differs)");
        return FALLBACK;
    }

    // CraftEngine installs alongside this plugin, so its config is a sibling: plugins/CraftEngine/config.yml
    @Nullable
    private static Material readFromCraftEngine(@Nullable File pluginFolder) {
        if (pluginFolder == null) return null;
        File pluginsDir = pluginFolder.getParentFile();
        if (pluginsDir == null) return null;

        File config = new File(pluginsDir, "CraftEngine/config.yml");
        if (!config.isFile()) return null;

        return FileCacheManager.getYamlCache().getEntryFile(config.toPath())
                .map(entry -> parse(entry.getData().getString("item.default-material")))
                .orElse(null);
    }

    @Nullable
    private static Material parse(@Nullable String name) {
        if (name == null || name.isBlank()) return null;
        // CraftEngine writes namespaced ids; only the path names a Bukkit Material.
        String value = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
        try {
            return Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Logger.warn("CraftEngine's item.default-material is '" + name + "', which is not a known material");
            return null;
        }
    }
}
