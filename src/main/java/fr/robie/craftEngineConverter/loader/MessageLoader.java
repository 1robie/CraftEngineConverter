package fr.robie.craftEngineConverter.loader;

import fr.robie.craftEngineConverter.core.utils.YamlUtils;
import fr.robie.craftEngineConverter.core.utils.format.Message;
import fr.robie.craftEngineConverter.core.utils.format.MessageType;
import fr.robie.craftEngineConverter.core.utils.save.Persist;
import fr.robie.craftEngineConverter.core.utils.save.Savable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageLoader extends YamlUtils implements Savable {
    private final List<Message> loadedMessages = new ArrayList<>();

    public MessageLoader(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void save(Persist persist) {

        if (persist != null) return;

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        YamlConfiguration configuration = getConfig(file);
        for (Message message : Message.values()) {

            if (!message.isUse()) continue;

            String path = message.name().toLowerCase().replace("_", "-");

            if (configuration.contains(path)) continue;

            if (message.getType() != MessageType.TCHAT) {
                configuration.set(path + ".type", message.getType().name());
            }
            if (message.getType().equals(MessageType.TCHAT) || message.getType().equals(MessageType.ACTION) || message.getType().equals(MessageType.CENTER)) {

                if (message.isMessage()) {
                    if (message.getType() != MessageType.TCHAT) {
                        configuration.set(path + ".messages", colorReverse(message.getMessages()));
                    } else {
                        configuration.set(path, colorReverse(message.getMessages()));
                    }
                } else {
                    if (message.getType() != MessageType.TCHAT) {
                        configuration.set(path + ".message", colorReverse(message.getMessage()));
                    } else {
                        configuration.set(path, colorReverse(message.getMessage()));
                    }
                }
            } else if (message.getType().equals(MessageType.TITLE)) {

                configuration.set(path + ".title", colorReverse(message.getTitle()));
                configuration.set(path + ".subtitle", colorReverse(message.getSubTitle()));
                configuration.set(path + ".fadeInTime", message.getStart());
                configuration.set(path + ".showTime", message.getTime());
                configuration.set(path + ".fadeOutTime", message.getEnd());
            }
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        loadMessages(configuration);
    }

    /**
     * Loads messages from the configuration file.
     *
     * @param persist The persist instance used for loading the data.
     */
    @Override
    public void load(Persist persist) {

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            this.save(null);
            return;
        }

        YamlConfiguration configuration = getConfig(file);
        this.save(null);

        loadMessages(configuration);
    }

    private void loadMessages(YamlConfiguration configuration) {

        this.loadedMessages.clear();

        for (String key : configuration.getKeys(false)) {
            loadMessage(configuration, key);
        }

        boolean canSave = false;
        for (Message message : Message.values()) {

            if (!this.loadedMessages.contains(message) && message.isUse()) {
                canSave = true;
                break;
            }
        }

        if (canSave) {
            this.save(null);
        }
    }

    /**
     * Loads a single message from the given YAML configuration.
     *
     * @param configuration The YAML configuration to load the message from.
     * @param key           The key under which the message is stored.
     */
    private void loadMessage(YamlConfiguration configuration, String key) {
        try {

            Message message = Message.valueOf(key.toUpperCase().replace("-", "_"));

            if (configuration.contains(key + ".type")) {

                MessageType messageType = MessageType.valueOf(configuration.getString(key + ".type", "TCHAT").toUpperCase());
                message.setType(messageType);
                switch (messageType) {
                    case ACTION:
                    case TCHAT_AND_ACTION: {
                        message.setMessage(configuration.getString(key + ".message"));
                        break;
                    }
                    case CENTER:
                    case TCHAT:
                    case WITHOUT_PREFIX: {
                        List<String> messages = configuration.getStringList(key + ".messages");
                        if (messages.isEmpty()) {
                            message.setMessage(configuration.getString(key + ".message"));
                        } else message.setMessages(messages);
                        break;
                    }
                    case TITLE: {
                        String title = configuration.getString(key + ".title");
                        String subtitle = configuration.getString(key + ".subtitle");
                        int fadeInTime = configuration.getInt(key + ".fadeInTime");
                        int showTime = configuration.getInt(key + ".showTime");
                        int fadeOutTime = configuration.getInt(key + ".fadeOutTime");
                        Map<String, Object> titles = new HashMap<>();
                        titles.put("title", color(title));
                        titles.put("subtitle", color(subtitle));
                        titles.put("start", fadeInTime);
                        titles.put("time", showTime);
                        titles.put("end", fadeOutTime);
                        titles.put("isUse", true);
                        message.setTitles(titles);
                        break;
                    }
                }

            } else {
                message.setType(MessageType.TCHAT);
                List<String> messages = configuration.getStringList(key);
                if (messages.isEmpty()) {
                    message.setMessage(configuration.getString(key));
                } else message.setMessages(messages);
            }

            this.loadedMessages.add(message);
        } catch (Exception ignored) {
        }
    }

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
    protected String colorReverse(String message) {
        Pattern pattern = Pattern.compile(net.md_5.bungee.api.ChatColor.COLOR_CHAR + "x[a-fA-F0-9-" + net.md_5.bungee.api.ChatColor.COLOR_CHAR + "]{12}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            String colorReplace = color.replace("§x", "#");
            colorReplace = colorReplace.replace("§", "");
            message = message.replace(color, colorReplace);
            matcher = pattern.matcher(message);
        }

        return message == null ? null : message.replace("§", "&");
    }
    protected List<String> colorReverse(List<String> messages) {
        return messages.stream().map(this::colorReverse).collect(Collectors.toList());
    }
}
