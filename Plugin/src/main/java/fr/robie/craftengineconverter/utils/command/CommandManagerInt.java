package fr.robie.craftengineconverter.utils.command;

public interface CommandManagerInt {
    /**
     * Registers a command
     * @param command the command to onLoad
     * @return the registered command
     */
    VCommand registerCommand(VCommand command);

    /**
     * Register a command with a single name
     * @param string the command name
     * @param command the command to onLoad
     */
    void registerCommand(String string, VCommand command);

    /**
     * Register a command with aliases
     * @param command the command name
     * @param vCommand the command to onLoad
     * @param aliases the command aliases
     */
    void registerCommand(String command, VCommand vCommand, String... aliases);

    /**
     * Starts the validation of all registered commands
     */
    void validCommands();
}
