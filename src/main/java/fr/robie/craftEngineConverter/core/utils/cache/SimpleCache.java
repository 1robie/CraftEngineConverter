package fr.robie.craftEngineConverter.core.utils.cache;

import java.util.HashMap;
import java.util.Map;

public class SimpleCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();

    public V get(K key, Loader<V> loader) {
        return this.cache.computeIfAbsent(key, (k) -> loader.load());
    }

    public interface Loader<V> {
        V load();
    }

    public void clear() {
        this.cache.clear();
    }
}