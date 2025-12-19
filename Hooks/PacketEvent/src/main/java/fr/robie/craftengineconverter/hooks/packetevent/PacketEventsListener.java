package fr.robie.craftengineconverter.hooks.packetevent;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.configuration.Configuration;
import fr.robie.craftengineconverter.common.format.ComponentMeta;
import fr.robie.craftengineconverter.common.logger.Logger;
import fr.robie.craftengineconverter.common.packet.PacketContent;
import fr.robie.craftengineconverter.common.packet.PacketProcessor;
import fr.robie.craftengineconverter.common.tag.ITagResolver;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PacketEventsListener extends PacketListenerAbstract {
    private final CraftEngineConverterPlugin plugin;
    private final ComponentMeta componentMeta;
    private final ITagResolver tagResolverUtils;
    private final Map<PacketType.Play.Server, PacketProcessor<?>> packetTypeProcessors = new HashMap<>();

    public PacketEventsListener(CraftEngineConverterPlugin plugin) {
        super(PacketListenerPriority.LOW);
        this.plugin = plugin;

        if (Configuration.pluginMessageFormatting){
            this.packetTypeProcessors.put(PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, PacketEventsProcessor.SYSTEM_CHAT_MESSAGE);
        }
        if (Configuration.titleFormatting){
            this.packetTypeProcessors.put(PacketType.Play.Server.SET_TITLE_TEXT, PacketEventsProcessor.SET_TITLE_TEXT);
            this.packetTypeProcessors.put(PacketType.Play.Server.SET_TITLE_SUBTITLE, PacketEventsProcessor.SET_TITLE_SUBTITLE);
        }
        if (Configuration.actionBarFormatting){
            this.packetTypeProcessors.put(PacketType.Play.Server.ACTION_BAR, PacketEventsProcessor.ACTION_BAR);
        }
        if (Configuration.bossBarFormatting){
            this.packetTypeProcessors.put(PacketType.Play.Server.BOSS_BAR, PacketEventsProcessor.BOSS_BAR);
        }

        this.componentMeta = this.plugin.getMessageFormatter() instanceof ComponentMeta meta ? meta : new ComponentMeta();
        this.tagResolverUtils = this.plugin.getTagResolver();
    }

    @Override @SuppressWarnings("unchecked")
    public void onPacketSend(@NotNull PacketSendEvent event){
        final PacketTypeCommon packetType = event.getPacketType();
        if (!(packetType instanceof PacketType.Play.Server server)) return;

        final PacketProcessor<?> processor = this.packetTypeProcessors.get(server);
        if (processor == null) {
//            switch (server) {
//                case ENTITY_RELATIVE_MOVE_AND_ROTATION,ENTITY_RELATIVE_MOVE,ENTITY_HEAD_LOOK,BLOCK_CHANGE,PLAYER_INFO_UPDATE,KEEP_ALIVE,DAMAGE_EVENT,EFFECT,ENTITY_ROTATION,ENTITY_ANIMATION,ENTITY_EFFECT,ENTITY_METADATA,ENTITY_MOVEMENT,ENTITY_POSITION_SYNC,ENTITY_EQUIPMENT,ENTITY_SOUND_EFFECT,END_COMBAT_EVENT,ENTER_COMBAT_EVENT,ENTITY_STATUS,ENTITY_TELEPORT,ENTITY_VELOCITY,TIME_UPDATE,SOUND_EFFECT,DESTROY_ENTITIES,CHUNK_DATA,CHUNK_BIOMES,BUNDLE,UPDATE_ADVANCEMENTS,UPDATE_ATTRIBUTES,SPAWN_ENTITY,CHANGE_GAME_STATE -> {
//                    return;
//                }
//                default -> {}
//            }
//            Logger.info("No processor found for packet of type " + server);
            return;
        }

        Object wrappedPacket = switch (server){
            case SYSTEM_CHAT_MESSAGE -> new WrapperPlayServerSystemChatMessage(event);
            case SET_TITLE_TEXT -> new WrapperPlayServerSetTitleText(event);
            case SET_TITLE_SUBTITLE -> new WrapperPlayServerSetTitleSubtitle(event);
            case ACTION_BAR -> new WrapperPlayServerActionBar(event);
            case BOSS_BAR -> new WrapperPlayServerBossBar(event);
            default -> null;
        };

        if (wrappedPacket == null) {
            return;
        };

        final PacketContent<?> packet;

        try {
            packet = ((PacketProcessor<Object>) processor).unpack(wrappedPacket);
        } catch (final Exception ex) {
            Logger.showException("Failed to unpack packet of type " + server, ex);
            return;
        }

        if (packet == null || packet.isEmpty()) return;
        final String message = packet.message();

        final Optional<String> parsed = this.tagResolverUtils.resolveTags(message, event.getPlayer());
        if (parsed.isEmpty()) {
            return;
        }

        final Component translated = this.componentMeta.getComponent(parsed.get());
        if (translated.equals(Component.empty())) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
        packet.save(translated);
    }
}
