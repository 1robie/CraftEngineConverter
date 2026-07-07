package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.listener.WorldConverterManager;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import org.jetbrains.annotations.NotNull;

public class CraftEngineConverterCommandWorldConverterClearCachedChunks extends SubCommand<CraftEngineConverter> {
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    public CraftEngineConverterCommandWorldConverterClearCachedChunks(CraftEngineConverter plugin) {
        super(plugin, "clear-cached-chunks");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_CLEAR_CACHED_CHUNKS.asPermission());
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        long startTime = System.currentTimeMillis();
        WorldConverterManager worldConverterManager = this.plugin.getWorldConverterManager();

        int clearedChunks = worldConverterManager.getProcessedChunksCount();
        worldConverterManager.clearProcessedChunks();

        long endTime = System.currentTimeMillis();

        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__COMPLETE, commandDispatch.getSender(),
                Placeholder.of("chunks", String.valueOf(clearedChunks),
                        "time", TimerBuilder.formatTimeAuto(endTime - startTime)));

        return CommandResultType.SUCCESS;
    }
}
