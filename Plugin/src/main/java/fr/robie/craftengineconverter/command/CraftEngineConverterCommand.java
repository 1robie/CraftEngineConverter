package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.bedrock.BedrockConverter;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;

public class CraftEngineConverterCommand extends VCommand {
    private final BedrockConverter bedrockConverter;

    public CraftEngineConverterCommand(CraftEngineConverter craftEngineConverter) {
        super(craftEngineConverter);
        this.bedrockConverter = new BedrockConverter(craftEngineConverter.getDataFolder());
        this.setPermission(Permission.COMMAND_USE);
        this.addSubCommand(new CraftEngineConverterCommandReload(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandConvert(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandClearFilesCache(craftEngineConverter));
        this.addSubCommand(new CraftEngineConverterCommandWorldConverter(craftEngineConverter));
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        this.syntaxMessage();

        this.bedrockConverter.convert();

        return CommandType.SUCCESS;
    }
}
