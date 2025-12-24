package fr.robie.craftengineconverter.common.format;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface MessageFormatter {

    void sendMessage(@NotNull CommandSender sender, String message);

    void sendTitle(@NotNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void sendAction(@NotNull Player player, String message);

    String getMessageColorized(String message);

    String getMessageLegacyColorized(String message);
}
