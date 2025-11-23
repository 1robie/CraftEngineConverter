package fr.robie.craftEngineConverter.command;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.utils.builder.TimerBuilder;
import fr.robie.craftEngineConverter.utils.command.CommandType;
import fr.robie.craftEngineConverter.utils.command.VCommand;
import fr.robie.craftEngineConverter.utils.format.Message;
import fr.robie.craftEngineConverter.utils.permission.Permission;

public class CraftEngineConverterCommandReload extends VCommand {
    public CraftEngineConverterCommandReload(CraftEngineConverter plugin) {
        super(plugin);
        this.addSubCommand("reload","rl");
        this.setPermission(Permission.COMMAND_RELOAD);
        this.setDescription(Message.DESCRIPTION_COMMAND_RELOAD);
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        long startTime = System.currentTimeMillis();
        plugin.reloadConfig();
        plugin.reloadFiles();
        long endTime = System.currentTimeMillis();
        message(plugin,sender, Message.COMMAND_RELOAD_SUCCESS,"time",TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND));
        return CommandType.SUCCESS;
    }
}
