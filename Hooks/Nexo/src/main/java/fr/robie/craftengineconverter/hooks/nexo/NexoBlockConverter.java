package fr.robie.craftengineconverter.hooks.nexo;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.events.custom_block.NexoBlockInteractEvent;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import com.nexomc.nexo.utils.drops.Drop;
import fr.robie.craftengineconverter.api.configuration.Configuration;
import fr.robie.craftengineconverter.api.configuration.Keys;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.converter.BlockConverter;
import fr.robie.craftengineconverter.common.permission.Permission;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class NexoBlockConverter extends BlockConverter implements Listener {

    public NexoBlockConverter(CraftEngineConverterPlugin plugin) {
        super(plugin, Plugins.NEXO);
    }

    @EventHandler
    public void onNexoBlockInteract(NexoBlockInteractEvent event) {
        if (!Configuration.get(Keys.NEXO_BLOCK_INTERACTION_CONVERSION) || !event.getPlayer().hasPermission(Permission.NEXO_BLOCK_INTERACT_CONVERSION.asPermission())) {
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
        this.propagateFrom(location);
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
    public boolean removeBlockAt(Location location) {
        return NexoBlocks.remove(location, null, Drop.emptyDrop());
    }

}