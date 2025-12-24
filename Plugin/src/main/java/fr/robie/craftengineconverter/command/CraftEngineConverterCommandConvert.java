package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.builder.TimerBuilder;
import fr.robie.craftengineconverter.common.enums.ConverterOptions;
import fr.robie.craftengineconverter.common.format.Message;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import fr.robie.craftengineconverter.utils.permission.Permission;
import org.bukkit.entity.Player;

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
        this.addFlag("-dryrun");
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        String targetPlugin = this.argAsString(0);
        ConverterOptions converterOption = argAsEnum(1, ConverterOptions.class, ConverterOptions.ALL);
        boolean dryRun = this.containFlag("-dryrun");
        if (dryRun){
            message(plugin,sender, Message.COMMAND_CONVERTER_DRYRUN_ENABLED);
        }
        if (targetPlugin == null){
            long startTime = System.currentTimeMillis();
            message(plugin,sender, Message.COMMAND_CONVERTER_START_ALL);
            Collection<Converter> converters = plugin.getConverters();
            AtomicInteger counter = new AtomicInteger(converters.size());
            for (Converter converter : converters){
                CompletableFuture<Void> voidCompletableFuture = processConverter(converter, converterOption, Optional.ofNullable(this.player), dryRun);
                voidCompletableFuture.thenRun(() -> {
                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        long endTime = System.currentTimeMillis();
                        message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE_ALL, "time", TimerBuilder.formatTimeAuto(endTime-startTime));
                        converter.getSettings().setDryRunEnabled(false);
                    }
                });
            }
        } else {
            Optional<Converter> optionalConverter = plugin.getConverter(targetPlugin);
            if (optionalConverter.isPresent()){
                long startTime = System.currentTimeMillis();
                message(plugin,sender, Message.COMMAND_CONVERTER_START, "plugin", targetPlugin);
                Converter converter = optionalConverter.get();
                CompletableFuture<Void> voidCompletableFuture = processConverter(converter, converterOption, Optional.ofNullable(this.player), dryRun);
                voidCompletableFuture.thenRun(() -> {
                    long endTime = System.currentTimeMillis();
                    message(plugin,sender, Message.COMMAND_CONVERTER_COMPLETE, "plugin", targetPlugin, "time", TimerBuilder.formatTimeAuto(endTime-startTime));
                    converter.getSettings().setDryRunEnabled(false);
                });
            } else {
                message(plugin,sender, Message.COMMAND_CONVERTER_NOT_FOUND, "plugin", targetPlugin);
            }
        }
        return CommandType.SUCCESS;
    }

    private CompletableFuture<Void> processConverter(Converter converter, ConverterOptions converterOption, Optional<Player> player, boolean dryRun) {
        converter.getSettings().setDryRunEnabled(dryRun);
        return switch (converterOption){
            case ALL -> converter.convertAll(player);
            case ITEMS -> converter.convertItems(true, player);
            case PACKS -> converter.convertPack(true, player);
            case EMOJIS -> converter.convertEmojis(true, player);
            case IMAGES -> converter.convertImages(true, player);
            case LANGUAGES -> converter.convertLanguages(true, player);
            case SOUNDS -> converter.convertSounds(true, player);
        };
    }
}
