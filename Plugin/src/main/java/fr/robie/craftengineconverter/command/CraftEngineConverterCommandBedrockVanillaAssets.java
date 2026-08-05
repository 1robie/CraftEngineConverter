package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssetStore;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Fetches the vanilla models and textures that packs inherit but do not ship, up front.
 * <p>
 * Conversion primes them on its own when they are missing, so this exists to move that cost out of a conversion
 * window: an admin can prepare before a release, or refresh after changing the target version, rather than
 * discovering a download mid-convert. {@code refresh} discards the cached jar first.
 */
public class CraftEngineConverterCommandBedrockVanillaAssets extends SubCommand<CraftEngineConverter> {

    public CraftEngineConverterCommandBedrockVanillaAssets(CraftEngineConverter plugin) {
        super(plugin, "vanilla-assets", "assets");
        this.setPermission(Permission.COMMAND_BEDROCK_CONVERT.asPermission());
        this.addFlag("refresh");
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        CommandSender sender = commandDispatch.getSender();
        boolean refresh = commandDispatch.hasFlag("refresh");
        String version = VanillaAssetStore.targetVersion();

        if (refresh && VanillaAssetStore.clear(this.plugin.getDataFolder())) {
            sender.sendMessage("§7Discarded the cached vanilla assets for " + version);
        }

        sender.sendMessage("§7Preparing vanilla assets for §f" + version + "§7...");

        CompletableFuture.runAsync(() -> {
            VanillaAssets assets = VanillaAssetStore.prepare(this.plugin.getDataFolder());
            if (!assets.isAvailable()) {
                sender.sendMessage("§cCould not prepare vanilla assets for " + version
                        + ". Check the console; you can also set vanilla-assets.path to assets you already have.");
                return;
            }
            sender.sendMessage("§aVanilla assets ready for " + version + " §7(" + describe(assets.source()) + ")");
        }).exceptionally(error -> {
            Logger.error("Failed to prepare vanilla assets.", error);
            sender.sendMessage("§cFailed to prepare vanilla assets. Check console for details.");
            return null;
        });

        return CommandResultType.SUCCESS;
    }

    private static String describe(Path source) {
        if (source == null) return "no source";
        try {
            if (Files.isDirectory(source)) return source.toString();
            return source.getFileName() + ", " + (Files.size(source) / (1024 * 1024)) + " MB";
        } catch (Exception e) {
            return source.toString();
        }
    }
}
