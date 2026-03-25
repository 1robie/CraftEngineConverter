package fr.robie.craftengineconverter.common.format;

import fr.robie.craftengineconverter.api.format.CraftEngineConverterMessage;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.format.message.ClassicMessage;
import fr.robie.craftengineconverter.api.format.message.TitleMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class ClassicMeta implements fr.robie.craftengineconverter.api.format.MessageFormatter {

    protected String color(String message) {
        if (message == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, String.valueOf(net.md_5.bungee.api.ChatColor.of(color)));
            matcher = pattern.matcher(message);
        }
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    private List<String> getMessages(@NotNull ClassicMessage message, @NotNull String prefix, @NotNull Object[] placeholders) {
        return message.messages().stream()
                .map(part -> this.color(this.parseText(prefix + part, placeholders)))
                .toList();
    }


    private <U, T> void send(@NotNull BiConsumer<U, T> function, @NotNull Collection<U> players, @NotNull T message) {
        for (U player : players) {
            function.accept(player, message);
        }
    }

    @Override
    public void sendMessage(@NotNull CommandSender sender, String message) {
        sender.sendMessage(this.color(message));
    }

    @Override
    public void sendTitle(@NotNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(this.color(title), this.color(subtitle), fadeIn, stay, fadeOut);
    }

    @Override
    public void sendAction(@NotNull Player player, String message) {
        player.sendActionBar(this.color(message));
    }

    @Override
    public void sendMessage(@NotNull CommandSender sender, @NotNull Message message, boolean prefix, @NotNull Object[] placeholders) {
        for (CraftEngineConverterMessage craftMessage : message.getCraftMessages()) {
            switch (craftMessage.messageType()) {
                case TITLE -> {
                    if (sender instanceof Player player) {
                        TitleMessage titleMessage = (TitleMessage) craftMessage;
                        this.sendTitle(player,
                                titleMessage.title(), titleMessage.subtitle(),
                                titleMessage.fadeIn(), titleMessage.stay(), titleMessage.fadeOut());
                    }
                }
                case TCHAT -> {
                    String prefixText = prefix ? Message.COMMAND__PREFIX.getMessage() : "";
                    this.getMessages((ClassicMessage) craftMessage, prefixText, placeholders)
                            .forEach(part -> this.send(CommandSender::sendMessage, List.of(sender), part));
                }
                case ACTION_BAR -> {
                    if (sender instanceof Player player) {
                        this.getMessages((ClassicMessage) craftMessage, "", placeholders)
                                .forEach(part -> this.send(Player::sendActionBar, List.of(player), part));
                    }
                }
                case WITHOUT_PREFIX -> {
                    this.getMessages((ClassicMessage) craftMessage, "", placeholders)
                            .forEach(part -> this.send(CommandSender::sendMessage, List.of(sender), part));
                }
                case TCHAT_AND_ACTION_BAR -> {
                    if (sender instanceof Player player) {
                        String prefixText = prefix ? Message.COMMAND__PREFIX.getMessage() : "";
                        this.getMessages((ClassicMessage) craftMessage, prefixText, placeholders)
                                .forEach(part -> {
                                    this.send(Player::sendMessage, List.of(player), part);
                                    this.send(Player::sendActionBar, List.of(player), part);
                                });
                    }
                }
                default -> {
                }
            }
        }
    }

    @Override
    public String getMessageColorized(String message) {
        return this.getMessageLegacyColorized(message);
    }

    @Override
    public String getMessageLegacyColorized(String message) {
        return this.color(message);
    }
}