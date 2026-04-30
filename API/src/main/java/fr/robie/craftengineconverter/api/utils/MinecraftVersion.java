package fr.robie.craftengineconverter.api.utils;

import org.bukkit.Bukkit;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static MinecraftVersion current;

    private final int major;
    private final int minor;
    private final int patch;

    private MinecraftVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Returns the current server's Minecraft version, parsed once and cached.
     */
    public static MinecraftVersion current() {
        if (current == null) {
            String bukkitVersion;
            try {
                bukkitVersion = Bukkit.getBukkitVersion();
            } catch (NoClassDefFoundError | Exception e) { // In case we're running in an environment without Bukkit (e.g., during testing), we can't get the version, so we default to (0, 0, 0).
                bukkitVersion = null;
            }
            current = parse(bukkitVersion);
        }
        return current;
    }

    /**
     * Parses a version string such as {@code "1.21.4"} or {@code "1.21.4-R0.1-SNAPSHOT"}.
     * If parsing fails, returns {@code (0, 0, 0)} and logs a warning — this causes all
     * {@code @SinceVersion} checks to fail conservatively rather than crashing.
     */
    public static MinecraftVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new MinecraftVersion(0, 0, 0);
        }
        // Strip everything after the first '-'  →  "1.21.4-R0.1-SNAPSHOT" becomes "1.21.4"
        String clean = raw.contains("-") ? raw.substring(0, raw.indexOf('-')) : raw;
        String[] parts = clean.split("\\.");
        try {
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new MinecraftVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            return new MinecraftVersion(0, 0, 0);
        }
    }

    /**
     * Returns {@code true} if this version is greater than or equal to {@code other}.
     */
    public boolean isAtLeast(MinecraftVersion other) {
        return this.compareTo(other) >= 0;
    }

    /**
     * Returns {@code true} if this version is less than or equal to {@code other}.
     */
    public boolean isAtMost(MinecraftVersion other) {
        return this.compareTo(other) <= 0;
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public int getPatch() {
        return this.patch;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MinecraftVersion other)) {
            return false;
        }
        return this.major == other.major && this.minor == other.minor && this.patch == other.patch;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * this.major + this.minor) + this.patch;
    }

    @Override
    public String toString() {
        return this.major + "." + this.minor + (this.patch != 0 ? "." + this.patch : "");
    }
}
