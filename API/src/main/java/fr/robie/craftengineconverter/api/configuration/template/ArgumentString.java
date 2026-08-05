package fr.robie.craftengineconverter.api.configuration.template;

import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A config string with its {@code ${placeholder}} positions already located.
 * <p>
 * CraftEngine templates are parameterised with {@code ${name}} placeholders, and a single value can mix
 * literal text with several of them ({@code minecraft:item/custom/${material}_${part}}). Parsing is done
 * once, up front, so that resolving a template is a walk over pre-split parts rather than a regex pass over
 * every string every time — which matters because a template is re-resolved for every item that uses it.
 * <p>
 * Keys are parsed too, not just values: a template may name a key by placeholder, and a placeholder that
 * resolves to {@code null} <b>drops its entry</b> — that is how CraftEngine omits a key conditionally.
 *
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code ${name}} — an unbound argument resolves to nothing and drops its entry, with a warning</li>
 *   <li>{@code ${name:-fallback}} — used when the argument is unbound</li>
 *   <li>{@code ${name^}} — capitalise; {@code ${name^^}} — upper-case</li>
 *   <li>{@code \$} — a literal dollar</li>
 *   <li>{@code \{} and {@code \}} — literal braces <i>inside</i> a placeholder</li>
 * </ul>
 * Brace depth is tracked, so braces nested inside a placeholder do not end it early. An unclosed
 * {@code ${} is treated as ordinary text rather than raising — a malformed string should not abort a
 * conversion.
 */
public interface ArgumentString {

    /** The string as authored, used for error messages. */
    String rawValue();

    /**
     * Resolves against {@code arguments}.
     *
     * @return the resolved value, or {@code null} to signal that the owning entry should be dropped
     */
    @Nullable
    Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments);

    /** Text with no placeholders. */
    final class Literal implements ArgumentString {
        private final String value;

        private Literal(String value) {
            this.value = value;
        }

        public static Literal of(String value) {
            return new Literal(value);
        }

        @Override
        public String rawValue() {
            return this.value;
        }

        @Override
        public Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments) {
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Literal other && this.value.equals(other.value);
        }

        @Override
        public int hashCode() {
            return this.value.hashCode();
        }

        @Override
        public String toString() {
            return "Literal(" + this.value + ")";
        }
    }

    /** A single {@code ${...}} reference. */
    final class Placeholder implements ArgumentString {
        private final String name;
        private final String rawText;
        private final String fallback;
        private final boolean hasFallback;
        private final boolean capitalize;
        private final boolean upperCase;

        /**
         * Strips one layer of matching quotes from a fallback.
         * <p>
         * A config writes {@code ${fence_type:-"oak"}_fence} and means the block {@code oak_fence}. The quotes are
         * ordinary characters inside a YAML plain scalar, so keeping them produced the id {@code "oak"_fence},
         * which matches no block — the fence's host state, and so the whole fence, resolved to nothing.
         */
        private static String unquote(String fallback) {
            if (fallback.length() < 2) return fallback;
            char first = fallback.charAt(0);
            char last = fallback.charAt(fallback.length() - 1);
            boolean quoted = (first == '"' || first == '\'') && first == last;
            return quoted ? fallback.substring(1, fallback.length() - 1) : fallback;
        }

        private Placeholder(String content) {
            this.rawText = "${" + content + "}";

            int separator = content.indexOf(":-");
            String namePart;
            if (separator < 0) {
                namePart = content;
                this.fallback = null;
                this.hasFallback = false;
            } else {
                namePart = content.substring(0, separator);
                this.fallback = unquote(content.substring(separator + 2));
                this.hasFallback = true;
            }

            // "^^" must be tested before "^", since both end with a caret.
            if (namePart.endsWith("^^")) {
                this.capitalize = false;
                this.upperCase = true;
                this.name = namePart.substring(0, namePart.length() - 2);
            } else if (namePart.endsWith("^")) {
                this.capitalize = true;
                this.upperCase = false;
                this.name = namePart.substring(0, namePart.length() - 1);
            } else {
                this.capitalize = false;
                this.upperCase = false;
                this.name = namePart;
            }
        }

        public static Placeholder of(String content) {
            return new Placeholder(content);
        }

        public String name() {
            return this.name;
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments) {
            Object value;
            TemplateArgument argument = arguments.get(this.name);
            if (argument != null) {
                value = argument.resolve(node, arguments);
            } else if (this.hasFallback) {
                value = parse(this.fallback).resolve(node, arguments);
            } else {
                boolean insideTemplateBody = node.contains("${") || node.contains("template[");
                if (insideTemplateBody) {
                    Logger.debug("Template argument " + this.rawText + " at " + node
                            + " is unbound while resolving a template body; omitting that entry");
                } else {
                    Logger.warn("Template argument " + this.rawText + " at " + node
                            + " is unbound; omitting that entry");
                }
                return null;
            }

            if (value == null) return null;
            if (this.upperCase) return String.valueOf(value).toUpperCase(Locale.ROOT);
            if (this.capitalize) return capitalise(String.valueOf(value));
            return value;
        }

        private static String capitalise(String value) {
            if (value.isEmpty()) return value;
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Placeholder other && this.rawText.equals(other.rawText);
        }

        @Override
        public int hashCode() {
            return this.rawText.hashCode();
        }

        @Override
        public String toString() {
            return "Placeholder(" + this.name + ")";
        }
    }

    /** Literal text and placeholders interleaved; the parts are concatenated on resolve. */
    final class Composite implements ArgumentString {
        private final ArgumentString[] parts;
        private final String rawText;

        private Composite(String rawText, ArgumentString[] parts) {
            this.rawText = rawText;
            this.parts = parts;
        }

        public static Composite of(String rawText, List<ArgumentString> parts) {
            return new Composite(rawText, parts.toArray(new ArgumentString[0]));
        }

        @Override
        public String rawValue() {
            return this.rawText;
        }

        @Override
        public Object resolve(@NotNull String node, @NotNull Map<String, TemplateArgument> arguments) {
            StringBuilder result = new StringBuilder();
            boolean any = false;
            for (ArgumentString part : this.parts) {
                Object resolved = part.resolve(node, arguments);
                if (resolved != null) {
                    result.append(resolved);
                    any = true;
                }
            }
            return any ? result.toString() : null;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Composite other && this.rawText.equals(other.rawText);
        }

        @Override
        public int hashCode() {
            return this.rawText.hashCode();
        }

        @Override
        public String toString() {
            return "Composite(" + this.rawText + ")";
        }
    }

    /**
     * Splits {@code input} into literal and placeholder parts.
     * <p>
     * A string with no {@code ${} returns a {@link Literal}, which is the overwhelmingly common case and
     * costs one scan.
     */
    @NotNull
    static ArgumentString parse(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return Literal.of("");
        }

        List<ArgumentString> parts = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int length = input.length();
        int i = 0;

        while (i < length) {
            char c = input.charAt(i);

            if (c == '$' && i + 1 < length && input.charAt(i + 1) == '{') {
                Scanned scanned = scanPlaceholder(input, i + 2);
                if (scanned == null) {
                    literal.append(c);
                    i++;
                    continue;
                }
                if (!literal.isEmpty()) {
                    parts.add(Literal.of(literal.toString()));
                    literal.setLength(0);
                }
                parts.add(Placeholder.of(scanned.content()));
                i = scanned.closeIndex() + 1;
            } else if (c == '\\' && i + 1 < length && input.charAt(i + 1) == '$') {
                literal.append('$');
                i += 2;
            } else {
                literal.append(c);
                i++;
            }
        }

        if (!literal.isEmpty()) {
            parts.add(Literal.of(literal.toString()));
        }

        return switch (parts.size()) {
            case 0 -> Literal.of("");
            case 1 -> parts.getFirst();
            default -> Composite.of(input, parts);
        };
    }

    /** A placeholder body and the index of the brace that closed it. */
    record Scanned(String content, int closeIndex) {}

    /**
     * Scans the body of a placeholder opened just before {@code start}.
     *
     * @return the body and closing index, or {@code null} if unterminated
     */
    @Nullable
    private static Scanned scanPlaceholder(String input, int start) {
        StringBuilder content = new StringBuilder();
        int depth = 1;
        int length = input.length();

        for (int j = start; j < length; ) {
            char c = input.charAt(j);
            if (c == '\\' && j + 1 < length && (input.charAt(j + 1) == '{' || input.charAt(j + 1) == '}')) {
                content.append(input.charAt(j + 1));
                j += 2;
            } else if (c == '{') {
                depth++;
                content.append(c);
                j++;
            } else if (c == '}') {
                if (--depth == 0) {
                    return new Scanned(content.toString(), j);
                }
                content.append(c);
                j++;
            } else {
                content.append(c);
                j++;
            }
        }
        return null;
    }
}
