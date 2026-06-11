package fr.robie.craftengineconverter.api;

import fr.robie.craftengineconverter.api.manager.FoliaCompatibilityManager;
import fr.robie.craftengineconverter.api.tag.ITagResolver;
import fr.robie.messageflow.formatter.MessageFormatter;

public interface CraftEngineConverterPluginInterface {
    FoliaCompatibilityManager getFoliaCompatibilityManager();

    MessageFormatter<?, ?> getMessageFormatter();

    ITagResolver getTagResolver();
}
