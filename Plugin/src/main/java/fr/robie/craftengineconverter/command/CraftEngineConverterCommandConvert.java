package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.enums.ConverterOption;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.Converter;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftEngineConverterCommandConvert extends VCommand {
    private final List<CompletableFuture<Void>> conversionTasks = new ArrayList<>();

    public CraftEngineConverterCommandConvert(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_CONVERT);
        this.setDescription(Message.COMMAND__CONVERTER__DESCRIPTION);
        this.addSubCommand("convert");
        this.addOptionalArg("plugin", (sender, args) -> this.plugin.getConverterNames());
        this.addOptionalArg("type", (sender, args) -> {
            Set<String> options = new HashSet<>();
            for (ConverterOption o : ConverterOption.values()) {
                options.add(o.name());
            }
            return options;
        });
        this.addFlag("--dryrun");
        this.addFlag("--force");
        this.addFlag("--threads", Integer.class, 1);
        this.setTabCompletor();
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        boolean forceConversion = this.containFlag("--force");
        if (!this.conversionTasks.isEmpty() && !forceConversion) {
            this.message(plugin, this.sender, Message.COMMAND__CONVERTER__ALREADY_RUNNING);
            return CommandType.SUCCESS;
        }

        if (forceConversion) {
            this.message(plugin, this.sender, Message.COMMAND__CONVERTER__FORCE_STOPPING);
            this.disableAllConversions();
        }

        String targetPlugin = this.argAsString(0);
        ConverterOption converterOption = this.argAsEnum(1, ConverterOption.class, ConverterOption.ALL);
        boolean dryRun = this.containFlag("--dryrun");
        if (dryRun) {
            this.message(plugin, this.sender, Message.COMMAND__CONVERTER__DRY_RUN_NOTE);
        }
        int threads = this.getFlagValueAsInteger("--threads");
        if (threads < 1) {
            threads = 1;
        } else if (threads > Runtime.getRuntime().availableProcessors()) {
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            threads = availableProcessors;
            this.message(plugin, this.sender, Message.COMMAND__CONVERTER__THREADS__ERROR_TOO_MANY, "max", availableProcessors);
        }
        this.message(plugin, this.sender, Message.COMMAND__CONVERTER__THREADS__INFO, "threads", threads);
        CraftEngineBlockState.resetAllLimits();
        if (targetPlugin == null) {
            long startTime = System.currentTimeMillis();
            this.message(plugin, this.sender, Message.COMMAND__CONVERTER__START__ALL);
            Collection<Converter> converters = plugin.getConverters();
            AtomicInteger counter = new AtomicInteger(converters.size());
            for (Converter converter : converters) {
                CompletableFuture<Void> voidCompletableFuture = converter.convert(converterOption, Optional.ofNullable(this.player), dryRun, threads);
                voidCompletableFuture.thenRun(() -> {
                    int remaining = counter.decrementAndGet();
                    if (remaining == 0) {
                        long endTime = System.currentTimeMillis();
                        this.message(plugin, this.sender, Message.COMMAND__CONVERTER__COMPLETE__ALL, "time", TimerBuilder.formatTimeAuto(endTime - startTime));
                    }
                    this.conversionTasks.remove(voidCompletableFuture);
                });
                this.conversionTasks.add(voidCompletableFuture);
            }
        } else {
            Optional<Converter> optionalConverter = plugin.getConverter(targetPlugin);
            if (optionalConverter.isPresent()) {
                long startTime = System.currentTimeMillis();
                this.message(plugin, this.sender, Message.COMMAND__CONVERTER__START__SINGLE, "plugin", targetPlugin);
                Converter converter = optionalConverter.get();
                CompletableFuture<Void> voidCompletableFuture = converter.convert(converterOption, Optional.ofNullable(this.player), dryRun, threads);
                voidCompletableFuture.thenRun(() -> {
                    long endTime = System.currentTimeMillis();
                    this.message(plugin, this.sender, Message.COMMAND__CONVERTER__COMPLETE__SINGLE, "plugin", targetPlugin, "time", TimerBuilder.formatTimeAuto(endTime - startTime));
                    this.conversionTasks.remove(voidCompletableFuture);
                });
                this.conversionTasks.add(voidCompletableFuture);
            } else {
                this.message(plugin, this.sender, Message.COMMAND__CONVERTER__NOT_SUPPORTED, "plugin", targetPlugin);
            }
        }
        return CommandType.SUCCESS;
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
    public void onDisable() {
        this.disableAllConversions();
    }
}
