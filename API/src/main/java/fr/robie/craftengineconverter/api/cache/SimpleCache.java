package fr.robie.craftengineconverter.api.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A small keep-forever cache, safe to read while it is being written.
 *
 * <p>Its one user is filled by a conversion running on its own thread and read by chat formatting and placeholder
 * resolution on whichever thread asks - so a plain map was being written and read at the same time, which costs
 * either a stale answer or a corrupted table. Null keys and values are ignored rather than throwing, since the keys
 * come from whatever text a player happened to send.
 */
public class SimpleCache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();

    public V get(K key) {
        return key == null ? null : this.cache.get(key);
    }

    public V getOrDefault(K key, Loader<V> loader) {
        if (key == null) {
            return loader.load();
        }
        return this.cache.computeIfAbsent(key, k -> loader.load());
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        this.cache.put(key, value);
    }

    public boolean containsKey(K key) {
        return key != null && this.cache.containsKey(key);
    }

    public interface Loader<V> {
        V load();
    }

    public void clear() {
        this.cache.clear();
    }
}
