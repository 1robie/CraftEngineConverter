package fr.robie.craftEngineConverter.command;

import fr.robie.craftEngineConverter.CraftEngineConverter;
import fr.robie.craftEngineConverter.converter.Converter;
import fr.robie.craftEngineConverter.utils.builder.TimerBuilder;
import fr.robie.craftEngineConverter.utils.command.CommandType;
import fr.robie.craftEngineConverter.utils.command.VCommand;
import fr.robie.craftEngineConverter.utils.enums.ConverterOptions;
import fr.robie.craftEngineConverter.utils.format.Message;
import fr.robie.craftEngineConverter.utils.permission.Permission;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CraftEngineConverterCommandConvert extends VCommand {
    public CraftEngineConverterCommandConvert(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_RELOAD);
        this.setDescription(Message.DESCRIPTION_COMMAND_CONVERT);
        this.addSubCommand("convert");
        this.addOptionalArg("plugin",(sender,args)-> this.plugin.getConverterNames());
        this.addOptionalArg("type", (sender,args)-> {
            Set<String> options = new HashSet<>();
            for (ConverterOptions o : ConverterOptions.values()) {
                options.add(o.name());
            }
            return options;
        });
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        String targetPlugin = this.argAsString(0);
        ConverterOptions converterOption = argAsEnum(1, ConverterOptions.class, ConverterOptions.ALL);
        if (targetPlugin == null){
            long startTime = System.currentTimeMillis();
            message(plugin,sender, Message.COMMAND_CONVERTER_START_ALL);
            for (Converter converter : plugin.getConverters()){
                processConverter(converter, converterOption);
            }
            long endTime = System.currentTimeMillis();
            message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE_ALL, "time", TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND));
        } else {
            Optional<Converter> optionalConverter = plugin.getConverter(targetPlugin);
            if (optionalConverter.isPresent()){
                long startTime = System.currentTimeMillis();
                message(plugin,sender, Message.COMMAND_CONVERTER_START, "plugin", targetPlugin);
                processConverter(optionalConverter.get(), converterOption);
                long endTime = System.currentTimeMillis();
                message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE, "plugin", targetPlugin, "time", TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND));
            } else {
                message(plugin,sender, Message.COMMAND_CONVERTER_NOT_FOUND, "plugin", targetPlugin);
            }
        }
        return CommandType.SUCCESS;
    }

    private void processConverter(Converter converter, ConverterOptions converterOption) {
        switch (converterOption){
            case ALL -> converter.convertAll();
            case ITEMS -> converter.convertItems();
            case PACKS -> converter.convertPack();
        }
    }
}
