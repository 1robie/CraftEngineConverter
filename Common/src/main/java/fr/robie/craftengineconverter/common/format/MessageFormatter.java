package fr.robie.craftengineconverter.common.format;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface MessageFormatter {

    void sendMessage(CommandSender sender, String message);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void sendAction(Player player, String message);

    String getMessageColorized(String message);

    String getMessageLegacyColorized(String message);
}
