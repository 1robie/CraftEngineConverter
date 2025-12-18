package fr.robie.craftengineconverter.common;

import fr.robie.craftengineconverter.common.format.MessageFormatter;
import fr.robie.craftengineconverter.common.tag.TagResolverUtils;

public interface CraftEngineConverterPlugin {
    MessageFormatter getMessageFormatter();

    TagResolverUtils getTagResolver();

    FoliaCompatibilityManager getFoliaCompatibilityManager();
}
