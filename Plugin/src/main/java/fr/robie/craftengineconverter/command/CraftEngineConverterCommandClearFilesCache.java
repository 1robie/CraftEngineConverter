package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import org.jetbrains.annotations.NotNull;

public class CraftEngineConverterCommandClearFilesCache extends SubCommand<CraftEngineConverter> {
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    public CraftEngineConverterCommandClearFilesCache(CraftEngineConverter plugin) {
        super(plugin, "clearfilescache", "cfc");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_CLEARFILESCACHE.asPermission());
        this.addFlag("all");
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        long startTime = System.currentTimeMillis();
        long clearedFiles;
        if (commandDispatch.hasFlag("all")) {
            clearedFiles = FileCacheManager.getTotalSize();
            FileCacheManager.invalidateAllCaches();
        } else {
            clearedFiles = FileCacheManager.cleanStaleEntries();
        }
        this.messageFormatter.sendMessage(Message.COMMAND__CLEAR_FILES_CACHE__COMPLETE, commandDispatch.getSender(), Placeholder.of("cleared_files", String.valueOf(clearedFiles), "time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTime)));
        return CommandResultType.SUCCESS;
    }
}
