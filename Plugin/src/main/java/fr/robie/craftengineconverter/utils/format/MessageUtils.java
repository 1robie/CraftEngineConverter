package fr.robie.craftengineconverter.utils.format;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.ObjectUtils;
import fr.robie.craftengineconverter.utils.enums.DefaultFontInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public abstract class MessageUtils extends ObjectUtils {
    private final static int CENTER_PX = 154;

    protected void messageWO(CraftEngineConverter plugin, CommandSender sender, fr.robie.craftengineconverter.common.format.Message message, Object... args) {
        plugin.getMessageFormatter().sendMessage(sender, getMessage(message, args));
    }

    protected void messageWO(CraftEngineConverter plugin, CommandSender player, String message, Object... args) {
        plugin.getMessageFormatter().sendMessage(player, getMessage(message, args));
    }
    protected void message(CraftEngineConverter plugin, CommandSender sender, String message, Object... args) {
        plugin.getMessageFormatter().sendMessage(sender, fr.robie.craftengineconverter.common.format.Message.PREFIX.msg() + getMessage(message, args));
    }
    protected void message(CraftEngineConverter plugin, CommandSender sender, fr.robie.craftengineconverter.common.format.Message message, Object... args) {
        if (sender instanceof ConsoleCommandSender) {
            if (!message.getMessages().isEmpty()) {
                message.getMessages().forEach(msg -> plugin.getMessageFormatter().sendMessage(sender, fr.robie.craftengineconverter.common.format.Message.PREFIX.msg() + getMessage(msg, args)));
            } else {
                plugin.getMessageFormatter().sendMessage(sender, fr.robie.craftengineconverter.common.format.Message.PREFIX.msg() + getMessage(message, args));
            }
        } else {
            Player player = (Player) sender;
            switch (message.getType()) {
                case CENTER:
                    if (!message.getMessages().isEmpty()) {
                        message.getMessages().forEach(msg -> plugin.getMessageFormatter().sendMessage(sender, this.getCenteredMessage(getMessage(msg, args))));
                    } else {
                        plugin.getMessageFormatter().sendMessage(sender, this.getCenteredMessage(getMessage(message, args)));
                    }
                    break;
                case ACTION:
                    plugin.getMessageFormatter().sendAction(player, getMessage(message, args));
                    break;
                case TCHAT:
                    if (!message.getMessages().isEmpty()) {
                        message.getMessages().forEach(msg -> plugin.getMessageFormatter().sendMessage(sender, fr.robie.craftengineconverter.common.format.Message.PREFIX.msg() + getMessage(msg, args)));
                    } else {
                        plugin.getMessageFormatter().sendMessage(sender, fr.robie.craftengineconverter.common.format.Message.PREFIX.msg() + getMessage(message, args));
                    }
                    break;
                default:
                    break;
            }
        }


    }

    private void message(CraftEngineConverter plugin, CommandSender sender, String message) {
        plugin.getMessageFormatter().sendMessage(sender, message);
    }
    protected String getMessage(fr.robie.craftengineconverter.common.format.Message message, Object... args) {
        return getMessage(message.getMessage(), args);
    }
    protected String getMessage(String message, Object... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Number of invalid arguments. Arguments must be in pairs.");
        }

        for (int i = 0; i < args.length; i += 2) {
            if (args[i] == null || args[i + 1] == null) {
                throw new IllegalArgumentException("Keys and replacement values must not be null.");
            }
            message = message.replace("%"+args[i].toString()+"%", args[i + 1].toString());
        }
        return message;
    }
    protected String getCenteredMessage(String message) {
        if (message == null || message.equals("")) {
            return "";
        }
        message = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb + message;
    }
}
