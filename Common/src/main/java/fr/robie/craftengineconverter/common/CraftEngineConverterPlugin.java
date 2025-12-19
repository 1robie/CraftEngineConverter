package fr.robie.craftengineconverter.common;

import fr.robie.craftengineconverter.common.format.MessageFormatter;
import fr.robie.craftengineconverter.common.tag.ITagResolver;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class CraftEngineConverterPlugin extends JavaPlugin {
    public abstract MessageFormatter getMessageFormatter();

    public abstract ITagResolver getTagResolver();

    public abstract FoliaCompatibilityManager getFoliaCompatibilityManager();
}
