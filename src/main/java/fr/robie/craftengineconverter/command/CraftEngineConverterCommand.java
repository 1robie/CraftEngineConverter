package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.craftengineconverter.utils.permission.Permission;

public class CraftEngineConverterCommand extends VCommand {
    public CraftEngineConverterCommand(CraftEngineConverter craftEngineConverter) {
        super(craftEngineConverter);
        this.setPermission(Permission.COMMAND_USE);
        this.addSubCommand(new CraftEngineConverterCommandReload(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandConvert(craftEngineConverter));
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        syntaxMessage();
        return CommandType.SUCCESS;
    }
}
