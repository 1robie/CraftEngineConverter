package fr.robie.craftengineconverter.utils;

import fr.robie.craftengineconverter.common.configuration.Configuration;
import fr.robie.craftengineconverter.common.tag.ITagResolver;
import fr.robie.craftengineconverter.common.tag.TagProcessor;
import fr.robie.craftengineconverter.hooks.placeholderapi.tag.PlaceholderAPITag;
import fr.robie.craftengineconverter.tag.GlyphTagProcessor;
import fr.robie.craftengineconverter.utils.plugins.Plugins;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagResolver implements ITagResolver {
    private final List<TagProcessor> tagProcessors = new ArrayList<>();

    @Override
    public void initTagProcessors() {
        if (Configuration.glyphTagEnabled){
            this.tagProcessors.add(new GlyphTagProcessor());
        }
        if (Plugins.PLACEHOLDER_API.isPresent()){
            this.tagProcessors.add(new PlaceholderAPITag());
        }
    }

    @Override
    public Optional<String> resolveTags(String message, Player player) {
        String result = message;
        boolean modified = false;

        for (TagProcessor processor : this.tagProcessors) {
            if (!processor.hasTag(result)) {
                continue;
            }
            Optional<String> processed = processor.process(result, player);
            if (processed.isPresent()) {
                result = processed.get();
                modified = true;
            }
        }

        return modified ? Optional.of(result) : Optional.empty();
    }


}
