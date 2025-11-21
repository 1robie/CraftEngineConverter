package fr.robie.craftEngineConverter.command;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.core.utils.command.CommandType;
import fr.robie.craftEngineConverter.core.utils.command.VCommand;
import fr.robie.craftEngineConverter.core.utils.permission.Permission;

public class CraftEngineConverterCommand extends VCommand {
    public CraftEngineConverterCommand(CraftEngineConverter craftEngineConverter) {
        super(craftEngineConverter);
        this.setPermission(Permission.COMMAND_USE);
        this.addSubCommand(new CraftEngineConverterCommandReload(craftEngineConverter));
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        syntaxMessage();
        return CommandType.SUCCESS;
    }
}
