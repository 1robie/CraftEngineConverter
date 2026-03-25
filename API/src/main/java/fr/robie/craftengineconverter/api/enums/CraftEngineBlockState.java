package fr.robie.craftengineconverter.api.enums;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.utils.BlockStateResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum CraftEngineBlockState {
    /*
     * ╔══════════════════════════════════════════════════════════════════╗
     * ║                    POOL HIERARCHY                                ║
     * ╚══════════════════════════════════════════════════════════════════╝
     *
     * ── SHARED POOLS ────────────────────────────────────────────────────
     *
     *  SOLID
     *  ├─ MUSHROOM
     *  │   ├─ RED_MUSHROOM_BLOCK  ─┐
     *  │   ├─ BROWN_MUSHROOM_BLOCK ─┼─ ↔ equiv each other + NOTE_BLOCK
     *  │   └─ MUSHROOM_STEM        ─┘
     *  └─ NOTE_BLOCK ────────────── ↔ equiv RED/BROWN/STEM
     *
     *  TRIPWIRE
     *  ├─ LOWER_TRIPWIRE  [attached=true]  ─┐
     *  └─ HIGHER_TRIPWIRE [attached=false] ─┴─ ↔ equiv each other + TRIPWIRE
     *
     *  LEAVES
     *  └─ WATERLOGGED_LEAVES  ↔ equiv LEAVES
     *
     * ── INDEPENDENT BLOCKS (no shared pool) ────────────────────────────
     *
     *  CACTUS     ↔ SUGAR_CANE
     *  SUGAR_CANE ↔ CACTUS
     *
     *  WEEPING_VINE  ─┐
     *  TWISTING_VINE ─┼─ ↔ equiv each other
     *  CAVE_VINE     ─┘
     *
     *  SAPLING  ↔ LEAVES
     *  KELP     ↔ NOTE_BLOCK
     *  CHORUS   ↔ LEAVES
     */

    LEAVES,
    WATERLOGGED_LEAVES,

    LOWER_TRIPWIRE,
    HIGHER_TRIPWIRE,
    TRIPWIRE,

    MUSHROOM_STEM,
    RED_MUSHROOM_BLOCK,
    BROWN_MUSHROOM_BLOCK,
    MUSHROOM,

    NOTE_BLOCK,
    SOLID,

    CACTUS,
    SUGAR_CANE,

    WEEPING_VINE,
    TWISTING_VINE,
    CAVE_VINE,

    SAPLING,
    KELP,
    CHORUS;

    private final Map<Plugins, Integer> pluginLimits = new HashMap<>();
    private final List<CraftEngineBlockState> contains = new ArrayList<>();
    private final List<CraftEngineBlockState> equivalents = new ArrayList<>();
    private int limit;
    private int start = 0;
    private CraftEngineBlockState parent = null;
    private String lastBlockState = null;

    static {

        RED_MUSHROOM_BLOCK.parent = MUSHROOM;
        BROWN_MUSHROOM_BLOCK.parent = MUSHROOM;
        MUSHROOM_STEM.parent = MUSHROOM;

        MUSHROOM.parent = SOLID;
        NOTE_BLOCK.parent = SOLID;

        WATERLOGGED_LEAVES.parent = LEAVES;

        LOWER_TRIPWIRE.parent = TRIPWIRE;
        HIGHER_TRIPWIRE.parent = TRIPWIRE;

        TRIPWIRE.contains.add(LOWER_TRIPWIRE);
        TRIPWIRE.contains.add(HIGHER_TRIPWIRE);

        MUSHROOM.contains.add(RED_MUSHROOM_BLOCK);
        MUSHROOM.contains.add(BROWN_MUSHROOM_BLOCK);
        MUSHROOM.contains.add(MUSHROOM_STEM);

        SOLID.contains.add(MUSHROOM);
        SOLID.contains.add(NOTE_BLOCK);

        LEAVES.equivalents.add(WATERLOGGED_LEAVES);
        WATERLOGGED_LEAVES.equivalents.add(LEAVES);

        LOWER_TRIPWIRE.equivalents.add(HIGHER_TRIPWIRE);
        LOWER_TRIPWIRE.equivalents.add(TRIPWIRE);
        HIGHER_TRIPWIRE.equivalents.add(LOWER_TRIPWIRE);
        HIGHER_TRIPWIRE.equivalents.add(TRIPWIRE);
        TRIPWIRE.equivalents.add(LOWER_TRIPWIRE);
        TRIPWIRE.equivalents.add(HIGHER_TRIPWIRE);

        RED_MUSHROOM_BLOCK.equivalents.add(BROWN_MUSHROOM_BLOCK);
        RED_MUSHROOM_BLOCK.equivalents.add(MUSHROOM_STEM);
        RED_MUSHROOM_BLOCK.equivalents.add(NOTE_BLOCK);
        BROWN_MUSHROOM_BLOCK.equivalents.add(RED_MUSHROOM_BLOCK);
        BROWN_MUSHROOM_BLOCK.equivalents.add(MUSHROOM_STEM);
        BROWN_MUSHROOM_BLOCK.equivalents.add(NOTE_BLOCK);
        MUSHROOM_STEM.equivalents.add(RED_MUSHROOM_BLOCK);
        MUSHROOM_STEM.equivalents.add(BROWN_MUSHROOM_BLOCK);
        MUSHROOM_STEM.equivalents.add(NOTE_BLOCK);

        NOTE_BLOCK.equivalents.add(RED_MUSHROOM_BLOCK);
        NOTE_BLOCK.equivalents.add(BROWN_MUSHROOM_BLOCK);
        NOTE_BLOCK.equivalents.add(MUSHROOM_STEM);

        WEEPING_VINE.equivalents.add(TWISTING_VINE);
        WEEPING_VINE.equivalents.add(CAVE_VINE);
        TWISTING_VINE.equivalents.add(WEEPING_VINE);
        TWISTING_VINE.equivalents.add(CAVE_VINE);
        CAVE_VINE.equivalents.add(WEEPING_VINE);
        CAVE_VINE.equivalents.add(TWISTING_VINE);

        CACTUS.equivalents.add(SUGAR_CANE);
        SUGAR_CANE.equivalents.add(CACTUS);

        SAPLING.equivalents.add(LEAVES);
        KELP.equivalents.add(NOTE_BLOCK);
        CHORUS.equivalents.add(LEAVES);
    }

    CraftEngineBlockState() {
        this.limit = 0;
    }

    public String getLastBlockState() {
        return this.lastBlockState;
    }

    public void setLastBlockStateAndIncrementLimit(@NotNull String lastBlockState) {
        this.lastBlockState = lastBlockState;
        this.limit++;

        if (this.parent != null) {
            this.parent.propagateIncrementLimit();
        }
    }

    private void propagateIncrementLimit() {
        this.limit++;
        if (this.parent != null) {
            this.parent.propagateIncrementLimit();
        }
    }

    public void setStart(int start) {
        if (start < 0) {
            throw new IllegalArgumentException("Start must be >= 0");
        }

        int oldStart = this.start;
        this.start = start;

        if (this.parent != null) {
            int deltaStart = start - oldStart;
            if (deltaStart > 0) {
                int newParentStart = this.parent.start + deltaStart;
                this.parent.setStart(newParentStart);
            }
        }
    }

    /**
     * Check if this CraftEngineBlockState has reached its limit.
     * Available slots = limit - start. If start >= limit, no slots are available.
     *
     * @param plugin The plugin to check
     * @return true if the limit is reached, false otherwise
     */
    private boolean hasReachLimit(Plugins plugin) {
        if (this.limit != -1) {
            int availableSlots = this.limit - this.start;
            if (availableSlots <= 0) {
                return true;
            }

            int myCurrentIndex = this.pluginLimits.getOrDefault(plugin, this.start);
            if (myCurrentIndex >= this.limit) {
                return true;
            }
        }

        CraftEngineBlockState target = this.getCountTarget();

        if (target == this) {
            return false;
        }

        if (target.limit == -1) {
            return false;
        }

        int currentCount = target.getCurrentCount(plugin);

        return currentCount >= target.limit;
    }

    /**
     * Check if after incrementing once, this state would be at its last available slot.
     *
     * @param plugin The plugin to check
     * @return true if the next increment would be the last available slot
     */
    private boolean isLastSlot(Plugins plugin) {
        int currentIndex = this.pluginLimits.getOrDefault(plugin, this.start);
        return (currentIndex + 1) >= this.limit;
    }

    /**
     * Get the BlockState that should be used for counting.
     * Returns parent if this BlockState is part of a shared pool, otherwise returns self.
     */
    private CraftEngineBlockState getCountTarget() {
        CraftEngineBlockState current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * Get the current count for this CraftEngineBlockState.
     * Returns the current index for this node (or aggregated from children).
     *
     * @param plugin The plugin to check
     * @return The current count/index
     */
    private int getCurrentCount(Plugins plugin) {
        if (Configuration.<LimitType>get(ConfigurationKey.BLOCK_STATE_LIMIT_TYPE) == fr.robie.craftengineconverter.api.enums.LimitType.PLUGIN) {
            return this.getMaxIndexRecursive(this, plugin);
        } else {
            return this.getTotalMaxIndexRecursive(this);
        }
    }

    /**
     * Return the maximum index used for a given plugin across the node and its descendants.
     * Only counts actual increments (entries in pluginLimits), not default start values.
     */
    private int getMaxIndexRecursive(CraftEngineBlockState node, Plugins plugin) {
        int maxIndex = node.start;

        Integer actualIndex = node.pluginLimits.get(plugin);
        if (actualIndex != null) {
            maxIndex = Math.max(maxIndex, actualIndex);
        }

        for (CraftEngineBlockState child : node.contains) {
            int childMax = this.getMaxIndexRecursive(child, plugin);
            maxIndex = Math.max(maxIndex, childMax);
        }

        return maxIndex;
    }

    /**
     * Return the maximum index used across all plugins for the node and its descendants.
     * Only counts actual increments (entries in pluginLimits), not default start values.
     */
    private int getTotalMaxIndexRecursive(CraftEngineBlockState node) {
        int maxIndex = node.start;

        for (Integer idx : node.pluginLimits.values()) {
            if (idx != null) {
                maxIndex = Math.max(maxIndex, idx);
            }
        }

        for (CraftEngineBlockState child : node.contains) {
            int childMax = this.getTotalMaxIndexRecursive(child);
            maxIndex = Math.max(maxIndex, childMax);
        }

        return maxIndex;
    }

    /**
     * Increment the usage counter.
     * Stores the index on THIS node (not the root), so that each node tracks its own range.
     *
     * @param plugin The plugin using this CraftEngineBlockState
     */
    public void increment(Plugins plugin) {
        int currentCount = this.pluginLimits.getOrDefault(plugin, this.start);
        this.pluginLimits.put(plugin, currentCount + 1);
    }

    /**
     * Decrement the usage counter.
     * Decrements THIS node's counter.
     *
     * @param plugin The plugin using this CraftEngineBlockState
     */
    public void decrement(Plugins plugin) {
        int current = this.pluginLimits.getOrDefault(plugin, this.start);
        if (current > this.start) {
            this.pluginLimits.put(plugin, current - 1);
        } else {
            this.pluginLimits.remove(plugin);
        }
    }

    /**
     * Add an equivalent CraftEngineBlockState that can be used as a fallback when this one reaches its limit.
     *
     * @param equivalent The equivalent CraftEngineBlockState
     */
    public void addEquivalent(CraftEngineBlockState equivalent) {
        if (!this.equivalents.contains(equivalent)) {
            this.equivalents.add(equivalent);
        }
    }

    /**
     * Get an available BlockStateResult (name, isLast flag, and block state instance),
     * or null if this state and all equivalents have reached their limit.
     * Does NOT increment the counter — use {@link #getAvailableAndIncrement(Plugins)} for that.
     *
     * @param plugin The plugin requesting the state
     * @return A {@link BlockStateResult}, or null if none available
     */
    public BlockStateResult getAvailable(Plugins plugin) {
        CraftEngineBlockState available = this.getAvailableCraftEngineBlockState(plugin, new ArrayList<>());
        if (available == null) {
            return null;
        }
        return new BlockStateResult(available, available.isLastSlot(plugin));
    }

    /**
     * Get an available BlockStateResult and increment its counter atomically.
     * Returns null if this state and all equivalents have reached their limit.
     *
     * @param plugin The plugin requesting the state
     * @return A {@link BlockStateResult}, or null if none available
     */
    public BlockStateResult getAvailableAndIncrement(Plugins plugin) {
        CraftEngineBlockState available = this.getAvailableCraftEngineBlockState(plugin, new ArrayList<>());
        if (available == null) {
            return null;
        }
        boolean last = available.isLastSlot(plugin);
        available.increment(plugin);
        return new BlockStateResult(available, last);
    }

    public BlockStateResult getAvailableAndIncrementNotLast(Plugins plugin) {
        CraftEngineBlockState available = this.getAvailableCraftEngineBlockState(plugin, new ArrayList<>());
        if (available == null) {
            return null;
        }
        boolean last = available.isLastSlot(plugin);
        if (!last) {
            available.increment(plugin);
        }
        return new BlockStateResult(available, last);
    }

    /**
     * Internal recursive method to find an available CraftEngineBlockState instance with cycle detection.
     *
     * @param plugin  The plugin requesting the CraftEngineBlockState
     * @param visited Set of already visited CraftEngineBlockStates to prevent infinite loops
     * @return The CraftEngineBlockState instance if available, or null if none available
     */
    private CraftEngineBlockState getAvailableCraftEngineBlockState(Plugins plugin, List<CraftEngineBlockState> visited) {
        if (visited.contains(this)) {
            return null;
        }
        visited.add(this);

        if (!this.hasReachLimit(plugin)) {
            return this;
        }

        for (CraftEngineBlockState equivalent : this.equivalents) {
            CraftEngineBlockState result = equivalent.getAvailableCraftEngineBlockState(plugin, visited);
            if (result != null) {
                return result;
            }
        }

        for (CraftEngineBlockState contained : this.contains) {
            CraftEngineBlockState result = contained.getAvailableCraftEngineBlockState(plugin, visited);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getStart() {
        return this.start;
    }

    public CraftEngineBlockState getParent() {
        return this.parent;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    /**
     * Get the number of remaining available slots.
     * Available slots = limit - current index.
     *
     * @param plugin The plugin to check
     * @return The number of remaining slots
     */
    public int getRemainingSlots(Plugins plugin) {
        int currentIndex = this.pluginLimits.getOrDefault(plugin, this.start);
        return Math.max(0, this.limit - currentIndex);
    }

    /**
     * Get the current usage count (number of slots used from start).
     *
     * @param plugin The plugin to check
     * @return The number of slots currently in use
     */
    public int getUsedSlots(Plugins plugin) {
        int currentIndex = this.pluginLimits.getOrDefault(plugin, this.start);
        return Math.max(0, currentIndex - this.start);
    }

    public List<CraftEngineBlockState> getContains() {
        return new ArrayList<>(this.contains);
    }

    public List<CraftEngineBlockState> getEquivalents() {
        return new ArrayList<>(this.equivalents);
    }

    public static void resetAllLimits() {
        for (CraftEngineBlockState state : CraftEngineBlockState.values()) {
            state.pluginLimits.clear();
        }
    }

    public void resetLimit() {
        this.pluginLimits.clear();
        this.lastBlockState = null;
        this.limit = 0;
        this.start = 0;
    }
}