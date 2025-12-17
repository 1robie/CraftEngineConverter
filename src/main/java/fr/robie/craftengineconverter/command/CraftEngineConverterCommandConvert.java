package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.builder.TimerBuilder;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.craftengineconverter.utils.enums.ConverterOptions;
import fr.robie.craftengineconverter.utils.format.Message;
import fr.robie.craftengineconverter.utils.permission.Permission;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
            Collection<Converter> converters = plugin.getConverters();
            AtomicInteger counter = new AtomicInteger(converters.size());
            for (Converter converter : converters){
                CompletableFuture<Void> voidCompletableFuture = processConverter(converter, converterOption);
                voidCompletableFuture.thenRun(() -> {
                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        long endTime = System.currentTimeMillis();
                        message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE_ALL, "time", TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND));
                    }
                });
            }
        } else {
            Optional<Converter> optionalConverter = plugin.getConverter(targetPlugin);
            if (optionalConverter.isPresent()){
                long startTime = System.currentTimeMillis();
                message(plugin,sender, Message.COMMAND_CONVERTER_START, "plugin", targetPlugin);
                CompletableFuture<Void> voidCompletableFuture = processConverter(optionalConverter.get(), converterOption);
                voidCompletableFuture.thenRun(() -> {
                    long endTime = System.currentTimeMillis();
                    message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE, "plugin", targetPlugin, "time", TimerBuilder.formatTime(endTime-startTime, TimerBuilder.TimeUnit.SECOND));
                });
            } else {
                message(plugin,sender, Message.COMMAND_CONVERTER_NOT_FOUND, "plugin", targetPlugin);
            }
        }
        return CommandType.SUCCESS;
    }

    private CompletableFuture<Void> processConverter(Converter converter, ConverterOptions converterOption) {
        return switch (converterOption){
            case ALL -> converter.convertAll();
            case ITEMS -> converter.convertItems(true);
            case PACKS -> converter.convertPack(true);
        };
    }
}
