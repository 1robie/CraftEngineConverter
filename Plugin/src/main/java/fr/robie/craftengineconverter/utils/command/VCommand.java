package fr.robie.craftengineconverter.utils.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.format.Message;
import fr.robie.craftengineconverter.utils.collection.CollectionBiConsumer;
import fr.robie.craftengineconverter.utils.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class VCommand extends Arguments {
    record FlagValue(String flag, boolean hasValue){}
    protected final CraftEngineConverter plugin;

    /**
     * Permission used for the command, if it is a null then everyone can
     * execute the command
     */
    private String permission;

    /**
     * Mother command of this command
     */
    protected VCommand parent;

    /**
     * Are all sub commands used
     */
    private final List<String> subCommands = new ArrayList<>();
    protected List<VCommand> subVCommands = new ArrayList<>();

    private final List<String> requireArgs = new ArrayList<>();
    private final List<String> optionalArgs = new ArrayList<>();
    private final List<FlagValue> flagsArgs = new ArrayList<>();
    /**
     * If this variable is false the command will not be able to use this
     * command
     */
    private boolean consoleCanUse = true;

    /**
     * This variable allows to run the main class of the command even with
     * arguments convenient for commands like /ban <player>
     */
    private boolean ignoreParent = false;
    private boolean ignoreArgs = false;
    private boolean extendedArgs = false;
    public boolean runAsync = false;
    private CommandType tabCompleter = CommandType.DEFAULT;

    /**
     * This is the person who executes the command
     */
    protected CommandSender sender;
    protected @Nullable Player player; // Null if console

    private String syntax;
    private String description;
    private int argsMinLength;
    private int argsMaxLength;

    protected Map<Integer, CollectionBiConsumer> tabCompletions = new HashMap<>();

    /**
     * @param plugin
     */
    public VCommand(CraftEngineConverter plugin) {
        super();
        this.plugin = plugin;
    }

    //
    // GETTER
    //

    public Optional<CollectionBiConsumer> getCompletionAt(int index) {
        return Optional.ofNullable(this.tabCompletions.getOrDefault(index, null));
    }

    /**
     * Return command permission
     *
     * @return the permission
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return the parent
     */
    public VCommand getParent() {
        return parent;
    }

    /**
     * @return the subCommand
     */
    public List<String> getSubCommands() {
        return subCommands;
    }

    /**
     * @return the consoleCanUse
     */
    public boolean isConsoleCanUse() {
        return consoleCanUse;
    }

    /**
     * @return the ignoreParent
     */
    public boolean isIgnoreParent() {
        return ignoreParent;
    }

    public CommandSender getSender() {
        return sender;
    }

    /**
     * @return the argsMinLength
     */
    public int getArgsMinLength() {
        return argsMinLength;
    }

    /**
     * @return the argsMaxLength
     */
    public int getArgsMaxLength() {
        return argsMaxLength;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Return the generate or custom syntax
     *
     * @return the syntax
     */
    public String getSyntax() {
        if (syntax == null) {
            syntax = generateDefaultSyntax("");
        }
        return syntax;
    }

    public boolean isIgnoreArgs() {
        return ignoreArgs;
    }

    public String getDescription() {
        return description == null ? "no description" : description;
    }

    public CommandType getTabCompleter() {
        return tabCompleter;
    }

    /*
     *
     */
    protected void setTabCompletor() {
        this.tabCompleter = CommandType.SUCCESS;
    }

    //
    // SETTER
    //

    public void setIgnoreArgs(boolean ignoreArgs) {
        this.ignoreArgs = ignoreArgs;
    }

    public void setIgnoreParent(boolean ignoreParent) {
        this.ignoreParent = ignoreParent;
    }

    public void setExtendedArgs(boolean extendedArgs) {
        this.extendedArgs = extendedArgs;
    }

    /**
     * @param syntax
     *            the syntax to set
     */
    protected VCommand setSyntax(String syntax) {
        this.syntax = syntax;
        return this;
    }

    /**
     * @param permission
     *            the permission to set
     */
    protected VCommand setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    /**
     * @param permission
     *            the permission to set
     */
    protected VCommand setPermission(Permission permission) {
        this.permission = permission.asPermission();
        return this;
    }

    /**
     * @param parent
     *            the parent to set
     */
    protected VCommand setParent(VCommand parent) {
        this.parent = parent;
        return this;
    }

    /**
     * @param consoleCanUse
     *            the consoleCanUse to set
     */
    protected VCommand setConsoleCanUse(boolean consoleCanUse) {
        this.consoleCanUse = consoleCanUse;
        return this;
    }

    protected VCommand onlyPlayers(){
        this.consoleCanUse = false;
        return this;
    }

    /**
     * Mettre la description de la commande
     *
     * @param description
     * @return
     */
    protected VCommand setDescription(String description) {
        this.description = description;
        return this;
    }

    /*
     * Ajouter un argument obligatoire
     */
    protected void addRequireArg(String message) {
        this.requireArgs.add(message);
        this.ignoreParent = this.parent == null;
        this.ignoreArgs = true;
    }

    /*
     * Ajouter un argument obligatoire
     */
    protected void addRequireArg(String message, CollectionBiConsumer runnable) {
        this.addRequireArg(message);
        int index = this.requireArgs.size();
        this.addCompletion(index - 1, runnable);
    }

    /**
     * Ajouter un argument optionel
     *
     * @param message
     */
    protected void addOptionalArg(String message, CollectionBiConsumer runnable) {
        this.addOptionalArg(message);
        int index = this.requireArgs.size() + this.optionalArgs.size();
        this.addCompletion(index - 1, runnable);
    }

    /**
     * Ajouter un argument optionel
     *
     * @param message
     */
    protected void addOptionalArg(String message) {
        this.optionalArgs.add(message);
        this.ignoreParent = this.parent == null;
        this.ignoreArgs = true;
    }

    protected void addFlag(@NotNull String flag) {
        this.flagsArgs.add(new FlagValue(flag, false));
    }

    /**
     * Mettre la description de la commande
     *
     * @param description
     * @return
     */
    protected VCommand setDescription(Message description) {
        this.description = description.getMessage();
        return this;
    }

    /**
     *
     * @return first command
     */
    public String getFirst() {
        return this.subCommands.getFirst();
    }

    //
    // OTHER
    //

    /**
     * Adds sub orders
     *
     * @param subCommand
     * @return this
     */
    public VCommand addSubCommand(String subCommand) {
        this.subCommands.add(subCommand);
        return this;
    }

    /**
     * Adds sub orders
     *
     * @param command
     * @return this
     */
    public VCommand addSubCommand(VCommand command) {
        command.setParent(this);
        this.plugin.getCommandManager().registerCommand(command);
        this.subVCommands.add(command);
        return this;
    }

    /**
     * Adds sub orders
     *
     * @param subCommand
     * @return this
     */
    public VCommand addSubCommand(String... subCommand) {
        this.subCommands.addAll(Arrays.asList(subCommand));
        return this;
    }

    /**
     * Add a {@link CollectionBiConsumer} to the index for the tab completion
     *
     * @param index
     * @param runnable
     */
    public void addCompletion(int index, CollectionBiConsumer runnable) {
        this.tabCompletions.put(index, runnable);
        this.setTabCompletor();
    }

    /**
     * Allows you to generate the syntax of the command manually But you you can
     * set it yourself with the setSyntax()
     *
     * @param syntax
     * @return generate syntax
     */
    private String generateDefaultSyntax(String syntax) {
        boolean update = syntax.isEmpty();

        StringBuilder syntaxBuilder = new StringBuilder();
        if (update) {
            appendRequiredArguments(syntaxBuilder);
            appendOptionalArguments(syntaxBuilder);
            appendFlags(syntaxBuilder);
            syntax = syntaxBuilder.toString().trim();
        }

        String tmpString = subCommands.getFirst() + syntax;
        return parent == null ? "/" + tmpString : parent.generateDefaultSyntax(" " + tmpString);
    }

    private void appendRequiredArguments(StringBuilder syntaxBuilder) {
        requireArgs.forEach(arg -> syntaxBuilder.append(" <").append(arg).append(">"));
    }

    private void appendOptionalArguments(StringBuilder syntaxBuilder) {
        optionalArgs.forEach(arg -> syntaxBuilder.append(" [<").append(arg).append(">]"));
    }

    private void appendFlags(StringBuilder syntaxBuilder) {
        flagsArgs.forEach(flag -> syntaxBuilder.append(" [").append(flag.flag).append("]"));
    }

    /**
     * Allows to know the number of parents in a recursive way
     *
     * @param defaultParent
     * @return
     */
    private int parentCount(int defaultParent) {
        return parent == null ? defaultParent : parent.parentCount(defaultParent + 1);
    }

    /**
     * Allows you to manage the arguments and check that the command is valid
     *
     * @param plugin
     * @param commandSender
     * @param args
     * @return
     */
    public CommandType prePerform(CraftEngineConverter plugin, CommandSender commandSender, String[] args) {

        this.parentCount = this.parentCount(0);

        String[] cleanedArgs = parseFlags(args);

        this.argsMaxLength = this.requireArgs.size() + this.optionalArgs.size() + this.parentCount;
        this.argsMinLength = this.requireArgs.size() + this.parentCount;

        if (this.syntax == null) {
            this.syntax = generateDefaultSyntax("");
        }

        this.args = cleanedArgs;

        String defaultString = super.argAsString(0);

        if (defaultString != null) {
            for (VCommand subCommand : subVCommands) {
                if (subCommand.getSubCommands().contains(defaultString.toLowerCase()))
                    return CommandType.CONTINUE;
            }
        }

        if ((this.argsMinLength != 0 && cleanedArgs.length < this.argsMinLength) ||
                this.argsMaxLength != 0 && cleanedArgs.length > this.argsMaxLength && !this.extendedArgs) {
            return CommandType.SYNTAX_ERROR;
        }

        this.sender = commandSender;
        if (this.sender instanceof Player playerInstance) {
            this.player = playerInstance;
        } else {
            this.player = null;
        }

        try {
            return perform(plugin);
        } catch (Exception e) {
            return CommandType.SYNTAX_ERROR;
        }
    }

    private String[] parseFlags(String[] args) {
        this.flags.clear();
        List<String> cleanedArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            boolean isFlag = false;
            for (FlagValue flag : this.flagsArgs) {
                String flagKey = flag.flag;
                if (arg.equals(flagKey)) {
                    isFlag = true;

                    if (flag.hasValue && i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        this.flags.put(flagKey, args[i + 1]);
                        i++;
                    } else {
                        this.flags.put(flagKey, "true");
                    }
                    break;
                } else if (arg.startsWith(flagKey + "=")) {
                    isFlag = true;
                    String value;
                    if (flag.hasValue) {
                        value = arg.substring((flag + "=").length());
                    } else {
                        value = "true";
                    }
                    this.flags.put(flagKey, value);
                    break;
                }
            }

            if (!isFlag) {
                cleanedArgs.add(arg);
            }
        }

        return cleanedArgs.toArray(new String[0]);
    }

    /**
     * Method that allows you to execute the command
     */
    protected abstract CommandType perform(CraftEngineConverter plugin);

    public boolean sameSubCommands() {
        if (this.parent == null) {
            return false;
        }
        for (String command : this.subCommands) {
            if (this.parent.getSubCommands().contains(command))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "VCommand [permission=" + permission + ", subCommands=" + subCommands + ", consoleCanUse="
                + consoleCanUse + ", description=" + description + "]";
    }

    /**
     * Generate tab completion
     *
     * @param plugin plugin
     * @param sender sender
     * @param args arguments
     * @return list of completions or null
     */
    public List<String> toTab(CraftEngineConverter plugin, CommandSender sender, String[] args) {

        this.parentCount = this.parentCount(0);

        TabParseResult parseResult = parseArgsForTab(args);

        int currentIndex = (parseResult.cleanedArgs.size() - this.parentCount) - 1;
        String lastArg = args[args.length - 1];

        Optional<CollectionBiConsumer> optional = this.getCompletionAt(currentIndex);
        List<String> completions = optional.map(consumer ->
                this.generateList(consumer.accept(sender, args), lastArg)
        ).orElse(null);

        List<String> availableFlags = getAvailableFlags(parseResult.usedFlags, lastArg);

        return combineCompletions(completions, availableFlags, currentIndex);
    }

    private TabParseResult parseArgsForTab(String[] args) {
        Set<String> usedFlags = new HashSet<>();
        List<String> cleanedArgs = new ArrayList<>();

        Set<String> flagSet = this.flagsArgs.stream()
                .map(f -> f.flag)
                .collect(Collectors.toSet());

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (flagSet.contains(arg)) {
                usedFlags.add(arg);

                FlagValue flagValue = findFlag(arg);
                if (flagValue != null && flagValue.hasValue &&
                        i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    i++;
                }
            } else if (arg.contains("=")) {
                String flagPart = arg.substring(0, arg.indexOf('='));
                if (flagSet.contains(flagPart)) {
                    usedFlags.add(flagPart);
                } else {
                    cleanedArgs.add(arg);
                }
            } else {
                cleanedArgs.add(arg);
            }
        }

        return new TabParseResult(usedFlags, cleanedArgs);
    }

    private FlagValue findFlag(String flag) {
        return this.flagsArgs.stream()
                .filter(f -> f.flag.equals(flag))
                .findFirst()
                .orElse(null);
    }

    private List<String> getAvailableFlags(Set<String> usedFlags, String filter) {
        if (this.flagsArgs.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerFilter = filter.toLowerCase();

        return this.flagsArgs.stream()
                .map(f -> f.flag)
                .filter(flag -> !usedFlags.contains(flag))
                .filter(flag -> filter.isEmpty() || flag.toLowerCase().startsWith(lowerFilter))
                .collect(Collectors.toList());
    }

    private List<String> combineCompletions(List<String> completions, List<String> flags, int currentIndex) {
        boolean canShowFlags = currentIndex >= this.requireArgs.size() && !flags.isEmpty();

        if (!canShowFlags) {
            return completions;
        }

        if (completions == null || completions.isEmpty()) {
            return flags;
        }

        List<String> combined = new ArrayList<>(flags);
        combined.addAll(completions);
        return combined;
    }

    /**
     * Classe interne pour stocker les résultats du parsing
     */
    private static class TabParseResult {
        final Set<String> usedFlags;
        final List<String> cleanedArgs;

        TabParseResult(Set<String> usedFlags, List<String> cleanedArgs) {
            this.usedFlags = usedFlags;
            this.cleanedArgs = cleanedArgs;
        }
    }
    /**
     * Generate list for tab completer
     *
     * @param startWith
     * @param strings
     * @return
     */
    protected List<String> generateList(String startWith, String... strings) {
        return generateList(Arrays.asList(strings), startWith);
    }

    /**
     * Generate list for tab completer
     *
     * @param startWith
     * @param strings
     * @return
     */
    protected List<String> generateList(Tab tab, String startWith, String... strings) {
        return generateList(Arrays.asList(strings), startWith, tab);
    }

    /**
     * Generate list for tab completer
     *
     * @param defaultList
     * @param startWith
     * @return
     */
    protected List<String> generateList(Collection<String> defaultList, String startWith) {
        return generateList(defaultList, startWith, Tab.CONTAINS);
    }

    /**
     * Generate list for tab completer
     *
     * @param defaultList
     * @param startWith
     * @param tab
     * @return
     */
    protected List<String> generateList(Collection<String> defaultList, String startWith, Tab tab) {
        List<String> newList = new ArrayList<>();
        for (String str : defaultList) {
            if (startWith.isEmpty()
                    || (tab.equals(Tab.START) ? str.toLowerCase().startsWith(startWith.toLowerCase())
                    : str.toLowerCase().contains(startWith.toLowerCase()))) {
                newList.add(str);
            }
        }
        return newList.isEmpty() ? null : newList;
    }

    /**
     * Add list of aliases
     *
     * @param aliases
     */
    public void addSubCommand(List<String> aliases) {
        this.subCommands.addAll(aliases);
    }

    /**
     * Allows to send the syntax of the commands
     *
     */
    public void syntaxMessage() {
        this.subVCommands.forEach(command -> {
            if (command.getPermission() == null || hasPermission(sender, command.getPermission())) {
                message(plugin,this.sender, Message.COMMAND_SYNTAXE_HELP, "syntax", command.getSyntax(), "description",
                        command.getDescription());
            }
        });
    }
}
