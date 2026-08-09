package fr.robie.craftengineconverter.converter.bedrock.asset;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.utils.MinecraftVersion;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decides where the vanilla assets come from and where they are cached.
 * <p>
 * Split from {@link VanillaAssets} because the two answer different questions: this one reads configuration and
 * may reach the network, while {@code VanillaAssets} only looks things up in whatever source it was handed. Keeping
 * them apart is what lets a conversion attach a source without any risk of downloading on the calling thread.
 */
public final class VanillaAssetStore {

    private VanillaAssetStore() {
        throw new UnsupportedOperationException("VanillaAssetStore is a utility class and cannot be instantiated.");
    }

    /** The Minecraft version whose assets to use: the configured one, or the server's own. */
    @NotNull
    public static String targetVersion() {
        String configured = Configuration.get(Keys.VANILLA_ASSETS_VERSION);
        if (configured != null && !configured.isBlank() && !configured.equalsIgnoreCase("auto")) {
            return configured.trim();
        }
        return MinecraftVersion.getCurrentVersion().toString();
    }

    public static Path cacheDir(@NotNull File pluginFolder) {
        return pluginFolder.toPath().resolve("vanilla-assets").resolve(targetVersion());
    }

    /**
     * The assets already available, without touching the network: the configured path if set, else a jar a previous
     * download or the {@code vanilla-assets} command left in the cache.
     */
    @NotNull
    public static VanillaAssets existing(@NotNull File pluginFolder) {
        Path cache = cacheDir(pluginFolder);

        Path configured = configuredPath();
        if (configured != null) {
            Logger.info("Using vanilla assets from " + configured);
            return new VanillaAssets(cache, configured);
        }

        Path jar = cache.resolve("client.jar");
        return Files.isRegularFile(jar) ? new VanillaAssets(cache, jar) : VanillaAssets.empty(cache);
    }

    /**
     * The assets, downloading them first if they are missing and downloading is allowed.
     * <p>
     * <b>Blocking.</b> Call it off the server thread — a client jar is tens of megabytes.
     */
    @NotNull
    public static VanillaAssets prepare(@NotNull File pluginFolder) {
        VanillaAssets existing = existing(pluginFolder);
        if (existing.isAvailable()) return existing;

        if (!Configuration.get(Keys.VANILLA_ASSETS_DOWNLOAD)) {
            Logger.info("vanilla-assets.download is off and nothing is cached, so vanilla parents cannot be"
                    + " resolved. Set vanilla-assets.path to assets you already have, or turn the download on.");
            return existing;
        }

        Path cache = cacheDir(pluginFolder);
        Path jar = VanillaAssetDownloader.download(targetVersion(), cache);
        return jar == null ? VanillaAssets.empty(cache) : new VanillaAssets(cache, jar);
    }

    /** Removes the cached jar for the target version, so the next prepare re-downloads it. */
    public static boolean clear(@NotNull File pluginFolder) {
        try {
            return Files.deleteIfExists(cacheDir(pluginFolder).resolve("client.jar"));
        } catch (Exception e) {
            Logger.warn("Could not delete the cached vanilla assets: " + e.getMessage());
            return false;
        }
    }

    @Nullable
    private static Path configuredPath() {
        String configured = Configuration.get(Keys.VANILLA_ASSETS_PATH);
        if (configured == null || configured.isBlank()) return null;

        Path path = Path.of(configured.trim());
        if (!Files.exists(path)) {
            Logger.warn("vanilla-assets.path points at " + path + ", which does not exist - ignoring it");
            return null;
        }
        return path;
    }
}
