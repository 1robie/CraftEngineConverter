package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.manager.FileCacheManager;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;

public class CraftEngineConverterCommandClearFilesCache extends VCommand {
    public CraftEngineConverterCommandClearFilesCache(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_CLEARFILESCACHE);
        this.setDescription(Message.COMMAND__CLEAR_FILES_CACHE__DESCRIPTION);
        this.addSubCommand("clearfilescache");
        this.addFlag("--all");
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        boolean clearAll = this.containFlag("--all");
        long startTime = System.currentTimeMillis();
        long clearedFiles;
        if (clearAll) {
            clearedFiles = FileCacheManager.getTotalSize();
            FileCacheManager.invalidateAllCaches();
        } else {
            clearedFiles = FileCacheManager.cleanStaleEntries();
        }
        this.message(this.plugin, this.sender, Message.COMMAND__CLEAR_FILES_CACHE__COMPLETE, "cleared_files", clearedFiles, "time", TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTime));
        return CommandType.SUCCESS;
    }
}
