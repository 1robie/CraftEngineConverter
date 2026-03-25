package fr.robie.craftengineconverter.api.format;

import fr.robie.craftengineconverter.api.CraftEngineConverterPluginInterface;
import fr.robie.craftengineconverter.api.cache.SimpleCache;
import fr.robie.craftengineconverter.api.format.message.BossBarMessage;
import fr.robie.craftengineconverter.api.format.message.ClassicMessage;
import fr.robie.craftengineconverter.api.format.message.TitleMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentMeta implements MessageFormatter {
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern HEX_SHORT_PATTERN = Pattern.compile("(?<!<)(?<!:)(?<!</)&#([a-fA-F0-9]{6})");
    private final SimpleCache<String, Component> cache = new SimpleCache<>();

    private final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder().resolver(StandardTags.defaults()).build())
            .build();

    private final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private final Map<String, String> COLORS_MAPPINGS = Map.ofEntries(
            Map.entry("0", "black"), Map.entry("1", "dark_blue"),
            Map.entry("2", "dark_green"), Map.entry("3", "dark_aqua"),
            Map.entry("4", "dark_red"), Map.entry("5", "dark_purple"),
            Map.entry("6", "gold"), Map.entry("7", "gray"),
            Map.entry("8", "dark_gray"), Map.entry("9", "blue"),
            Map.entry("a", "green"), Map.entry("b", "aqua"),
            Map.entry("c", "red"), Map.entry("d", "light_purple"),
            Map.entry("e", "yellow"), Map.entry("f", "white"),
            Map.entry("k", "obfuscated"), Map.entry("l", "bold"),
            Map.entry("m", "strikethrough"), Map.entry("n", "underlined"),
            Map.entry("o", "italic"), Map.entry("r", "reset")
    );

    private final CraftEngineConverterPluginInterface plugin;


    public ComponentMeta(@NotNull CraftEngineConverterPluginInterface plugin) {
        this.plugin = plugin;
    }


    private String colorMiniMessage(String message) {
        message = this.convertLegacyHex(message);      // §x§r§g§b§2§f§3 → <#rgb2f3>
        message = this.convertShorLegacyHex(message);  // &#a1b2c3 → <#a1b2c3>
        message = this.replaceLegacyColors(message);   // &a → <green>, §c → <red>, …
        return message;
    }

    private @NotNull String convertLegacyHex(String message) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group().replaceAll("§x|§", "");
            matcher.appendReplacement(sb, "<#" + hex + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private @NotNull String convertShorLegacyHex(String message) {
        Matcher matcher = HEX_SHORT_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceLegacyColors(String message) {
        for (var entry : this.COLORS_MAPPINGS.entrySet()) {
            String key = entry.getKey();
            String value = "<" + entry.getValue() + ">";
            message = message.replace("&" + key, value)
                    .replace("§" + key, value)
                    .replace("&" + key.toUpperCase(), value)
                    .replace("§" + key.toUpperCase(), value);
        }
        return message;
    }

    public Component getComponent(String message) {
        return this.cache.getOrDefault(message, () ->
                this.MINI_MESSAGE.deserialize(this.colorMiniMessage(message))
                        .decoration(TextDecoration.ITALIC, false));
    }

    private Component getComponentWithPlaceholders(@NotNull String message, @NotNull Object... placeholders) {
        if (placeholders.length == 0) {
            return this.getComponent(message);
        }
        return this.MINI_MESSAGE.deserialize(this.colorMiniMessage(this.parseText(message, placeholders)));
    }

    public static String getPlainText(Component component) {
        if (component == null) {
            return "";
        }
        return MiniMessage.miniMessage().serialize(component);
    }


    private <T> void send(@NotNull BiConsumer<Audience, T> function, @NotNull Collection<Audience> audiences, @NotNull T message) {
        audiences.forEach(audience -> function.accept(audience, message));
    }

    private void sendComponents(@NotNull Collection<Audience> audiences, @NotNull ClassicMessage message, boolean prefix, @NotNull Object[] placeholders, @NotNull BiConsumer<Audience, Component> sender) {
        List<Component> components = this.getComponents(message, prefix, placeholders);
        if (!components.isEmpty()) {
            this.sendToAudiences(audiences, components, sender);
        }
    }

    public void sendToAudiences(@NotNull Collection<Audience> audiences, @NotNull List<Component> components, @NotNull BiConsumer<Audience, Component> sender) {
        if (components.isEmpty()) {
            return;
        }

        audiences.forEach(audience -> components.forEach(component -> sender.accept(audience, component)));
    }

    private List<Component> getComponents(@NotNull ClassicMessage message, boolean prefix, @NotNull Object[] placeholders) {
        List<String> messages = message.messages();
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        if (messages.getFirst() != null && messages.getFirst().isBlank()) {
            return Collections.emptyList();
        }

        String prefixText = prefix ? Message.COMMAND__PREFIX.getMessage() : "";
        return messages.stream()
                .map(s -> this.getComponentWithPlaceholders(prefixText + s, placeholders))
                .toList();
    }


    @Override
    public void sendMessage(@NotNull CommandSender sender, String message) {
        sender.sendMessage(this.getComponent(message));
    }

    @Override
    public void sendMessage(@NotNull CommandSender sender, @NotNull Message message, boolean prefix, @NotNull Object[] placeholders) {
        this.sendMessage(Collections.singleton(sender), message, prefix, placeholders);
    }

    public void sendMessage(@NotNull Collection<Audience> audiences, @NotNull Message message, boolean prefix, @NotNull Object... placeholders) {
        if (audiences.isEmpty()) {
            return;
        }

        for (CraftEngineConverterMessage craftmessage : message.getCraftMessages()) {
            switch (craftmessage.messageType()) {
                case TITLE -> {
                    TitleMessage titleMessage = (TitleMessage) craftmessage;
                    Title title = Title.title(
                            this.getComponentWithPlaceholders(titleMessage.title(), placeholders),
                            this.getComponentWithPlaceholders(titleMessage.subtitle(), placeholders),
                            titleMessage.fadeIn(), titleMessage.stay(), titleMessage.fadeOut()
                    );
                    this.sendTitle(audiences, title);
                }
                case TCHAT -> this.sendComponents(audiences, (ClassicMessage) craftmessage,
                        prefix, placeholders, Audience::sendMessage);
                case ACTION_BAR -> this.sendComponents(audiences, (ClassicMessage) craftmessage,
                        prefix, placeholders, Audience::sendActionBar);
                case WITHOUT_PREFIX -> this.sendComponents(audiences, (ClassicMessage) craftmessage,
                        false, placeholders, Audience::sendMessage);
                case TCHAT_AND_ACTION_BAR -> this.sendComponents(audiences, (ClassicMessage) craftmessage,
                        prefix, placeholders, (audience, component) -> {
                            audience.sendMessage(component);
                            audience.sendActionBar(component);
                        });
                case BOSS_BAR -> this.sendBossbar(audiences, (BossBarMessage) craftmessage, placeholders);
                default -> {
                }
            }
        }
    }

    @Override
    public void sendTitle(@NotNull Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = (title != null && !title.isEmpty()) ? this.getComponent(title) : null;
        Component subtitleComponent = (subtitle != null && !subtitle.isEmpty()) ? this.getComponent(subtitle) : null;

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        player.showTitle(Title.title(
                titleComponent != null ? titleComponent : Component.empty(),
                subtitleComponent != null ? subtitleComponent : Component.empty(),
                times
        ));
    }

    public void sendTitle(@NotNull Collection<Audience> audiences, @NotNull Title title) {
        this.send(Audience::showTitle, audiences, title);
    }

    @Override
    public void sendAction(@NotNull Player player, String message) {
        player.sendActionBar(this.getComponent(message));
    }

    public void sendBossbar(@NotNull Collection<Audience> audiences, @NotNull BossBarMessage bossBarMessage, @NotNull Object[] placeholders) {
        BossBar bossBar = BossBar.bossBar(
                this.getComponentWithPlaceholders(bossBarMessage.title(), placeholders),
                bossBarMessage.progress(),
                bossBarMessage.color(),
                bossBarMessage.overlay(),
                bossBarMessage.flags()
        );
        this.send(Audience::showBossBar, audiences, bossBar);
        this.plugin.getFoliaCompatibilityManager()
                .runLaterAsync(() -> this.send(Audience::hideBossBar, audiences, bossBar),
                        bossBarMessage.duration(), TimeUnit.SECONDS);
    }


    @Override
    public String getMessageColorized(String message) {
        return this.colorMiniMessage(message);
    }

    @Override
    public String getMessageLegacyColorized(String message) {
        return this.LEGACY_SERIALIZER.serialize(this.getComponent(message));
    }
}