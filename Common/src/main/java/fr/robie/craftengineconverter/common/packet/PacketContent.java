package fr.robie.craftengineconverter.common.packet;

import net.kyori.adventure.text.Component;

public record PacketContent<C>(C container, PacketProcessor<C> processor, String message) {

    public void save(final Component component) {
        this.processor.edit(container, component);
    }

    public boolean isEmpty() {
        return message.isEmpty();
    }

}