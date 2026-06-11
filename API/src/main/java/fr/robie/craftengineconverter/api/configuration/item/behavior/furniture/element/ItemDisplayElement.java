package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element;

import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.DisplayProperties;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.ItemElement;
import fr.robie.craftengineconverter.api.enums.ItemDisplayType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemDisplayElement extends ItemElement {
    private ItemDisplayType displayTransform = ItemDisplayType.FIXED;

    private final DisplayProperties display = new DisplayProperties();

    public ItemDisplayElement(@NotNull String item) { super(item); }

    public void setDisplayTransform(@NotNull ItemDisplayType transform) { this.displayTransform = transform; }
    public DisplayProperties display() { return this.display; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "item_display");
        super.serialize(data);
        if (this.displayTransform != ItemDisplayType.FIXED)
            data.put("display-transform", this.displayTransform.name().toLowerCase(Locale.ROOT));
        this.display.serialize(data);
        return data;
    }
}
