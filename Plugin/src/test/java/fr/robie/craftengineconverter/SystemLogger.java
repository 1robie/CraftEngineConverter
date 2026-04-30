package fr.robie.craftengineconverter;

import fr.robie.craftengineconverter.api.logger.LogType;
import fr.robie.craftengineconverter.api.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemLogger extends Logger {

    private static final String ANSI_RESET = "\u001B[0m";

    public SystemLogger() {
        super("CraftEngineConverter");
    }

    @Override
    public void log(@Nullable String subPrefix, String message, LogType logType, Object... args) {
        String prefix = subPrefix != null ? "[" + this.prefix + "] [" + subPrefix + "] " : "[" + this.prefix + "] ";

        String ansiColor = toAnsi(logType.getColor());
        String parsed = this.parseText(message, args);

        System.out.println(prefix + ansiColor + stripFormatting(parsed) + ANSI_RESET);
    }

    private static String toAnsi(String color) {
        if (color == null || color.isEmpty()) return "";
        if (color.startsWith("§") && color.length() == 2) {
            String ansi = legacyCodeToAnsi(Character.toLowerCase(color.charAt(1)));
            return ansi != null ? ansi : "";
        }
        if (color.startsWith("<") && color.endsWith(">")) {
            String tag = color.substring(1, color.length() - 1).toLowerCase();
            String ansi = miniMessageTagToAnsi(tag);
            return ansi != null ? ansi : "";
        }
        return "";
    }

    private static String stripFormatting(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String ansi = legacyCodeToAnsi(code);
                if (ansi != null) sb.append(ansi);
                i++;
                continue;
            }
            sb.append(text.charAt(i));
        }
        text = sb.toString();

         text = text.replace("<reset>", ANSI_RESET);
        text = text.replaceAll("</[^>]+>", ANSI_RESET);

        text = text.replace("<bold>", "\u001B[1m");
        text = text.replace("<italic>", "\u001B[3m");
        text = text.replace("<underlined>", "\u001B[4m");
        text = text.replace("<strikethrough>", "\u001B[9m");

        text = Pattern.compile("<[^>]+>").matcher(text).replaceAll(match -> {
            String tag = match.group().substring(1, match.group().length() - 1).toLowerCase();
            String ansi = miniMessageTagToAnsi(tag);
            return ansi != null ? Matcher.quoteReplacement(ansi) : "";
        });

        return text;
    }

    @Nullable
    private static String legacyCodeToAnsi(char code) {
        return switch (code) {
            case '0' -> "\u001B[30m";  // black
            case '1' -> "\u001B[34m";  // dark_blue
            case '2' -> "\u001B[32m";  // dark_green
            case '3' -> "\u001B[36m";  // dark_aqua
            case '4' -> "\u001B[31m";  // dark_red
            case '5' -> "\u001B[35m";  // dark_purple
            case '6' -> "\u001B[33m";  // gold
            case '7' -> "\u001B[37m";  // gray
            case '8' -> "\u001B[90m";  // dark_gray
            case '9' -> "\u001B[94m";  // blue
            case 'a' -> "\u001B[92m";  // green
            case 'b' -> "\u001B[96m";  // aqua
            case 'c' -> "\u001B[91m";  // red
            case 'd' -> "\u001B[95m";  // light_purple
            case 'e' -> "\u001B[93m";  // yellow
            case 'f' -> "\u001B[97m";  // white
            case 'l' -> "\u001B[1m";   // bold
            case 'o' -> "\u001B[3m";   // italic
            case 'n' -> "\u001B[4m";   // underline
            case 'm' -> "\u001B[9m";   // strikethrough
            case 'r' -> ANSI_RESET;
            default  -> null;
        };
    }

    @Nullable
    private static String miniMessageTagToAnsi(String tag) {
        return switch (tag) {
            case "black"        -> "\u001B[30m";
            case "dark_blue"    -> "\u001B[34m";
            case "dark_green"   -> "\u001B[32m";
            case "dark_aqua"    -> "\u001B[36m";
            case "dark_red"     -> "\u001B[31m";
            case "dark_purple"  -> "\u001B[35m";
            case "gold"         -> "\u001B[33m";
            case "gray"         -> "\u001B[37m";
            case "dark_gray"    -> "\u001B[90m";
            case "blue"         -> "\u001B[94m";
            case "green"        -> "\u001B[92m";
            case "aqua"         -> "\u001B[96m";
            case "red"          -> "\u001B[91m";
            case "light_purple" -> "\u001B[95m";
            case "yellow"       -> "\u001B[93m";
            case "white"        -> "\u001B[97m";
            case "bold"         -> "\u001B[1m";
            case "italic"       -> "\u001B[3m";
            case "underlined"   -> "\u001B[4m";
            case "strikethrough"-> "\u001B[9m";
            case "reset"        -> ANSI_RESET;
            default             -> null;
        };
    }
}