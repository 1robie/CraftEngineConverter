package fr.robie.craftengineconverter.api.configuration.functions.internal;

import java.util.Locale;
import java.util.Map;

public class CommandFunction extends AbstractEventFunction {
    private final String command;
    private PlayerTarget target = PlayerTarget.SELF;
    private boolean asPlayer = false;
    private boolean asOp = false;
    private boolean asEvent = false;

    public CommandFunction(String command) {
        super("command");
        this.command = command;
    }

    public void setTarget(PlayerTarget target) {
        this.target = target;
    }

    public void setAsPlayer(boolean asPlayer) {
        this.asPlayer = asPlayer;
    }

    public void setAsOp(boolean asOp) {
        this.asOp = asOp;
    }

    public void setAsEvent(boolean asEvent) {
        this.asEvent = asEvent;
    }

    public String getCommand() {
        return this.command;
    }

    public PlayerTarget getTarget() {
        return this.target;
    }

    public boolean isAsPlayer() {
        return this.asPlayer;
    }

    public boolean isAsOp() {
        return this.asOp;
    }

    public boolean isAsEvent() {
        return this.asEvent;
    }


    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = super.serialize();
        map.put("command", this.command);
        if (this.target != PlayerTarget.SELF) {
            map.put("target", this.target.name().toLowerCase(Locale.ROOT));
        }
        if (this.asPlayer) {
            map.put("as-player", true);
        }
        if (this.asOp) {
            map.put("as-op", true);
        }
        if (this.asEvent) {
            map.put("as-event", true);
        }
        return map;
    }
}
