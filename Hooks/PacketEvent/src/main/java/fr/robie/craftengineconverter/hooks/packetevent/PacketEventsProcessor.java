package fr.robie.craftengineconverter.hooks.packetevent;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import fr.robie.craftengineconverter.common.format.ComponentMeta;
import fr.robie.craftengineconverter.common.packet.PacketContent;
import fr.robie.craftengineconverter.common.packet.PacketProcessor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class PacketEventsProcessor {
    public static final PacketProcessor<WrapperPlayServerSystemChatMessage> SYSTEM_CHAT_MESSAGE = new PacketProcessor<>() {
        @Override
        public String name() {
            return "SYSTEM_CHAT_MESSAGE";
        }

        @Override
        public void edit(WrapperPlayServerSystemChatMessage wrappedPacket, Component component) {
            wrappedPacket.setMessage(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerSystemChatMessage> unpack(WrapperPlayServerSystemChatMessage wrappedPacket) {
            Component internal = wrappedPacket.getMessage();
            return new PacketContent<>(wrappedPacket, this, ComponentMeta.getPlainText(internal));
        }
    };

    public static final PacketProcessor<WrapperPlayServerSetTitleText> SET_TITLE_TEXT = new PacketProcessor<>() {
        @Override
        public String name() {
            return "SET_TITLE_TEXT";
        }

        @Override
        public void edit(WrapperPlayServerSetTitleText container, Component component) {
            container.setTitle(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerSetTitleText> unpack(WrapperPlayServerSetTitleText container) {
            Component internal = container.getTitle();
            return new PacketContent<>(container, this, ComponentMeta.getPlainText(internal));
        }
    };

    public static final PacketProcessor<WrapperPlayServerSetTitleSubtitle> SET_TITLE_SUBTITLE = new PacketProcessor<>() {
        @Override
        public String name() {
            return "SET_TITLE_SUBTITLE";
        }

        @Override
        public void edit(WrapperPlayServerSetTitleSubtitle container, Component component) {
            container.setSubtitle(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerSetTitleSubtitle> unpack(WrapperPlayServerSetTitleSubtitle container) {
            Component internal = container.getSubtitle();
            return new PacketContent<>(container, this, ComponentMeta.getPlainText(internal));
        }
    };

    public static final PacketProcessor<WrapperPlayServerActionBar> ACTION_BAR = new PacketProcessor<>() {
        @Override
        public String name() {
            return "ACTION_BAR";
        }

        @Override
        public void edit(WrapperPlayServerActionBar container, Component component) {
            container.setActionBarText(component);
        }

        @Override
        public @NotNull PacketContent<WrapperPlayServerActionBar> unpack(WrapperPlayServerActionBar container) {
            Component internal = container.getActionBarText();
            return new PacketContent<>(container, this, ComponentMeta.getPlainText(internal));
        }
    };
}
