package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.listener.WorldConverterManager;
import fr.robie.messageflow.formatter.MessageFormatter;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import fr.robie.paperdispatch.flag.Flags;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CraftEngineConverterCommandWorldConverterStart extends SubCommand<CraftEngineConverter> {
    private final MessageFormatter<CraftEngineConverterPlugin, ?> messageFormatter;
    private CompletableFuture<Void> currentConversion = null;

    public CraftEngineConverterCommandWorldConverterStart(CraftEngineConverter plugin) {
        super(plugin, "start");
        this.messageFormatter = plugin.getMessageFormatter();
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_START.asPermission());
        this.addFlag("force");
        this.addFlag(Flags.intFlag("chunks-per-tick", 1, 100).defaultTo(10));
    }

    public void onDisable() {
        if (this.currentConversion != null && !this.currentConversion.isDone()) {
            this.currentConversion.cancel(true);
        }
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        WorldConverterManager worldConverterManager = this.plugin.getWorldConverterManager();

        boolean forceConversion = commandDispatch.hasFlag("force");

        CommandSender sender = commandDispatch.getSender();
        if (this.currentConversion != null && !this.currentConversion.isDone() && !forceConversion) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__ALREADY_RUNNING, sender);
            return CommandResultType.SUCCESS;
        }

        if (forceConversion && this.currentConversion != null && !this.currentConversion.isDone()) {
            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__FORCE_STOPPING, sender);
            worldConverterManager.cancelAllConversions();
            this.currentConversion.cancel(true);
        }

        int chunksPerTick = commandDispatch.getFlagValue("chunks-per-tick", Integer.class);

        List<World> worlds = Bukkit.getServer().getWorlds();
        int totalChunks = 0;
        for (World world : worlds) {
            totalChunks += world.getLoadedChunks().length;
        }

        BukkitProgressBar.Builder builder = new BukkitProgressBar.Builder(totalChunks).options(Configuration.worldConverterProgressBarOptions).prefix("World Converter:").suffix("chunks").updateInterval(5000);
        Player player = commandDispatch.getPlayer();
        if (player != null) {
            builder.player(player);
            builder.showBar(false);
        }
        BukkitProgressBar progressBar = builder.build(this.plugin);

        this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__START, sender, Placeholder.of("chunks", String.valueOf(totalChunks)));

        int oldConvertedBlocks = worldConverterManager.getPlacementTracker().getBlocksConverted();
        int oldConvertedFurniture = worldConverterManager.getPlacementTracker().getFurnitureConverted();

        long startTime = System.currentTimeMillis();

        progressBar.start();

        worldConverterManager.clearProcessedChunks();

        CompletableFuture<Void> schedulingFuture = worldConverterManager.executeChunckWithThrottling(chunksPerTick, progressBar);

        this.currentConversion = schedulingFuture.thenCompose(v -> worldConverterManager.awaitAllConversions());
        this.currentConversion.thenRun(() -> {
            long endTime = System.currentTimeMillis();
            int processedChunks = worldConverterManager.getProcessedChunksCount();
            int convertedBlocks = worldConverterManager.getPlacementTracker().getBlocksConverted();
            int convertedFurniture = worldConverterManager.getPlacementTracker().getFurnitureConverted();

            Placeholder.Builder placeholderBuilder = Placeholder.builder();
            placeholderBuilder.register("chunks", String.valueOf(processedChunks))
                    .register("blocks", String.valueOf(convertedBlocks - oldConvertedBlocks))
                    .register("furniture", String.valueOf(convertedFurniture - oldConvertedFurniture))
                    .register("time", TimerBuilder.formatTimeAuto(endTime - startTime));

            this.messageFormatter.sendMessage(Message.COMMAND__WORLD_CONVERTER__COMPLETE, sender, placeholderBuilder.build());
        });
        return CommandResultType.SUCCESS;
    }
}
