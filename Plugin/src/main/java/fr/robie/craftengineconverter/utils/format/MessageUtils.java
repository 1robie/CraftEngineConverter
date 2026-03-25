package fr.robie.craftengineconverter.utils.format;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.format.TextFormatter;
import org.bukkit.command.CommandSender;

public abstract class MessageUtils implements TextFormatter {

    protected void messageWO(CraftEngineConverter plugin, CommandSender sender, Message message, Object... placeholders) {
        plugin.getMessageFormatter().sendMessage(sender, message, false, placeholders);
    }

    protected void message(CraftEngineConverter plugin, CommandSender sender, Message message, Object... args) {
        plugin.getMessageFormatter().sendMessage(sender, message, true, args);
    }
}
