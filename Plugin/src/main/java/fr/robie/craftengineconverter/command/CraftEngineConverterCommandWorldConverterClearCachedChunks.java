package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.listener.WorldConverterManager;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.messageflow.formatter.Placeholder;

public class CraftEngineConverterCommandWorldConverterClearCachedChunks extends VCommand {
    public CraftEngineConverterCommandWorldConverterClearCachedChunks(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_CLEAR_CACHED_CHUNKS);
        this.setDescription(Message.COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__DESCRIPTION);
        this.addSubCommand("clear-cached-chunks");
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        long startTime = System.currentTimeMillis();
        WorldConverterManager worldConverterManager = plugin.getWorldConverterManager();

        int clearedChunks = worldConverterManager.getProcessedChunksCount();
        worldConverterManager.clearProcessedChunks();

        long endTime = System.currentTimeMillis();

        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__CLEAR_CACHED_CHUNKS__COMPLETE, this.sender,
                Placeholder.of("chunks", String.valueOf(clearedChunks),
                        "time", TimerBuilder.formatTimeAuto(endTime - startTime)));

        return CommandType.SUCCESS;
    }
}
