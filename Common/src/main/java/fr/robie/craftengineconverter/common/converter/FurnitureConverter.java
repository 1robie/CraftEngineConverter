package fr.robie.craftengineconverter.common.converter;

import com.google.gson.Gson;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.history.EntityHistory;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class FurnitureConverter extends ObjectConverter {
    public FurnitureConverter(CraftEngineConverterPlugin plugin, Plugins pluginType) {
        super(plugin, pluginType);
    }

    /** Converts whatever adjoins furniture that was just converted, within the configured budget. */
    protected void propagateFrom(Location origin) {
        ConversionCounter counter = this.propagationCounter();
        if (counter == null) {
            return;
        }

        Set<BlockKey> processed = new HashSet<>();
        processed.add(BlockKey.of(origin));
        this.executeFurnitureConversion(origin, processed, counter);
    }

    protected void executeFurnitureConversion(Location entityLoc, Set<BlockKey> processed, ConversionCounter counter) {
        for (int[] offset : this.ADJACENT_OFFSETS) {
            if (counter.hasReachedLimit()) {
                return;
            }
            Entity furnitureEntityAt = this.getFurnitureEntityAt(entityLoc.clone().add(offset[0], offset[1], offset[2]));
            Location adjacentLoc = (furnitureEntityAt == null ? entityLoc.clone().add(offset[0], offset[1], offset[2]) : furnitureEntityAt.getLocation()).add(0, -0.5, 0);
            if (!adjacentLoc.isChunkLoaded() || !processed.add(BlockKey.of(adjacentLoc))) {
                continue;
            }
            if (!this.isFurnitureAt(adjacentLoc)) {
                continue;
            }
            String newName = this.getNewNameForFurniture(adjacentLoc);
            if (newName == null || !this.isRegistered(newName)) {
                continue;
            }
            // isFurnitureAt can be true where no entity was found - Nexo answers it from the barrier blocks, and
            // the probe happens half a block above adjacentLoc - so the furniture is still converted, just without
            // the snapshot that would let it be restored.
            String entityNBT = furnitureEntityAt == null ? null : furnitureEntityAt.getAsString();
            if (this.removeFurnitureAt(adjacentLoc)) {
                this.placeFurniture(newName, adjacentLoc, entityNBT);
                counter.increment();
                this.executeFurnitureConversion(adjacentLoc, processed, counter);
            }
        }
    }

    @Override
    public boolean isRegistered(String itemId) {
        return CraftEngineFurniture.byId(Key.from(itemId)) != null;
    }

    /** How far around a probe point the hooks look for the entity that carries a piece of furniture. */
    protected static final double FURNITURE_SEARCH_RADIUS = 1.0;

    /**
     * The candidate closest to {@code location}, rather than whichever the world happened to return first.
     * <p>
     * The search box is a block wide in every direction, so a neighbouring piece of furniture is just as likely to
     * be inside it as the one actually being asked about - and picking the wrong one converts the wrong furniture.
     */
    @Nullable
    protected static Entity nearest(Location location, Collection<? extends Entity> candidates, Predicate<Entity> accepted) {
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            if (!accepted.test(candidate)) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    public abstract Entity getFurnitureEntityAt(Location location);

    public abstract boolean isFurnitureAt(Location location);

    public abstract String getNewNameForFurniture(Location location);

    public abstract boolean removeFurnitureAt(Location location);

    public void placeFurniture(String itemId, Location location, @Nullable String entityNBT) {
        this.plugin.getPlacementTracker().placeFurniture(itemId, location);
        if (entityNBT == null) {
            return;
        }

        // Callers place furniture half a block below the entity it replaces; history stores where the entity was.
        Location entityLocation = location.clone().add(0, 0.5, 0);
        this.serverProfile.addEntityHistory(
                new EntityHistory(null, new Gson().toJson(entityLocation.serialize()), entityNBT, false));
    }

    public static boolean isRegisteredStatic(String itemId) {
        return CraftEngineFurniture.byId(Key.from(itemId)) != null;
    }
}
