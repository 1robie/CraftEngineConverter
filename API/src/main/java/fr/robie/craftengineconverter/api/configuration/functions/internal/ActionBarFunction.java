package fr.robie.craftengineconverter.api.configuration.functions.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ActionBarFunction extends AbstractEventFunction {
    private final String actionbarMessage;
    private PlayerTarget target = PlayerTarget.SELF;

    public ActionBarFunction(@NotNull String actionbarMessage, @NotNull PlayerTarget target) {
        super("actionbar");
        this.actionbarMessage = actionbarMessage;
    }

    public ActionBarFunction(@NotNull String actionbarMessage) {
        this(actionbarMessage, PlayerTarget.SELF);
    }

    public void setTarget(@NotNull PlayerTarget target) {
        this.target = target;
    }

    public PlayerTarget getTarget() {
        return this.target;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("actionbar", this.actionbarMessage);
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase());
        }
        return map;
    }
}
