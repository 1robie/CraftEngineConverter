package fr.robie.craftengineconverter.utils;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.tag.TagProcessor;
import fr.robie.craftengineconverter.common.tag.TagResolverUtils;
import fr.robie.craftengineconverter.tag.GlyphTagProcessor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TagResolver implements TagResolverUtils {
    private final CraftEngineConverter plugin;
    private final List<TagProcessor> tagProcessors = new ArrayList<>();

    public TagResolver(CraftEngineConverter plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initTagProcessors() {
        this.tagProcessors.add(new GlyphTagProcessor());
    }

    @Override
    public Optional<String> resolveTags(String message, Player player) {
        String result = message;
        boolean modified = false;

        for (TagProcessor processor : this.tagProcessors) {
            if (!processor.hasTag(result)) {
                continue;
            }
            Optional<String> processed = processor.process(result);
            if (processed.isPresent()) {
                result = processed.get();
                modified = true;
            }
        }

        return modified ? Optional.of(result) : Optional.empty();
    }


}
