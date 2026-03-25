package fr.robie.craftengineconverter.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.utils.drops.Drop;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.ConfigurationKey;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.history.BlockHistory;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.converter.BlockConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

public class NexoBlockConverter extends BlockConverter implements Listener {

    public NexoBlockConverter(CraftEngineConverterPlugin plugin) {
        super(plugin, Plugins.NEXO);
    }

    @EventHandler
    public void onNexoBlockInteract(NexoBlockInteractEvent event) {
        if (!Configuration.<Boolean>get(ConfigurationKey.NEXO_BLOCK_INTERACTION_CONVERSION) || !event.getPlayer().hasPermission(Permission.NEXO_BLOCK_INTERACT_CONVERSION.asPermission())) {
            return;
        }
        String itemID = event.getMechanic().getItemID();
        String newName = this.getNewName(itemID);

        if (newName == null || !this.isRegistered(newName)) {
            return;
        }

        Block block = event.getBlock();
        String blockState = block.getBlockData().getAsString();
        Location location = block.getLocation();

        if (!this.removeBlockAt(location)) {
            return;
        }

        this.placeBlock(newName, location, blockState);
        event.setCancelled(true);

        if (Configuration.<Boolean>get(ConfigurationKey.ALLOW_BLOCK_CONVERSION_PROPAGATION) && Configuration.<Integer>get(ConfigurationKey.MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH) > 1) {
            Set<Location> processed = new HashSet<>();
            processed.add(location);
            ConversionCounter counter = new ConversionCounter(Configuration.<Integer>get(ConfigurationKey.MAX_BLOCK_CONVERSION_PROPAGATION_DEPTH) - 1);
            this.executeBlockConversion(block.getLocation(), processed, counter);
        }
    }


    @Override
    public boolean isCustomBlockAt(Location location) {
        return NexoBlocks.customBlockMechanic(location) != null;
    }

    @Override
    public String getNewNameForCustomBlock(Location location) {
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(location);
        if (mechanic == null) {
            return null;
        }
        return this.getNewName(mechanic.getItemID());
    }

    @Override
    public void placeBlock(String itemId, Location location, String oldBlockState) {
        BlockHistory blockHistory = new BlockHistory(null,
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                oldBlockState,
                false);
        super.placeBlock(itemId, location, oldBlockState);
        this.serverProfile.addBlockHistory(blockHistory);
    }

    @Override
    public boolean removeBlockAt(Location location) {
        return NexoBlocks.remove(location, null, Drop.emptyDrop());
    }

}