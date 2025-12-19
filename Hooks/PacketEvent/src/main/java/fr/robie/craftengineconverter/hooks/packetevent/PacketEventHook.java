package fr.robie.craftengineconverter.hooks.packetevent;

import com.github.retrooper.packetevents.PacketEvents;
import fr.robie.craftengineconverter.api.packet.PacketLoader;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

public class PacketEventHook implements PacketLoader {
    private final CraftEngineConverterPlugin plugin;

    public PacketEventHook(CraftEngineConverterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(){
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this.plugin));
        PacketEvents.getAPI().load();
        if (this.plugin.getFoliaCompatibilityManager().isPaper()) {
            PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this.plugin));
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
