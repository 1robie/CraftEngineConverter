package fr.robie.craftengineconverter.utils;



import fr.robie.messageflow.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic cache for batching objects before processing them.
 * Thread-safe implementation using ConcurrentHashMap with upsert behavior.
 *
 * @param <T> The type of objects to cache
 */
public class TypedCache<T> {
    private final Class<T> type;
    private final Map<String, T> cache;
    private final Consumer<List<T>> batchProcessor;
    private final int maxBatchSize;
    private final Function<T, String> keyExtractor;
    private final AtomicBoolean draining = new AtomicBoolean();
    private volatile int overflowThreshold = Integer.MAX_VALUE;
    private volatile Runnable overflowHandler;

    private static final int FLUSH_ATTEMPTS = 100;
    private static final long FLUSH_RETRY_DELAY_MS = 20L;

    /**
     * Creates a new TypedCache.
     *
     * @param type The class type
     * @param batchProcessor Function to process a batch of objects
     * @param maxBatchSize Maximum number of objects to process per batch
     * @param keyExtractor Function to extract a unique key from an object
     */
    public TypedCache(Class<T> type, Consumer<List<T>> batchProcessor, int maxBatchSize, Function<T, String> keyExtractor) {
        this.type = type;
        this.cache = new ConcurrentHashMap<>();
        this.batchProcessor = batchProcessor;
        this.maxBatchSize = maxBatchSize;
        this.keyExtractor = keyExtractor;
    }

    /**
     * Adds an object to the cache.
     * If an object with the same key already exists, it will be replaced (upsert behavior).
     *
     * @param object The object to cache
     */
    public void add(T object) {
        if (object == null) {
            return;
        }
        this.cache.put(this.keyExtractor.apply(object), object);

        Runnable handler = this.overflowHandler;
        if (handler != null && this.cache.size() >= this.overflowThreshold && !this.draining.get()) {
            handler.run();
        }
    }

    /**
     * Asks to be drained early once it holds {@code threshold} objects, rather than waiting for the next scheduled
     * save. Without this the buffer only empties on a timer, and anything producing rows faster than one cycle
     * removes them grows without bound - which is precisely what a world-wide conversion does.
     *
     * @param threshold how many pending objects trigger the handler
     * @param handler   runs on the caller's thread; it is expected to schedule the drain elsewhere, not perform it
     */
    public void onOverflow(int threshold, Runnable handler) {
        this.overflowThreshold = threshold;
        this.overflowHandler = handler;
    }

    /**
     * Processes up to maxBatchSize objects from the cache.
     *
     * @return Number of objects processed
     */
    /**
     * Writes out everything pending, a batch at a time, up to {@code maxObjects}.
     * <p>
     * Draining one batch per scheduled save was the whole problem: at 250 rows a minute the buffer could not keep up
     * with a conversion, so it grew for as long as the conversion ran. The cap is here only to stop a single cycle
     * monopolising the database connection.
     *
     * @return how many objects were written
     */
    public int drain(int maxObjects) {
        if (!this.draining.compareAndSet(false, true)) {
            return 0;
        }
        try {
            int written = 0;
            while (written < maxObjects && !this.cache.isEmpty()) {
                int batch = this.processBatch();
                if (batch == 0) {
                    break;
                }
                written += batch;
            }
            return written;
        } finally {
            this.draining.set(false);
        }
    }

    /**
     * Processes up to maxBatchSize objects from the cache.
     *
     * @return the number of objects written, or 0 when nothing could be
     */
    public int processBatch() {
        if (this.cache.isEmpty()) {
            return 0;
        }

        List<T> batch = new ArrayList<>();
        List<String> keysToRemove = new ArrayList<>();
        int count = 0;

        for (Map.Entry<String, T> entry : this.cache.entrySet()) {
            if (count >= this.maxBatchSize) {
                break;
            }
            batch.add(entry.getValue());
            keysToRemove.add(entry.getKey());
            count++;
        }

        if (batch.isEmpty()) {
            return 0;
        }

        try {
            this.batchProcessor.accept(batch);
            keysToRemove.forEach(this.cache::remove);
            return batch.size();
        } catch (Exception e) {
            Logger.error(
                "Failed to process batch of " + this.type.getSimpleName() + " objects",
                e
            );
            return 0;
        }
    }

    /**
     * Writes out everything pending, however long that takes. Called at shutdown, where losing rows is the one
     * outcome worth blocking to avoid.
     * <p>
     * A scheduled drain may hold the lock when this runs, which is why it retries rather than returning: {@code
     * drain} declines to run concurrently with itself, and treating that as "nothing left to write" would drop
     * whatever the other drain had not reached.
     */
    public void flush() {
        if (this.cache.isEmpty()) {
            return;
        }

        Logger.info("Flushing " + this.cache.size() + " remaining " + this.type.getSimpleName() + " objects from cache...");
        int written = 0;
        for (int attempt = 0; attempt < FLUSH_ATTEMPTS && !this.cache.isEmpty(); attempt++) {
            int batch = this.drain(Integer.MAX_VALUE);
            written += batch;
            if (batch == 0 && !this.cache.isEmpty()) {
                try {
                    Thread.sleep(FLUSH_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (this.cache.isEmpty()) {
            Logger.info("Finished flushing " + written + " " + this.type.getSimpleName() + " objects.");
        } else {
            Logger.error("Gave up flushing with " + this.cache.size() + " " + this.type.getSimpleName()
                    + " objects still pending; they will not be written.");
        }
    }

    /**
     * Gets the number of objects currently in the cache.
     *
     * @return Cache size
     */
    public int size() {
        return this.cache.size();
    }

    /**
     * Checks if the cache is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }

    /**
     * Gets the type of objects this cache handles.
     *
     * @return The class type
     */
    public Class<T> getType() {
        return this.type;
    }

    /**
     * Clears all objects from the cache without processing them.
     */
    public void clear() {
        this.cache.clear();
    }
}

