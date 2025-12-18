package fr.robie.craftengineconverter.hooks.packetevent;

import com.github.retrooper.packetevents.PacketEvents;
import fr.robie.craftengineconverter.api.packet.PacketLoader;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;

public class PacketEventHook implements PacketLoader {
    private final Plugin plugin;
    private final CraftEngineConverterPlugin pluginInstance;

    public PacketEventHook(Plugin plugin, CraftEngineConverterPlugin pluginInstance) {
        this.pluginInstance = pluginInstance;
        this.plugin = plugin;
    }

    @Override
    public void onLoad(){
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this.plugin));
        PacketEvents.getAPI().load();
        if (pluginInstance.getFoliaCompatibilityManager().isPaper()) {
            PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this.pluginInstance));
        }
    }

    @Override
    public void onEnable(){
        PacketEvents.getAPI().init();
    }

    @Override
    public void onDisable(){
        PacketEvents.getAPI().terminate();
    }
}
