package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.utils.builder.TimerBuilder;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.craftengineconverter.utils.format.Message;
import fr.robie.craftengineconverter.utils.permission.Permission;

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
