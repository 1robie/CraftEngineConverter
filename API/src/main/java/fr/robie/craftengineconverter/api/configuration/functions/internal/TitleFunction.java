package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class TitleFunction extends AbstractEventFunction {
    private final String title;
    private final String subtitle;
    private Integer fadeIn;
    private Integer stay;
    private Integer fadeOut;

    public TitleFunction(String title, String subtitle) {
        super("title");
        this.title = title;
        this.subtitle = subtitle;
    }

    public void setFadeIn(int fadeIn) {
        this.fadeIn = fadeIn;
    }

    public void setStay(int stay) {
        this.stay = stay;
    }

    public void setFadeOut(int fadeOut) {
        this.fadeOut = fadeOut;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("title", this.title);
        map.put("subtitle", this.subtitle);
        if (this.fadeIn != null) {
            map.put("fade-in", this.fadeIn);
        }
        if (this.stay != null) {
            map.put("stay", this.stay);
        }
        if (this.fadeOut != null) {
            map.put("fade-out", this.fadeOut);
        }
        return map;
    }
}
