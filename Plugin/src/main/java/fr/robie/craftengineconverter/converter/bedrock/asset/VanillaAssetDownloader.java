package fr.robie.craftengineconverter.converter.bedrock.asset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * Fetches a Minecraft client jar so the vanilla models and textures a pack inherits can be read.
 * <p>
 * Uses Mojang's own published endpoints, the same ones the launcher uses: the version manifest names every
 * version, a version's own JSON names its client jar and that jar's SHA-1. Nothing is redistributed — the plugin
 * ships no Mojang files and fetches from the official source into a local cache, which is what a launcher does.
 * <p>
 * The hash is verified. A jar that does not match is deleted rather than cached, because a truncated download
 * would otherwise be indistinguishable from a version whose assets are simply missing files.
 */
public final class VanillaAssetDownloader {
    private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private VanillaAssetDownloader() {
        throw new UnsupportedOperationException("VanillaAssetDownloader is a utility class and cannot be instantiated.");
    }

    /**
     * Downloads the client jar for {@code version} into {@code cacheDir}, unless it is already there.
     * <p>
     * Blocking, and deliberately so — it is the caller's job to be off the main thread. Downloading tens of
     * megabytes on the server thread would stall the server for the duration.
     *
     * @return the jar, or {@code null} when it could not be fetched
     */
    @Nullable
    public static Path download(@NotNull String version, @NotNull Path cacheDir) {
        Path jar = cacheDir.resolve("client.jar");
        if (Files.isRegularFile(jar)) {
            Logger.info("Vanilla assets for " + version + " are already cached");
            return jar;
        }

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()) {
            JsonObject versionEntry = findVersion(client, version);
            if (versionEntry == null) {
                Logger.warn("Minecraft version " + version + " is not in Mojang's version manifest"
                        + " - set vanilla-assets.version to a released version, or vanilla-assets.path to assets"
                        + " you already have");
                return null;
            }

            JsonObject download = fetchJson(client, versionEntry.get("url").getAsString())
                    .getAsJsonObject("downloads").getAsJsonObject("client");
            String url = download.get("url").getAsString();
            String expectedSha1 = download.get("sha1").getAsString();

            Logger.info("Downloading vanilla assets for " + version + " from Mojang");
            Files.createDirectories(cacheDir);

            Path partial = cacheDir.resolve("client.jar.part");
            HttpResponse<InputStream> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                Logger.warn("Mojang returned HTTP " + response.statusCode() + " for the " + version + " client jar");
                return null;
            }
            try (InputStream in = response.body()) {
                Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
            }

            String actualSha1 = sha1Of(partial);
            if (!expectedSha1.equalsIgnoreCase(actualSha1)) {
                Files.deleteIfExists(partial);
                Logger.warn("The downloaded " + version + " client jar is corrupt (expected sha1 " + expectedSha1
                        + ", got " + actualSha1 + ") - not caching it");
                return null;
            }

            Files.move(partial, jar, StandardCopyOption.REPLACE_EXISTING);
            Logger.info("Cached vanilla assets for " + version + " ("
                    + (Files.size(jar) / (1024 * 1024)) + " MB) at " + jar);
            return jar;
        } catch (Exception e) {
            Logger.warn("Could not download vanilla assets for " + version + ": " + e.getMessage()
                    + " - custom blocks will fall back to a built-in cube shape."
                    + " Set vanilla-assets.path to use assets you already have.");
            return null;
        }
    }

    @Nullable
    private static JsonObject findVersion(HttpClient client, String version) throws Exception {
        JsonArray versions = fetchJson(client, MANIFEST_URL).getAsJsonArray("versions");
        for (JsonElement element : versions) {
            JsonObject entry = element.getAsJsonObject();
            if (version.equals(entry.get("id").getAsString())) return entry;
        }
        return null;
    }

    private static JsonObject fetchJson(HttpClient client, String url) throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static String sha1Of(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}
