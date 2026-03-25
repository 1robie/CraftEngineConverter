package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;

public class CraftEngineConverterCommandWorldConverter extends VCommand {

    public CraftEngineConverterCommandWorldConverter(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_WORLDCONVERTER);
        this.setDescription(Message.COMMAND__WORLD_CONVERTER__DESCRIPTION);
        this.addSubCommand("worldconverter", "wc");
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterClearCachedChunks(plugin));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterStart(plugin));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverterRestore(plugin));
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        this.syntaxMessage();
        return CommandType.SUCCESS;
    }
}
