package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;

import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;

public class CraftEngineConverterCommandReload extends VCommand {
    public CraftEngineConverterCommandReload(CraftEngineConverter plugin) {
        super(plugin);
        this.addSubCommand("reload", "rl");
        this.setPermission(Permission.COMMAND_RELOAD);
        this.setDescription(Message.COMMAND__RELOAD__DESCRIPTION);
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        try {
            long startTime = System.currentTimeMillis();
            plugin.reloadBlockStateMappings();
            plugin.reloadConfig();
            plugin.reloadMessages();
            long endTime = System.currentTimeMillis();
            this.messageFormatter.sendMessage(Message.COMMAND__RELOAD__SUCCESS, this.sender, Placeholder.of("time", TimerBuilder.formatTimeAuto(endTime - startTime)));
        } catch (Exception e) {
            Logger.error("An error occurred while reloading the plugin.", e);
            this.messageFormatter.sendMessage(Message.COMMAND__RELOAD__FAILURE, this.sender);
        }
        return CommandType.SUCCESS;
    }
}
