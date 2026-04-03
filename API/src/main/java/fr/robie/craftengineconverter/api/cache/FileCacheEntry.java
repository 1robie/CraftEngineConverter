package fr.robie.craftengineconverter.api.cache;

import java.io.File;

public class FileCacheEntry<T> {
    private final long lastModified;
    private final File file;
    private final T data;

    public FileCacheEntry(File file, T data) {
        this.file = file;
        this.data = data;
        this.lastModified = file.lastModified();
    }

    public boolean isUpToDate() {
        return this.file.exists() && this.file.lastModified() == this.lastModified;
    }

    public File getFile() {
        return this.file;
    }

    public T getData() {
        return this.data;
    }

    public long getLastModified() {
        return this.lastModified;
    }
}