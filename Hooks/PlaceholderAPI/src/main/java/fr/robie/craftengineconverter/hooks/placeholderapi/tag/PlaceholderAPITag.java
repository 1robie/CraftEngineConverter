package fr.robie.craftengineconverter.hooks.placeholderapi.tag;

import fr.robie.craftengineconverter.common.tag.TagProcessor;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderAPITag implements TagProcessor {
    private static final Pattern PLACEHOLDER_API_PATTERN = Pattern.compile("\\\\?<(?:placeholderapi|papi):([^>]+)>");

    @Override
    public String getTagName() {
        return "placeholderapi";
    }

    @Override
    public Pattern getPattern() {
        return PLACEHOLDER_API_PATTERN;
    }

    @Override
    public Optional<String> process(String input, Player player) {
        if (player == null) {
            return Optional.of(input);
        }

        Matcher matcher = PLACEHOLDER_API_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);

            String[] placeholders = placeholder.split(":");

            StringBuilder joinedPlaceholder = new StringBuilder();
            for (String placeholderPart : placeholders) {
                joinedPlaceholder.append(PlaceholderAPI.setPlaceholders(player, "%" + placeholderPart + "%"));
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(joinedPlaceholder.toString()));
        }

        matcher.appendTail(result);
        return Optional.of(result.toString());
    }

    @Override
    public boolean hasTag(String input) {
        return PLACEHOLDER_API_PATTERN.matcher(input).find();
    }
}
