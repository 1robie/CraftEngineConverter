package fr.robie.craftengineconverter.api.configuration.bedrock.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the MiniMessage string CraftEngine names an item with into a {@link TextComponent}.
 * <p>
 * What this replaces threw the formatting away: {@code input.replaceAll("<[^>]+>", "")} deleted every tag, so
 * {@code <!i><red><bold>Flame Cane</bold></red>} reached Bedrock as plain white "Flame Cane". The colour is in the
 * pack because the author put it there.
 * <p>
 * <b>{@code <!i>} is the one to understand.</b> Java renders a custom item name in italics by default, so nearly
 * every CraftEngine name opens with {@code <!i>} to turn that off. Bedrock does not italicise by default, so the
 * tag looks redundant — it is not. It has to be emitted as an explicit {@code "italic": false}, because a name that
 * also sets a colour becomes a component, and a component without the flag would let Java's default back in.
 * <p>
 * <b>This parser is deliberately partial.</b> MiniMessage is a large grammar and a half-understood tag mangles a
 * name, which is worse than dropping formatting. Only the tags CraftEngine actually emits are handled; anything
 * else makes {@link #parse} return {@code null} so the caller can fall back to the old strip-to-plain-text
 * behaviour. Failing to the previous result is always available, so this can never be worse than what it replaces.
 */
public final class MiniMessageToComponent {

    private MiniMessageToComponent() {
        throw new UnsupportedOperationException("MiniMessageToComponent is a utility class.");
    }

    private static final Pattern TAG = Pattern.compile("<([^<>]+)>");
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    /** The sixteen Java colour names, which MiniMessage and the component format spell identically. */
    private static final Set<String> COLORS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white");

    // MiniMessage's short spellings, mapped onto the component field each one sets.
    private static final java.util.Map<String, String> STYLES = java.util.Map.of(
            "bold", "bold", "b", "bold",
            "italic", "italic", "i", "italic", "em", "italic",
            "underlined", "underlined", "u", "underlined",
            "strikethrough", "strikethrough", "st", "strikethrough",
            "obfuscated", "obfuscated");

    /** One level of styling, as the tag stack sees it. */
    private record Style(String color, java.util.Map<String, Boolean> flags) {

        static Style empty() {
            return new Style(null, java.util.Map.of());
        }

        Style withColor(String value) {
            return new Style(value, this.flags);
        }

        Style withFlag(String name, boolean value) {
            java.util.Map<String, Boolean> next = new java.util.LinkedHashMap<>(this.flags);
            next.put(name, value);
            return new Style(this.color, next);
        }

        void applyTo(TextComponent component) {
            if (this.color != null) component.withColor(this.color);
            for (var flag : this.flags.entrySet()) {
                switch (flag.getKey()) {
                    case "bold" -> component.withBold(flag.getValue());
                    case "italic" -> component.withItalic(flag.getValue());
                    case "underlined" -> component.withUnderlined(flag.getValue());
                    case "strikethrough" -> component.withStrikethrough(flag.getValue());
                    case "obfuscated" -> component.withObfuscated(flag.getValue());
                    default -> { }
                }
            }
        }

        boolean isEmpty() {
            return this.color == null && this.flags.isEmpty();
        }
    }

    /**
     * @return the parsed name, or {@code null} when the string uses a tag this does not understand and the caller
     *         should fall back
     */
    public static TextComponent parse(String input) {
        if (input == null) return null;

        Deque<Style> stack = new ArrayDeque<>();
        stack.push(Style.empty());

        List<TextComponent> parts = new java.util.ArrayList<>();
        Matcher matcher = TAG.matcher(input);
        int cursor = 0;

        while (matcher.find()) {
            String literal = input.substring(cursor, matcher.start());
            if (!literal.isEmpty()) parts.add(styled(TextComponent.literal(literal), stack.peek()));
            cursor = matcher.end();

            String tag = matcher.group(1).trim();
            if (!handle(tag, stack, parts)) return null;
        }

        String tail = input.substring(cursor);
        if (!tail.isEmpty()) parts.add(styled(TextComponent.literal(tail), stack.peek()));

        return combine(parts);
    }

    /** @return whether the tag was understood */
    private static boolean handle(String tag, Deque<Style> stack, List<TextComponent> parts) {
        String lower = tag.toLowerCase(Locale.ROOT);

        if (lower.equals("reset")) {
            stack.clear();
            stack.push(Style.empty());
            return true;
        }

        // A closing tag drops the level it opened. Unbalanced input is tolerated rather than rejected: the base
        // style is kept so the name still renders, which beats refusing a name over a stray </bold>.
        if (lower.startsWith("/")) {
            if (stack.size() > 1) stack.pop();
            return true;
        }

        // "<!i>" and "<!bold>" - MiniMessage's negation, which is how a name turns Java's default italics off.
        if (lower.startsWith("!")) {
            String name = STYLES.get(lower.substring(1));
            if (name == null) return false;
            stack.push(stack.peek().withFlag(name, false));
            return true;
        }

        if (lower.startsWith("lang:")) {
            String key = tag.substring("lang:".length()).trim();
            if (key.isEmpty()) return false;
            parts.add(styled(TextComponent.translatable(key), stack.peek()));
            return true;
        }

        if (COLORS.contains(lower)) {
            stack.push(stack.peek().withColor(lower));
            return true;
        }

        if (HEX.matcher(lower).matches()) {
            stack.push(stack.peek().withColor(lower));
            return true;
        }

        String style = STYLES.get(lower);
        if (style != null) {
            stack.push(stack.peek().withFlag(style, true));
            return true;
        }

        // Gradients, rainbows, click and hover events, fonts, anything else - not understood, so say so.
        return false;
    }

    private static TextComponent styled(TextComponent component, Style style) {
        if (style != null && !style.isEmpty()) style.applyTo(component);
        return component;
    }

    /**
     * Folds the parts into one component.
     * <p>
     * A single part is returned as-is rather than wrapped, so an unstyled name stays a plain string in the mapping
     * and a bare {@code <lang:...>} name stays a bare {@code translate} - which is what lets the caller recognise it
     * and register the Bedrock lang alias.
     */
    private static TextComponent combine(List<TextComponent> parts) {
        if (parts.isEmpty()) return null;
        if (parts.size() == 1) return parts.getFirst();

        // An empty root, so no styling of the first part leaks onto the rest through inheritance.
        TextComponent root = TextComponent.literal("");
        for (TextComponent part : parts) root.addExtra(part);
        return root;
    }
}
