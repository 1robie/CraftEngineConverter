package fr.robie.craftengineconverter.api.configuration.loader.models.special;

import fr.robie.craftengineconverter.api.annotations.AutoModelConfigurationLoader;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.models.model.SimpleModelConfiguration;
import fr.robie.craftengineconverter.api.configuration.loader.models.ModelConfigurationLoader;
import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reduces a {@code minecraft:special} node to the ordinary model underneath it.
 *
 * <h2>What special is</h2>
 * Java uses it where the client draws something no model file can describe — a shield's banner patterns, a
 * trident's spin, a conduit, a decorated pot:
 * <pre>
 * {"type":"minecraft:special","base":"minecraft:item/shield_blocking","model":{"type":"minecraft:shield"}}
 * </pre>
 * The {@code model} names a hard-coded renderer. The {@code base} is a real model file, and it is what carries the
 * display transforms the client poses that renderer with.
 *
 * <h2>Why the base is worth taking</h2>
 * The node used to be dropped outright, which took the whole branch with it — so a vanilla-shaped shield or
 * trident converted to nothing at all. Keeping the base recovers the poses, and for a custom pack it usually
 * recovers everything: an author who wants a custom shield replaces the renderer with real geometry anyway, and
 * a pack that leaves the vanilla base in place gets an extruded sprite in the right position rather than a hole.
 * <p>
 * The renderer itself is not reproducible on Bedrock under any circumstances, which is what the warning says.
 */
@AutoModelConfigurationLoader({"special", "minecraft:special"})
public class SpecialModelConfigurationLoader implements ModelConfigurationLoader<ModelConfiguration> {

    @Override
    public @Nullable ModelConfiguration load(@NotNull ConfigurationSection section) {
        String base = section.getString("base");
        if (base == null) {
            Logger.debug("A 'special' model names no base, so there is nothing to fall back to - skipping");
            return null;
        }

        ConfigurationSection renderer = section.getConfigurationSection("model");
        String rendererType = renderer == null ? "unknown" : renderer.getString("type", "unknown");
        Logger.debug("Using '" + base + "' in place of the '" + rendererType + "' special renderer,"
                + " which Bedrock has no equivalent for");

        return new SimpleModelConfiguration(base);
    }
}
