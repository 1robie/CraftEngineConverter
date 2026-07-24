package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.paperdispatch.command.BaseCommand;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import org.jetbrains.annotations.NotNull;

public class CraftEngineConverterCommand extends BaseCommand<CraftEngineConverter> {

    public CraftEngineConverterCommand(CraftEngineConverter craftEngineConverter) {
        super(craftEngineConverter, "craftengineconverter", "cengineconverter", "cec");
        this.setPermission(Permission.COMMAND_USE.asPermission());
        this.addSubCommand(new CraftEngineConverterCommandReload(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandConvert(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandClearFilesCache(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverter(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandBedrock(craftEngineConverter));
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        return CommandResultType.SUCCESS;
    }
}
