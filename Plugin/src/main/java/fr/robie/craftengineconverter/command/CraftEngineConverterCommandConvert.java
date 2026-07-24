package fr.robie.craftengineconverter.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.paperdispatch.argument.EnumArgument;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import fr.robie.paperdispatch.flag.Flags;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftEngineConverterCommandConvert extends SubCommand<CraftEngineConverter> {
    private final List<CompletableFuture<Void>> conversionTasks = new ArrayList<>();
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;

    public CraftEngineConverterCommandConvert(CraftEngineConverter plugin) {
        super(plugin, "convert");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_CONVERT.asPermission());

        this.addOptionalArgument(Commands.argument("plugin", StringArgumentType.word()).suggests(((context, builder) -> {
            this.plugin.getConverterNames().stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(builder.getRemaining())).forEach(builder::suggest);
            return builder.buildFuture();
        })));
        this.addOptionalArgument(Commands.argument("type", new EnumArgument<>(ConverterOption.class)));

        this.addFlag("dryrun");
        this.addFlag("force");
        this.addFlag(Flags.intFlag("threads").defaultTo(1));
    }

    private void disableAllConversions() {
        if (!this.conversionTasks.isEmpty()) {
            for (CompletableFuture<Void> task : new ArrayList<>(this.conversionTasks)) {
                task.cancel(true);
            }
            this.conversionTasks.clear();
        }
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        CommandSender sender = commandDispatch.getSender();
        boolean forceConversion = commandDispatch.hasFlag("force");
        if (!this.conversionTasks.isEmpty() && !forceConversion) {
            this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__ALREADY_RUNNING, sender);
            return CommandResultType.SUCCESS;
        }

        if (forceConversion) {
            this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__FORCE_STOPPING, sender);
            this.disableAllConversions();
        }

        ConverterOption converterOption = commandDispatch.getArgument("type", ConverterOption.class, ConverterOption.ALL);
        int threads = commandDispatch.getFlagValue("threads", Integer.class);
        if (threads < 1) {
            threads = 1;
        } else if (threads > Runtime.getRuntime().availableProcessors()) {
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            threads = availableProcessors;
            this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__THREADS__ERROR_TOO_MANY, sender, Placeholder.of("max", String.valueOf(availableProcessors)));
        }
        this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__THREADS__INFO, sender, Placeholder.of("threads", String.valueOf(threads)));
        CraftEngineBlockState.resetAllLimits();
        Optional<String> optionalPlugin = commandDispatch.getOptionalArgument("plugin", String.class);
        if (optionalPlugin.isPresent()) {
            String targetPlugin = optionalPlugin.get();
            Optional<Converter> optionalConverter = this.plugin.getConverter(targetPlugin);
            if (optionalConverter.isPresent()) {
                long startTime = System.currentTimeMillis();
                this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__START__SINGLE, sender, Placeholder.of("plugin", targetPlugin));
                Converter converter = optionalConverter.get();
                CompletableFuture<Void> voidCompletableFuture = converter.convert(converterOption, Optional.ofNullable(commandDispatch.getPlayer()), commandDispatch.hasFlag("dryrun"), threads);
                voidCompletableFuture.thenRun(() -> {
                    long endTime = System.currentTimeMillis();
                    this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__COMPLETE__SINGLE, sender, Placeholder.of("plugin", targetPlugin, "time", TimerBuilder.formatTimeAuto(endTime - startTime)));
                    this.conversionTasks.remove(voidCompletableFuture);
                });
                this.conversionTasks.add(voidCompletableFuture);
            } else {
                this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__NOT_SUPPORTED, sender, Placeholder.of("plugin", targetPlugin));
            }
        } else {
            long startTime = System.currentTimeMillis();
            this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__START__ALL,sender);
            Collection<Converter> converters = this.plugin.getConverters();
            AtomicInteger counter = new AtomicInteger(converters.size());
            for (Converter converter : converters) {
                CompletableFuture<Void> voidCompletableFuture = converter.convert(converterOption, Optional.ofNullable(commandDispatch.getPlayer()), commandDispatch.hasFlag("dryrun"), threads);
                voidCompletableFuture.thenRun(() -> {
                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        long endTime = System.currentTimeMillis();
                        this.messageFormatter.sendMessage(Message.COMMAND__CONVERTER__COMPLETE__ALL, sender, Placeholder.of("time", TimerBuilder.formatTimeAuto(endTime - startTime)));
                    }
                    this.conversionTasks.remove(voidCompletableFuture);
                });
                this.conversionTasks.add(voidCompletableFuture);
            }
        }
        return CommandResultType.SUCCESS;
    }
}
