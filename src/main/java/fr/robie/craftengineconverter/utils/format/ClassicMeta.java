package fr.robie.craftengineconverter.utils.format;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassicMeta implements MessageFormatter{
    @Override
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    @Override
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    @Override
    public void sendAction(Player player, String message) {
        player.sendActionBar(color(message));
    }

    @Override
    public String getMessageColorized(String message) {
        return getMessageLegacyColorized(message);
    }

    @Override
    public String getMessageLegacyColorized(String message) {
        return color(message);
    }

    protected String color(String message) {
        if (message == null) return null;
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, String.valueOf(net.md_5.bungee.api.ChatColor.of(color)));
            matcher = pattern.matcher(message);
        }
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}
