package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import org.jetbrains.annotations.NotNull;

public class CraftEngineConverterCommandWorldConverter extends SubCommand<CraftEngineConverter> {

    public CraftEngineConverterCommandWorldConverter(CraftEngineConverter plugin) {
        super(plugin, "worldconverter", "wc");
        this.setPermission(Permission.COMMAND_WORLDCONVERTER.asPermission());
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterClearCachedChunks(plugin));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterStart(plugin));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterRestore(plugin));
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        return CommandResultType.SUCCESS;
    }
}
