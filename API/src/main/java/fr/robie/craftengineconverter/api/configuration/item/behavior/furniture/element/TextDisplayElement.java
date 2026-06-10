package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element;

import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.DisplayProperties;
import net.momirealms.craftengine.core.entity.display.TextDisplayAlignment;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TextDisplayElement implements Element {
    private final String text;
    private int lineWidth = -1;
    private int[] backgroundColor = null;
    private int textOpacity = -1;
    private boolean hasShadow = false;
    private boolean seeThrough = false;
    private boolean useDefaultBackground = true;
    private TextDisplayAlignment alignment = null;

    private final DisplayProperties display = new DisplayProperties();

    public TextDisplayElement(@NotNull String text) { this.text = text; }

    public DisplayProperties display() { return this.display; }
    public void setLineWidth(int lineWidth) { this.lineWidth = lineWidth; }
    public void setBackgroundColor(int a, int r, int g, int b) { this.backgroundColor = new int[]{a, r, g, b}; }
    public void setTextOpacity(int textOpacity) { this.textOpacity = textOpacity; }
    public void setHasShadow(boolean hasShadow) { this.hasShadow = hasShadow; }
    public void setSeeThrough(boolean seeThrough) { this.seeThrough = seeThrough; }
    public void setUseDefaultBackground(boolean useDefaultBackground) { this.useDefaultBackground = useDefaultBackground; }
    public void setAlignment(@NotNull TextDisplayAlignment alignment) { this.alignment = alignment; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "text_display");
        data.put("text", this.text);
        this.display.serialize(data);
        if (this.lineWidth != -1) data.put("line-width", this.lineWidth);
        if (this.backgroundColor != null) data.put("background-color", this.backgroundColor);
        if (this.textOpacity != -1) data.put("text-opacity", this.textOpacity);
        if (this.hasShadow) data.put("has-shadow", true);
        if (this.seeThrough) data.put("is-see-through", true);
        if (!this.useDefaultBackground) data.put("use-default-background-color", false);
        if (this.alignment != null) data.put("alignment", this.alignment.name().toLowerCase(Locale.ROOT));
        return data;
    }
}
