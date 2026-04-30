package fr.robie.craftengineconverter.api;

import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;

public class ReflectionsCache {
    private static final ReflectionsCache INSTANCE = new ReflectionsCache();
    private final Map<String, Reflections> cache = new HashMap<>();

    private ReflectionsCache() {
    }

    public static ReflectionsCache getInstance() {
        return INSTANCE;
    }

    public Reflections getOrCreate(Object caller, String namespace) {
        return this.cache.computeIfAbsent(namespace, key -> new Reflections(namespace));
    }
}