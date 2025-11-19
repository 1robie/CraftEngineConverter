package fr.robie.craftEngineConverter.core.utils.collection;

import org.bukkit.command.CommandSender;

import java.util.List;

@FunctionalInterface
public interface CollectionBiConsumer {

    /**
     * Accept consumer
     *
     * @param sender command sender
     * @param args command arguments
     * @return list of string
     */
    List<String> accept(CommandSender sender, String[] args);
}