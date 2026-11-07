package fr.robie.craftengineconverter.api.configuration.item.data;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public record AttributeModifier(String type, net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Slot slot,
                                Key id, double amount,
                                net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Operation operation,
                                @Nullable Display display) {
    public AttributeModifier(String type, net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Slot slot, Key id, double amount, net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Operation operation, @Nullable Display display) {
        this.amount = amount;
        this.display = display;
        this.id = id;
        this.operation = operation;
        this.slot = slot;
        this.type = type;
    }

    public record Display(net.momirealms.craftengine.core.attribute.vanilla.VanillaAttributeModifier.Display.Type type, String value) {
    }
}
