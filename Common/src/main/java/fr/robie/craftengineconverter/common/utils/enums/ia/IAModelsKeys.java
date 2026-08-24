package fr.robie.craftengineconverter.common.utils.enums.ia;

import java.util.Collection;

public enum IAModelsKeys {
    BOW("normal", "pulling_0", "pulling_1", "pulling_2"),
    FISHING_ROD("normal", "cast"),
    CROSSBOW("normal", "pulling_0", "pulling_1", "pulling_2", "rocket", "arrow"),
    TRIDENT("normal", "throwing"),
    SHIELD("normal", "blocking");
    private final String[] keys;

    IAModelsKeys(String... keys) {
        this.keys = keys;
    }

    /**
     * Whether the given keys are exactly this model type's key set.
     *
     * <p>Several types share the {@code normal} key and have the same key count
     * (fishing rod, trident and shield all have two), so a partial match is not
     * enough to tell them apart - every key has to be present.
     */
    public boolean matches(Collection<String> keysToCheck) {
        if (keysToCheck.size() != this.keys.length) {
            return false;
        }
        for (String key : this.keys) {
            if (!this.contains(keysToCheck, key)) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(Collection<String> keysToCheck, String key) {
        for (String keyToCheck : keysToCheck) {
            if (key.equalsIgnoreCase(keyToCheck)) {
                return true;
            }
        }
        return false;
    }
}
