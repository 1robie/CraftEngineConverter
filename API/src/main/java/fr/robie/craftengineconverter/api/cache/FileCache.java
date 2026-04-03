package fr.robie.craftengineconverter.api.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.craftengineconverter.api.logger.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class FileCache<T> {
    private final Cache<Path, FileCacheEntry<T>> cache;
    private final Function<File, T> loader;

    public FileCache(int maxSize, long expireAfterWriteMinutes, Function<File, T> loader) {
        this.loader = loader;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWriteMinutes, TimeUnit.MINUTES)
                .build();
    }

    public Optional<FileCacheEntry<T>> getEntryFile(@NotNull Path path) {
        File file = path.toFile();

        FileCacheEntry<T> entry = this.cache.getIfPresent(path);

        if (entry != null && entry.isUpToDate()) {
            return Optional.of(entry);
        }

        if (!file.exists()) {
            this.cache.invalidate(path);
            return Optional.empty();
        }

        try {
            T data = this.loader.apply(file);
            if (data == null) {
                Logger.debug(Message.ERROR__CACHE__NULL_RESULT, "path", path.toString());
                this.cache.invalidate(path);
                return Optional.empty();
            }
            entry = new FileCacheEntry<>(file, data);
            this.cache.put(path, entry);
            return Optional.of(entry);
        } catch (Exception e) {
            Logger.debug(Message.ERROR__CACHE__EXCEPTION, "path", path.toString(), "message", e.getMessage());
            this.cache.invalidate(path);
            return Optional.empty();
        }
    }

    public Optional<T> getData(@NotNull Path path) {
        return this.getEntryFile(path).map(FileCacheEntry::getData);
    }

    public void invalidateCache(@NotNull Path path) {
        this.cache.invalidate(path);
    }

    public void clearCache() {
        this.cache.invalidateAll();
    }

    public long cleanStaleEntries() {
        long initialSize = this.cache.size();
        this.cache.cleanUp();
        return initialSize - this.cache.size();
    }

    public long size() {
        return this.cache.size();
    }
}