package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CraftEngineConverterCommandReload extends SubCommand<CraftEngineConverter> {
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    public CraftEngineConverterCommandReload(CraftEngineConverter plugin) {
        super(plugin, "reload", "rl");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_RELOAD.asPermission());
    }

    @Override
    protected fr.robie.paperdispatch.command.@NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        CommandSender sender = commandDispatch.getSender();
        try {
            long startTime = System.currentTimeMillis();
            commandDispatch.getPlugin().reloadBlockStateMappings();
            commandDispatch.getPlugin().reloadConfig();
            commandDispatch.getPlugin().reloadMessages();
            long endTime = System.currentTimeMillis();
            this.messageFormatter.sendMessage(Message.COMMAND__RELOAD__SUCCESS, sender, Placeholder.of("time", TimerBuilder.formatTimeAuto(endTime - startTime)));
            return CommandResultType.SUCCESS;
        } catch (Exception e) {
            Logger.error("An error occurred while reloading the plugin.", e);
            this.messageFormatter.sendMessage(Message.COMMAND__RELOAD__FAILURE, sender);
            return CommandResultType.FAILURE;
        }
    }
}
