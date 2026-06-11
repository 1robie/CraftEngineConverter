package fr.robie.craftengineconverter.utils.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.format.Message;

import fr.robie.craftengineconverter.utils.CraftEngineConverterUtils;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CommandManager extends CraftEngineConverterUtils implements CommandExecutor, TabCompleter, CommandManagerInt {
    private static CommandMap commandMap;
    private static Constructor<? extends PluginCommand> constructor;

    private MessageFormatter<?,?> messageFormatter;

    static {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
        } catch (Exception ignored) {
        }
    }

    private final CraftEngineConverter plugin;
    private final List<VCommand> commands = new ArrayList<VCommand>();

    /**
     * F
     *
     * @param corePlugin
     */
    public CommandManager(CraftEngineConverter corePlugin) {
        this.plugin = corePlugin;
    }

    /**
     * Load all commands by calling their onLoad method recursively.
     * This method should be called when the plugin is being loaded.
     */
    public void loadCommands() {
        this.messageFormatter = this.plugin.getMessageFormatter();
        this.applyToCommands(VCommand::onLoad);
    }

    /**
     * Enable all commands by calling their onEnable method recursively.
     * This method should be called when the plugin is being enabled.
     */
    public void enableCommands() {
        this.applyToCommands(VCommand::onEnable);
    }

    /**
     * Disable all commands by calling their onDisable method recursively.
     * This method is called when the plugin is being disabled.
     */
    public void disableCommands() {
        this.applyToCommands(VCommand::onDisable);
    }


    /**
     * Apply a consumer action to all registered commands recursively with simple start/end logging.
     * The provided action should be safe (exceptions are caught and logged).
     *
     * @param action action to apply to each VCommand
     */
    private void applyToCommands(Consumer<VCommand> action) {
        for (VCommand command : this.commands) {
            this.applyRecursive(command, action);
        }
    }

    private void applyRecursive(VCommand command, Consumer<VCommand> action) {
        if (command == null) {
            return;
        }
        try {
            action.accept(command);
        } catch (Exception e) {
            Logger.error("Error while applying command action to: " + command.getSyntax() + " - " + e.getMessage());
        }
        List<VCommand> children = command.getSubVCommands();
        if (children != null && !children.isEmpty()) {
            for (VCommand child : children) {
                this.applyRecursive(child, action);
            }
        }
    }

    /**
     * Valid commands
     */
    @Override
    public void validCommands() {
        Logger.info("Loading %count% commands", Placeholder.of("count", String.valueOf(this.getUniqueCommand())));
        this.commandChecking();
        Logger.info("Commands loaded successfully");
    }

    /**
     *
     * @param command
     * @return
     */
    @Override
    public VCommand registerCommand(VCommand command) {
        this.commands.add(command);
        return command;
    }

    @Override
    public void registerCommand(String string, VCommand command) {
        this.registerCommand(this.plugin, string, command, new ArrayList<>());
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        for (VCommand command : this.commands) {
            if (command.getSubCommands().contains(cmd.getName().toLowerCase(Locale.ROOT))) {
                if ((args.length == 0 || command.isIgnoreParent()) && command.getParent() == null) {
                    CommandType type = this.processRequirements(command, sender, args);
                    if (!type.equals(CommandType.CONTINUE)) {
                        return true;
                    }
                }
            } else if (args.length >= 1 && command.getParent() != null
                    && this.canExecute(args, cmd.getName().toLowerCase(Locale.ROOT), command)) {
                CommandType type = this.processRequirements(command, sender, args);
                if (!type.equals(CommandType.CONTINUE)) {
                    return true;
                }
            }
        }
        this.messageFormatter.sendMessage(Message.COMMAND__NO_ARGS, sender);
        return true;
    }

    /**
     * @param args
     * @param cmd
     * @param command
     * @return true if can execute
     */
    private boolean canExecute(String[] args, String cmd, VCommand command) {
        for (int index = args.length - 1; index > -1; index--) {
            if (command.getSubCommands().contains(args[index].toLowerCase(Locale.ROOT))) {
                if (command.isIgnoreArgs()
                        && (command.getParent() == null || this.canExecute(args, cmd, command.getParent(), index - 1))) {
                    return true;
                }
                if (index < args.length - 1) {
                    return false;
                }
                return this.canExecute(args, cmd, command.getParent(), index - 1);
            }
        }
        return false;
    }

    /**
     * @param args
     * @param cmd
     * @param command
     * @param index
     * @return
     */
    private boolean canExecute(String[] args, String cmd, VCommand command, int index) {
        if (index < 0 && command.getSubCommands().contains(cmd.toLowerCase(Locale.ROOT))) {
            return true;
        } else if (index < 0) {
            return false;
        } else if (command.getSubCommands().contains(args[index].toLowerCase(Locale.ROOT))) {
            return this.canExecute(args, cmd, command.getParent(), index - 1);
        }
        return false;
    }

    /**
     * Allows you to process an order. First we check if the sender has the
     * permission or if the command has a permission. If yes then we execute the
     * command otherwise we send the message for the permission
     *
     * @param command - Object that contains the command
     * @param sender  - Person who executes the command
     * @param strings - Argument of the command
     * @return CommandType - Return of the command
     */
    private CommandType processRequirements(VCommand command, CommandSender sender, String[] strings) {

        if (!(sender instanceof Player) && !command.isConsoleCanUse()) {
            this.messageFormatter.sendMessage(Message.COMMAND__PLAYER_ONLY, sender);
            return CommandType.DEFAULT;
        }

        if (command.getPermission() == null || this.hasPermission(sender, command.getPermission())) {

            if (command.runAsync) {
                super.runAsync(this.plugin, () -> {
                    CommandType returnType = command.prePerform(this.plugin, sender, strings);
                    if (returnType == CommandType.SYNTAX_ERROR) {
                        this.messageFormatter.sendMessage(Message.COMMAND__SYNTAX__ERROR, sender, Placeholder.of("syntax", command.getSyntax()));
                    }
                });
                return CommandType.DEFAULT;
            }

            CommandType returnType = command.prePerform(this.plugin, sender, strings);
            if (returnType == CommandType.SYNTAX_ERROR) {
                this.messageFormatter.sendMessage(Message.COMMAND__SYNTAX__ERROR, sender, Placeholder.of("syntax", command.getSyntax()));
            }
            return returnType;
        }
        this.messageFormatter.sendMessage(Message.COMMAND__NO_PERMISSION, sender);
        return CommandType.DEFAULT;
    }

    public List<VCommand> getCommands() {
        return this.commands;
    }

    private int getUniqueCommand() {
        return (int) this.commands.stream().filter(command -> command.getParent() == null).count();
    }

    /**
     * @param command
     * @param commandString
     * @return
     */
    public boolean isValid(VCommand command, String commandString) {
        return command.getParent() != null ? this.isValid(command.getParent(), commandString)
                : command.getSubCommands().contains(commandString.toLowerCase(Locale.ROOT));
    }

    /**
     * Allows you to check if all commands are correct If an command does not
     * have
     */
    private void commandChecking() {
        this.commands.forEach(command -> {
            if (command.sameSubCommands()) {
                Logger.error(command + " command to an argument similar to its parent command !");
                this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String str, String[] args) {

        for (VCommand command : this.commands) {

            if (command.getSubCommands().contains(cmd.getName().toLowerCase(Locale.ROOT))) {
                if (args.length == 1 && command.getParent() == null) {
                    return this.proccessTab(sender, command, args);
                }
            } else {
                String[] newArgs = Arrays.copyOf(args, args.length - 1);
                if (newArgs.length >= 1 && command.getParent() != null
                        && this.canExecute(newArgs, cmd.getName().toLowerCase(Locale.ROOT), command)) {
                    return this.proccessTab(sender, command, args);
                }
            }
        }

        return null;
    }

    /**
     * Allows to execute the tab completion
     *
     * @param sender
     * @param command
     * @param args
     * @return
     */
    private List<String> proccessTab(CommandSender sender, VCommand command, String[] args) {

        CommandType type = command.getTabCompleter();
        if (type.equals(CommandType.DEFAULT)) {
            String startWith = args[args.length - 1];

            List<String> tabCompleter = new ArrayList<>();
            for (VCommand vCommand : this.commands) {
                if ((vCommand.getParent() != null && vCommand.getParent() == command)) {
                    for (String subCmd : vCommand.getSubCommands()) {
                        if (vCommand.getPermission() == null || sender.hasPermission(vCommand.getPermission())) {
                            if (startWith.isEmpty() || subCmd.startsWith(startWith)) {
                                tabCompleter.add(subCmd);
                            }
                        }
                    }
                }
            }
            return tabCompleter.isEmpty() ? null : tabCompleter;

        } else if (type.equals(CommandType.SUCCESS)) {
            return command.toTab(this.plugin, sender, args);
        }

        return null;
    }

    @Override
    public void registerCommand(String command, VCommand vCommand, String... aliases) {
        this.registerCommand(this.plugin, command, vCommand, Arrays.asList(aliases));
    }

    /**
     * Register spigot command without plugin.yml This method will allow to
     * onLoad a command in the spigot without using the plugin.yml This saves
     * time and understanding, the plugin.yml file is clearer
     *
     * @param string   - Main command
     * @param vCommand - Command object
     * @param aliases  - Command aliases
     */
    public void registerCommand(Plugin plugin, String string, VCommand vCommand, List<String> aliases) {
        try {
            PluginCommand command = constructor.newInstance(string, this.plugin);
            command.setExecutor(this);
            command.setTabCompleter(this);
            if (aliases != null && !aliases.isEmpty()) {
                command.setAliases(aliases);
            }


            this.commands.add(vCommand.addSubCommand(string));
            vCommand.addSubCommand(aliases);

            if (!commandMap.register(command.getName(), plugin.getPluginMeta().getName(), command)) {
                Logger.info("Unable to add the command " + vCommand.getSyntax());
            }
        } catch (Exception exception) {
            Logger.error("Error while registering command " + vCommand.getSyntax(), exception);
        }
    }
}
