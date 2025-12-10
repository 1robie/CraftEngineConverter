package fr.robie.craftEngineConverter.utils;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaCompatibilityManager {
    private final FoliaLib foliaLib;

    public FoliaCompatibilityManager(Plugin plugin) {
        this.foliaLib = new FoliaLib(plugin);
    }

    /**
     * Get the FoliaLib instance for direct access if needed.
     */
    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    /**
     * Check if running on Folia.
     */
    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    /**
     * Check if running on Paper.
     */
    public boolean isPaper() {
        return foliaLib.isPaper();
    }

    /**
     * Check if running on Spigot.
     */
    public boolean isSpigot() {
        return foliaLib.isSpigot();
    }

    /**
     * Run a task on the next tick.
     * On Folia: Uses GlobalRegionScheduler
     * On Paper/Spigot: Uses main thread scheduler
     */
    public void runNextTick(Runnable task) {
        foliaLib.getScheduler().runNextTick(wrappedTask -> task.run());
    }

    /**
     * Run a task asynchronously.
     * Works the same on all platforms.
     */
    public void runAsync(Runnable task) {
        foliaLib.getScheduler().runAsync(wrappedTask -> task.run());
    }

    public CompletableFuture<Void> runAsyncComplatable(Runnable task) {
        return foliaLib.getScheduler().runAsync(wrappedTask -> task.run());
    }

    /**
     * Run a task after a delay.
     *
     * @param task The task to run
     * @param delay Delay in ticks
     */
    public void runLater(Runnable task, long delay) {
        foliaLib.getScheduler().runLater(wrappedTask -> task.run(), delay);
    }

    public CompletableFuture<Void> runLaterComplatable(Runnable task, long delay) {
        return foliaLib.getScheduler().runLater(wrappedTask -> task.run(), delay);
    }

    /**
     * Run a task after a delay asynchronously.
     *
     * @param task The task to run
     * @param delay Delay in ticks
     */
    public void runLaterAsync(Runnable task, long delay) {
        foliaLib.getScheduler().runLaterAsync(wrappedTask -> task.run(), delay);
    }

    /**
     * Run a task after a delay with TimeUnit.
     *
     * @param task The task to run
     * @param delay Delay amount
     * @param unit Time unit
     */
    public void runLater(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLater(wrappedTask -> task.run(), delay, unit);
    }

    /**
     * Run a task after a delay asynchronously with TimeUnit.
     *
     * @param task The task to run
     * @param delay Delay amount
     */
    public void runLaterAsync(Runnable task, long delay, TimeUnit unit) {
        foliaLib.getScheduler().runLaterAsync(wrappedTask -> task.run(), delay, unit);
    }

    public CompletableFuture<Void> runLaterAsyncComplatable(Consumer<WrappedTask> task, long delay, TimeUnit unit) {
        return foliaLib.getScheduler().runLaterAsync(task, delay, unit);
    }

    public CompletableFuture<Void> runLaterAsyncComplatable(Runnable task, long delay) {
        return foliaLib.getScheduler().runLaterAsync(wrappedTask -> task.run(), delay);
    }

    /**
     * Run a task repeatedly at fixed intervals.
     *
     * @param task The task to run
     * @param initialDelay Initial delay in ticks
     * @param period Period between executions in ticks
     */
    public void runTimer(Runnable task, long initialDelay, long period) {
        foliaLib.getScheduler().runTimer(wrappedTask -> task.run(), initialDelay, period);
    }

    /**
     * Run a task repeatedly at fixed intervals asynchronously.
     *
     * @param task The task to run
     * @param initialDelay Initial delay in ticks
     * @param period Period between executions in ticks
     */
    public void runTimerAsync(Runnable task, long initialDelay, long period) {
        foliaLib.getScheduler().runTimerAsync(wrappedTask -> task.run(), initialDelay, period);
    }

    /**
     * Run a task repeatedly at fixed intervals asynchronously with TimeUnit.
     *
     * @param task The task to run
     * @param initialDelay Initial delay
     * @param period Period between executions
     * @param unit Time unit for both delay and period
     */
    public void runTimerAsync(Runnable task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimerAsync(wrappedTask -> task.run(), initialDelay, period, unit);
    }

    public void runTimerAsync(Consumer<WrappedTask> task, long initialDelay, long period, TimeUnit unit) {
        foliaLib.getScheduler().runTimerAsync(task, initialDelay, period, unit);
    }

    /**
     * Run a task at a specific location.
     * On Folia: Uses RegionScheduler for the region owning the location
     * On Paper/Spigot: Uses main thread scheduler
     *
     * @param location The location
     * @param task The task to run
     */
    public void runAtLocation(Location location, Runnable task) {
        foliaLib.getScheduler().runAtLocation(location, wrappedTask -> task.run());
    }

    /**
     * Run a task for a specific entity.
     * On Folia: Uses EntityScheduler for the entity
     * On Paper/Spigot: Uses main thread scheduler
     *
     * @param entity The entity
     * @param task The task to run
     */
    public void runAtEntity(Entity entity, Runnable task) {
        foliaLib.getScheduler().runAtEntity(entity, wrappedTask -> task.run());
    }

    /**
     * Teleport an entity asynchronously.
     * On Folia: Uses native async teleportation
     * On Paper/Spigot: Falls back to synchronous teleportation on next tick
     *
     * @param entity The entity to teleport
     * @param location The destination location
     * @return CompletableFuture<Boolean> indicating success
     */
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return foliaLib.getScheduler().teleportAsync(entity, location);
    }

    /**
     * Teleport an entity asynchronously with a specific cause.
     *
     * @param entity The entity to teleport
     * @param location The destination location
     * @param cause The teleportation cause
     * @return CompletableFuture<Boolean> indicating success
     */
    public CompletableFuture<Boolean> teleportAsync(Entity entity, Location location, PlayerTeleportEvent.TeleportCause cause) {
        return foliaLib.getScheduler().teleportAsync(entity, location, cause);
    }

    /**
     * Teleport a player safely (with appropriate region context).
     * This method ensures the teleportation happens in the correct thread context.
     *
     * @param player The player to teleport
     * @param location The destination location
     * @return CompletableFuture<Boolean> indicating success
     */
    public CompletableFuture<Boolean> teleportPlayerSafely(Player player, Location location) {
        return teleportAsync(player, location, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    /**
     * Cancel all tasks associated with this plugin.
     * Should be called during plugin shutdown.
     */
    public void cancelAllTasks() {
        foliaLib.getScheduler().cancelAllTasks();
    }

    /**
     * Get platform information for debugging.
     */
    public String getPlatformInfo() {
        if (foliaLib.isFolia()) {
            return "Folia (Regionised Multithreading)";
        } else if (foliaLib.isPaper()) {
            return "Paper (Single Thread with Folia Compatibility)";
        } else {
            return "Spigot/Bukkit (Single Thread with Folia Compatibility)";
        }
    }

    /**
     * Check if a location is in the current region's thread context (Folia only).
     * On Paper/Spigot, this always returns true.
     *
     * @param location The location to check
     * @return true if the current thread owns this location's region
     */
    public boolean isOwnedByCurrentRegion(Location location) {
        if (foliaLib.isFolia()) {
            // Use Bukkit.isOwnedByCurrentRegion() on Folia
            try {
                return org.bukkit.Bukkit.isOwnedByCurrentRegion(location);
            } catch (Exception e) {
                // Fallback if method doesn't exist in current version
                return true;
            }
        }
        return true; // Always true on Paper/Spigot (single thread)
    }

    /**
     * Check if an entity is in the current region's thread context (Folia only).
     * On Paper/Spigot, this always returns true.
     *
     * @param entity The entity to check
     * @return true if the current thread owns this entity's region
     */
    public boolean isOwnedByCurrentRegion(Entity entity) {
        if (foliaLib.isFolia()) {
            // Use Bukkit.isOwnedByCurrentRegion() on Folia
            try {
                return org.bukkit.Bukkit.isOwnedByCurrentRegion(entity);
            } catch (Exception e) {
                // Fallback if method doesn't exist in current version
                return true;
            }
        }
        return true; // Always true on Paper/Spigot (single thread)
    }
}