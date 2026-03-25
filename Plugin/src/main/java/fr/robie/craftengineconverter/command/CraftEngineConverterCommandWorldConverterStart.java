package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.progress.BukkitProgressBar;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.listener.WorldConverterManager;
import fr.robie.craftengineconverter.utils.command.CommandType;
import fr.robie.craftengineconverter.utils.command.VCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CraftEngineConverterCommandWorldConverterStart extends VCommand {
    private CompletableFuture<Void> currentConversion = null;

    public CraftEngineConverterCommandWorldConverterStart(CraftEngineConverter plugin) {
        super(plugin);
        this.setPermission(Permission.COMMAND_WORLDCONVERTER_START);
        this.addSubCommand("start");
        this.addFlag("--force");
        this.addFlag("--chunks-per-tick", Integer.class, 10);
    }

    @Override
    protected CommandType perform(CraftEngineConverter plugin) {
        WorldConverterManager worldConverterManager = plugin.getWorldConverterManager();

        boolean forceConversion = this.containFlag("--force");

        if (this.currentConversion != null && !this.currentConversion.isDone() && !forceConversion) {
            this.message(plugin, this.sender, Message.COMMAND__WORLD_CONVERTER__ALREADY_RUNNING);
            return CommandType.SUCCESS;
        }

        if (forceConversion && this.currentConversion != null && !this.currentConversion.isDone()) {
            this.message(plugin, this.sender, Message.COMMAND__WORLD_CONVERTER__FORCE_STOPPING);
            worldConverterManager.cancelAllConversions();
            this.currentConversion.cancel(true);
        }

        int chunksPerTick = this.getFlagValueAsInteger("--chunks-per-tick");
        if (chunksPerTick < 1) {
            chunksPerTick = 1;
        } else if (chunksPerTick > 100) {
            chunksPerTick = 100;
        }

        List<World> worlds = Bukkit.getServer().getWorlds();
        int totalChunks = 0;
        for (World world : worlds) {
            totalChunks += world.getLoadedChunks().length;
        }

        BukkitProgressBar.Builder builder = new BukkitProgressBar.Builder(totalChunks).options(Configuration.worldConverterProgressBarOptions).prefix("World Converter:").suffix("chunks").updateInterval(5000);
        if (this.player != null) {
            builder.player(this.player);
            builder.showBar(false);
        }
        BukkitProgressBar progressBar = builder.build(this.plugin);

        this.message(plugin, this.sender, Message.COMMAND__WORLD_CONVERTER__START, "chunks", totalChunks);

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

            this.message(plugin, this.sender, Message.COMMAND__WORLD_CONVERTER__COMPLETE,
                    "chunks", processedChunks,
                    "blocks", convertedBlocks - oldConvertedBlocks,
                    "furniture", convertedFurniture - oldConvertedFurniture,
                    "time", TimerBuilder.formatTimeAuto(endTime - startTime));
        });

        return CommandType.SUCCESS;
    }

    @Override
    public void onDisable() {
        if (this.currentConversion != null && !this.currentConversion.isDone()) {
            this.currentConversion.cancel(true);
        }
    }
}
