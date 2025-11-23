package fr.robie.craftEngineConverter.utils.command;

public interface CommandManagerInt {
    /**
     * Registers a command
     * @param command the command to register
     * @return the registered command
     */
    VCommand registerCommand(VCommand command);

    /**
     * Register a command with a single name
     * @param string the command name
     * @param command the command to register
     */
    void registerCommand(String string, VCommand command);

    /**
     * Register a command with aliases
     * @param command the command name
     * @param vCommand the command to register
     * @param aliases the command aliases
     */
    void registerCommand(String command, VCommand vCommand, String... aliases);

    /**
     * Starts the validation of all registered commands
     */
    void validCommands();
}
