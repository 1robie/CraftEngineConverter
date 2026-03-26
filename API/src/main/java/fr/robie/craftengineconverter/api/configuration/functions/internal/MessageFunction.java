package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Map;

public class MessageFunction extends AbstractEventFunction {
    private final Object message;
    private PlayerTarget target = PlayerTarget.SELF;
    private boolean overlay = false;

    public MessageFunction(Object message) {
        super("message");
        this.message = message;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    public void setOverlay(boolean overlay) {
        this.overlay = overlay;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("message", this.message);
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        if (this.overlay) {
            map.put("overlay", true);
        }
        return map;
    }
}
