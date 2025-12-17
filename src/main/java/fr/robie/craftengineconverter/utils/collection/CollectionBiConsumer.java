package fr.robie.craftengineconverter.utils.collection;

import org.bukkit.command.CommandSender;

import java.util.Collection;

@FunctionalInterface
public interface CollectionBiConsumer {

    /**
     * Accept consumer
     *
     * @param sender command sender
     * @param args command arguments
     * @return list of string
     */
    Collection<String> accept(CommandSender sender, String[] args);
}