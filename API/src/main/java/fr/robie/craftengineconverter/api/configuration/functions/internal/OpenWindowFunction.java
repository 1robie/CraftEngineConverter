package fr.robie.craftengineconverter.api.configuration.functions.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;

public class OpenWindowFunction extends AbstractEventFunction {
    private final GuiType guiType;
    private String title;
    private PlayerTarget target = PlayerTarget.SELF;

    public OpenWindowFunction(@NotNull GuiType guiType) {
        super("open_window");
        this.guiType = guiType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    public GuiType getGuiType() {
        return this.guiType;
    }

    public String getTitle() {
        return this.title;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("gui", this.guiType.name().toLowerCase(Locale.ROOT));
        if (this.title != null) {
            map.put("title", this.title);
        }
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase(Locale.ROOT));
        }
        return map;
    }

    public enum GuiType {
        ANVIL,
        ENCHANTMENT,
        GRINDSTONE,
        LOOM,
        SMITHING,
        CRAFTING,
        CARTOGRAPHY
    }
}
