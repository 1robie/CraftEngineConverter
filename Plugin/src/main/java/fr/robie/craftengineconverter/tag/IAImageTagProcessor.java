package fr.robie.craftengineconverter.tag;

import fr.robie.craftengineconverter.api.tag.TagProcessor;
import fr.robie.craftengineconverter.common.utils.CraftEngineImageUtils;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IAImageTagProcessor implements TagProcessor {
    private static final Pattern IA_GUI_PATTERN = Pattern.compile(":([^:]+):");

    @Override
    public String getTagName() {
        return "ItemsAdder Bitmap";
    }

    @Override
    public Pattern getPattern() {
        return IA_GUI_PATTERN;
    }

    @Override
    public boolean hasTag(String input) {
        return IA_GUI_PATTERN.matcher(input).find();
    }

    @Override
    public Optional<String> process(String input, Player player) {
        Matcher matcher = IA_GUI_PATTERN.matcher(input);

        if (!matcher.find()) {
            return Optional.empty();
        }

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        matcher.reset();

        while (matcher.find()) {
            String imageName = matcher.group(1);
            String fullMatch = matcher.group(0);

            result.append(input, lastEnd, matcher.start());
            if (imageName.startsWith("offset_")) {
                String substring = imageName.substring("offset_".length());
                int finalOffset;
                try {
                    finalOffset = Integer.parseInt(substring);
                    String offsetImage = CraftEngineImageUtils.createOffsetImage(finalOffset);
                    result.append(offsetImage);
                } catch (NumberFormatException e) {
                    result.append(fullMatch);
                    lastEnd = matcher.end();
                    continue;
                }
            } else {
                Optional<String> converted = CraftEngineImageUtils.convert(imageName);
                if (converted.isPresent()) {
                    result.append(converted.get());
                } else {
                    result.append(fullMatch);
                }
            }
            lastEnd = matcher.end();
        }

        result.append(input, lastEnd, input.length());

        return Optional.of(result.toString());
    }
}
