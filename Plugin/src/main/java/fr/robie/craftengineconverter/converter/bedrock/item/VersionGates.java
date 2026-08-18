package fr.robie.craftengineconverter.converter.bedrock.item;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.utils.MinecraftVersion;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * CraftEngine's {@code $$} version gates, as they reach this converter.
 *
 * <h2>What a gate looks like by the time we see it</h2>
 * An author writes one key:
 * <pre>
 * items:
 *   $$&gt;=1.21.4#topaz_trident:
 *     default:topaz_trident: { ... }
 * </pre>
 * but {@code .} is the configuration path separator, so the YAML layer splits it into nested sections —
 * {@code $$&gt;=1} → {@code 21} → {@code 4#topaz_trident} — and two gates sharing a prefix merge into one
 * tree ({@code $$&gt;=1.21.4#topaz_trident} and {@code $$&gt;=1.21.11#topaz_spear} become siblings
 * {@code 4#topaz_trident} and {@code 11#topaz_spear} under {@code $$&gt;=1/21}). Rejoining the fragments
 * with {@code .} recovers the expression the author wrote.
 * <p>
 * The part after {@code #} is only a label, the same convention {@link ConfigFactoryExpander#sectionType}
 * documents for section keys. A gate may also carry no label at all, in which case there is nothing to
 * stop the descent but the shape of the children.
 *
 * <h2>Why this matters</h2>
 * Nothing read these before, so every gated entry was taken for an item id whose body happened to
 * contain one real item — which parsed to nothing and was dropped without a word. In the sample pack
 * that silently lost the trident, the spear and the elytra.
 */
public final class VersionGates {

    private VersionGates() {
        throw new UnsupportedOperationException("VersionGates is a utility and cannot be instantiated.");
    }

    /**
     * Stands in for "newer than anything a pack will gate on".
     * <p>
     * Used when the target version is unknown, which is the normal case for the headless
     * {@code devConvert}: there is no Bukkit, so {@link MinecraftVersion#getCurrentVersion()} reports
     * {@code 0.0.0} and <i>every</i> {@code >=} gate would fail — the exact silent-drop this class exists
     * to end. Treating unknown as newest picks what a current server would pick.
     */
    private static final MinecraftVersion NEWEST = MinecraftVersion.parse("9999.0.0");

    /**
     * Whether a key is a fragment of a split gate rather than an item id.
     * <p>
     * Broader than {@link ConfigFactoryExpander}'s own predicate, which only knows {@code $$…} and bare
     * digits: the middle of a range splits to {@code 1~1} and a labelled tail to {@code 4#topaz_trident},
     * neither of which is all-digits. An item id is never any of these — CraftEngine ids are
     * {@code namespace:name}.
     */
    public static boolean isFragment(@NotNull String key) {
        if (key.startsWith("$$")) return true;
        if (key.isEmpty()) return false;

        String body = key.indexOf('#') < 0 ? key : key.substring(0, key.indexOf('#'));
        if (body.isEmpty()) return false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!Character.isDigit(c) && c != '~' && c != '.') return false;
        }
        return true;
    }

    /**
     * The Minecraft version gates are resolved against.
     * <p>
     * The running server first, since that is what the pack is actually being converted for. Failing that
     * a pinned {@code vanilla-assets.version}, which an operator sets when converting for a version other
     * than the one they are on. Failing both, {@link #NEWEST}.
     */
    @NotNull
    public static MinecraftVersion targetVersion() {
        MinecraftVersion server = MinecraftVersion.getCurrentVersion();
        if (server.getMajor() > 0) return server;

        try {
            String pinned = Configuration.get(Keys.VANILLA_ASSETS_VERSION);
            if (pinned != null && !pinned.isBlank() && !pinned.equalsIgnoreCase("auto")) {
                MinecraftVersion parsed = MinecraftVersion.parse(pinned);
                if (parsed.getMajor() > 0) return parsed;
            }
        } catch (RuntimeException ignored) {
            // No configuration loaded (a unit test, or a very early call). Fall through to NEWEST.
        }
        return NEWEST;
    }

    /**
     * Whether a gate admits the target version.
     *
     * @param gate the rejoined expression including its {@code $$} prefix and excluding any {@code #label},
     *             e.g. {@code $$>=1.21.4} or {@code $$1.20.1~1.21.3}
     */
    public static boolean accepts(@NotNull String gate, @NotNull MinecraftVersion target) {
        String expression = gate.startsWith("$$") ? gate.substring(2) : gate;
        expression = expression.trim().toLowerCase(Locale.ROOT);
        if (expression.isEmpty()) return true;

        int tilde = expression.indexOf('~');
        if (tilde >= 0) {
            MinecraftVersion low = MinecraftVersion.parse(expression.substring(0, tilde));
            MinecraftVersion high = MinecraftVersion.parse(expression.substring(tilde + 1));
            return target.isAtLeast(low) && target.isAtMost(high);
        }

        if (expression.startsWith(">=")) return target.isAtLeast(MinecraftVersion.parse(expression.substring(2)));
        if (expression.startsWith("<=")) return target.isAtMost(MinecraftVersion.parse(expression.substring(2)));
        if (expression.startsWith(">")) return target.isAfter(MinecraftVersion.parse(expression.substring(1)));
        if (expression.startsWith("<")) return target.isBefore(MinecraftVersion.parse(expression.substring(1)));

        MinecraftVersion exact = MinecraftVersion.parse(expression);
        if (exact.getMajor() == 0) {
            // Not a version at all. Better to convert the entry than to drop it over a syntax this does
            // not know - dropping is what the bug being fixed here did.
            Logger.warn("Could not read version gate '" + gate + "' - its entries are converted anyway");
            return true;
        }
        return target.equals(exact);
    }

    /** The expression part of a rejoined gate, with any {@code #label} removed. */
    @NotNull
    public static String expressionOf(@NotNull String joinedKey) {
        int hash = joinedKey.indexOf('#');
        return hash < 0 ? joinedKey : joinedKey.substring(0, hash);
    }

    /** Whether a rejoined key carries the {@code #label} that ends a gate's fragments. */
    public static boolean isLabelled(@NotNull String joinedKey) {
        return joinedKey.indexOf('#') >= 0;
    }
}
