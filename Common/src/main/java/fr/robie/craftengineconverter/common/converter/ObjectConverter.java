package fr.robie.craftengineconverter.common.converter;

import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.profile.ServerProfile;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.PluginNameMapper;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

public abstract class ObjectConverter {
    protected final CraftEngineConverterPlugin plugin;
    protected final ServerProfile serverProfile;
    protected final Plugins pluginType;
    protected final PluginNameMapper nameMapper;
    protected final int[][] ADJACENT_OFFSETS = {
            {0, 1, 0},   // above
            {0, -1, 0},  // below
            {-1, 0, 0},  // left
            {1, 0, 0},   // right
            {0, 0, 1},   // front
            {0, 0, -1}   // back
    };

    public ObjectConverter(CraftEngineConverterPlugin plugin, Plugins pluginType) {
        this.plugin = plugin;
        this.serverProfile = plugin.getServerProfile();
        this.pluginType = pluginType;
        this.nameMapper = PluginNameMapper.getInstance();
    }

    public String getNewName(String oldName) {
        return this.nameMapper.getNewName(this.pluginType, oldName);
    }

    public abstract boolean isRegistered(String itemId);

    /**
     * A budget for one interaction's propagation, or null when it should not propagate at all. The clicked object is
     * already converted by the time this is asked for, which is why the budget is one less than the configured limit.
     */
    @Nullable
    protected ConversionCounter propagationCounter() {
        if (!Configuration.get(Keys.ALLOW_BLOCK_CONVERSION_PROPAGATION)) {
            return null;
        }
        int limit = Configuration.get(Keys.MAX_BLOCK_CONVERSION_PROPAGATION_LIMIT);
        return limit > 1 ? new ConversionCounter(limit - 1) : null;
    }

    /**
     * Identifies a position by the block it falls in.
     * <p>
     * Propagation has to remember where it has been, and {@link Location} cannot do that job: it compares exact
     * coordinates, and the same piece of furniture is reached both through its entity's own fractional position and
     * through a whole-block offset from a neighbour. Those two never compare equal, so a Location-keyed set lets the
     * walk revisit the same furniture over and over until the conversion budget runs out.
     */
    protected record BlockKey(String world, int x, int y, int z) {

        protected static BlockKey of(Location location) {
            World world = location.getWorld();
            return new BlockKey(world == null ? "" : world.getName(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    protected static class ConversionCounter {
        private final int maxConversions;
        private int conversions = 0;

        public ConversionCounter(int maxConversions) {
            this.maxConversions = maxConversions;
        }

        void increment() {
            this.conversions++;
        }

        boolean hasReachedLimit() {
            return this.conversions >= this.maxConversions;
        }
    }
}
