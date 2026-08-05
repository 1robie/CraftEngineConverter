package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option;

import fr.robie.messageflow.logger.Logger;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Author-declared rules assigning converted items to a creative-menu group by matching their
 * <b>CraftEngine item id</b>.
 * <p>
 * {@link VanillaItemGroups} can only ever infer a group from the item's <i>base material</i>, which is often
 * nothing useful: a custom ore built on {@code nether_brick} inherits no group at all, even though
 * {@code itemGroup.name.ore} exists and is plainly where it belongs. The pack author is the one who knows
 * their own naming convention — {@code *_ore}, {@code *_log} — so they get to state it, and matching happens
 * on the id ({@code default:topaz_ore}) rather than the material.
 * <p>
 * Rules are ordered as written in the configuration and the <b>first match wins</b>, so a narrow rule must
 * precede a broad one. A rule matches ids two ways, and may use either or both:
 * <ul>
 *   <li>{@code wildcards} — {@code *_ore}, where {@code *} is any run of characters and {@code ?} is one.
 *       Describes the whole id, with the namespace optional, as {@code blacklisted-paths} does.</li>
 *   <li>{@code patterns} — Java regex applied with {@link java.util.regex.Matcher#find()}, so {@code _ore$}
 *       and {@code .*_ore} both work.</li>
 * </ul>
 * Both are case-insensitive. They are separate keys rather than one auto-detected key because a bare
 * {@code topaz} means "contains topaz" as a regex but "is exactly topaz" as a wildcard, and guessing wrong
 * would quietly change what a rule matches.
 * <p>
 * Only vanilla group names work — a custom group lives in a behavior pack, which Geyser cannot send — so a
 * name {@link VanillaItemGroups} does not know is warned about at load time rather than silently ignored by
 * the client.
 */
public final class CreativeGroupRules {

    private static final CreativeGroupRules EMPTY = new CreativeGroupRules(List.of());

    private final List<Rule> rules;

    private CreativeGroupRules(@NotNull List<Rule> rules) {
        this.rules = rules;
    }

    public static CreativeGroupRules empty() {
        return EMPTY;
    }

    /**
     * Reads the rules from a {@code creative-groups} section. Every rule is validated here, once, so a typo
     * is reported at startup instead of producing a silently ungrouped pack; a rule that cannot be used is
     * skipped and the rest still apply.
     */
    public static CreativeGroupRules parse(@Nullable ConfigurationSection section) {
        if (section == null) return EMPTY;

        List<Rule> parsed = new ArrayList<>();
        // getKeys(false) preserves the order the keys appear in the file, which is what makes
        // "first match wins" a promise the author can rely on.
        for (String name : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(name);
            if (ruleSection == null) {
                Logger.warn("creative-groups." + name + " is not a section - ignoring it");
                continue;
            }

            String group = ruleSection.getString("group");
            if (group == null || group.isBlank()) {
                Logger.warn("creative-groups." + name + " has no 'group' - ignoring it");
                continue;
            }
            if (!VanillaItemGroups.isKnownGroup(group)) {
                Logger.warn("creative-groups." + name + " uses group '" + group + "', which is not a vanilla"
                        + " Bedrock group - the client will ignore it, since custom groups need a behavior"
                        + " pack and Geyser cannot send those");
            }

            List<String> patternStrings = ruleSection.getStringList("patterns");
            List<String> wildcardStrings = ruleSection.getStringList("wildcards");
            if (patternStrings.isEmpty() && wildcardStrings.isEmpty()) {
                Logger.warn("creative-groups." + name + " has no 'wildcards' or 'patterns' - ignoring it");
                continue;
            }

            List<Pattern> patterns = new ArrayList<>();
            for (String wildcard : wildcardStrings) {
                patterns.add(wildcardToPattern(wildcard));
            }
            for (String patternString : patternStrings) {
                try {
                    patterns.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE));
                } catch (PatternSyntaxException e) {
                    // Writing "*_ore" under 'patterns' is the natural mistake, and as a regex it is simply
                    // invalid, so say where it belongs instead of only reporting the syntax error.
                    String hint = looksLikeWildcard(patternString)
                            ? " - it looks like a wildcard, so list it under 'wildcards' instead of 'patterns'"
                            : " - skipping that pattern: " + e.getDescription();
                    Logger.warn("creative-groups." + name + " has an invalid regex '" + patternString + "'" + hint);
                }
            }
            if (patterns.isEmpty()) {
                Logger.warn("creative-groups." + name + " has no usable pattern - ignoring it");
                continue;
            }

            parsed.add(new Rule(name, group, ruleSection.getString("category"), patterns));
        }

        return parsed.isEmpty() ? EMPTY : new CreativeGroupRules(List.copyOf(parsed));
    }

    /**
     * Compiles a wildcard into the equivalent regex: {@code *} stands for any run of characters, {@code ?} for
     * exactly one, and everything else is literal.
     * <p>
     * The result is anchored, so a wildcard describes the <b>whole</b> id rather than any part of it — which is
     * what {@code *_ore} plainly means. The namespace is optional in that match, following the same convention
     * as {@code blacklisted-paths}: {@code topaz_*} matches {@code default:topaz_ore} without the author
     * having to spell out {@code *:topaz_*}.
     * <p>
     * A wildcard can never fail to compile, which is the main reason to keep it a separate key from
     * {@code patterns} rather than guessing which of the two an author meant.
     */
    static Pattern wildcardToPattern(@NotNull String wildcard) {
        StringBuilder regex = new StringBuilder("^(?:[^:]*:)?");
        StringBuilder literal = new StringBuilder();
        for (char c : wildcard.toCharArray()) {
            if (c != '*' && c != '?') {
                literal.append(c);
                continue;
            }
            if (!literal.isEmpty()) {
                regex.append(Pattern.quote(literal.toString()));
                literal.setLength(0);
            }
            regex.append(c == '*' ? ".*" : ".");
        }
        if (!literal.isEmpty()) regex.append(Pattern.quote(literal.toString()));

        return Pattern.compile(regex.append('$').toString(), Pattern.CASE_INSENSITIVE);
    }

    private static boolean looksLikeWildcard(@NotNull String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0;
    }

    /**
     * Normalises whatever the configuration layer holds for the {@code creative-groups} key. The key is
     * typed loosely so that its written-back default stays a plain YAML mapping rather than a serialized
     * Java object — see {@code ConfigurationKey.CREATIVE_GROUPS}.
     */
    public static CreativeGroupRules from(@Nullable Object configValue) {
        if (configValue instanceof CreativeGroupRules rules) return rules;
        if (configValue instanceof ConfigurationSection section) return parse(section);
        return EMPTY;
    }

    public boolean isEmpty() {
        return this.rules.isEmpty();
    }

    public int size() {
        return this.rules.size();
    }

    /**
     * The first rule matching {@code itemId}, or {@code null} when none does.
     *
     * @param itemId a CraftEngine item id such as {@code default:topaz_ore}
     */
    @Nullable
    public Rule match(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        for (Rule rule : this.rules) {
            for (Pattern pattern : rule.patterns()) {
                if (pattern.matcher(itemId).find()) return rule;
            }
        }
        return null;
    }

    /**
     * @param name     the rule's key in the configuration, used only in log messages
     * @param group    the vanilla group name to emit, e.g. {@code itemGroup.name.ore}
     * @param category an optional {@code creative_category} override; without it the material-derived
     *                 category still applies, and the group would be dropped if that came out as none
     * @param patterns every wildcard and regex of the rule, already compiled — a wildcard arrives here as the
     *                 anchored regex it is equivalent to, so matching needs to know no difference
     */
    public record Rule(@NotNull String name, @NotNull String group, @Nullable String category,
                       @NotNull List<Pattern> patterns) {

        /**
         * The declared category as a {@link BedrockOptions.CreativeCategory}, or {@code null} when the rule
         * declares none or names one that does not exist.
         */
        @Nullable
        public BedrockOptions.CreativeCategory resolvedCategory() {
            if (this.category == null || this.category.isBlank()) return null;
            try {
                return BedrockOptions.CreativeCategory.valueOf(this.category.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                Logger.warn("creative-groups." + this.name + " uses unknown category '" + this.category
                        + "' - falling back to the category derived from the item's material");
                return null;
            }
        }
    }
}
