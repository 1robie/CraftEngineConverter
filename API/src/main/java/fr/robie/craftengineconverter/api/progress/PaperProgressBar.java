package fr.robie.craftengineconverter.api.progress;

import fr.robie.craftengineconverter.api.CraftEngineConverterPluginInterface;
import fr.robie.messageflow.formatter.AdventureMessageFormatter;
import net.kyori.adventure.bossbar.BossBar;

/**
 * PaperProgressBar displays a progress bar using the Adventure BossBar API for Paper servers.
 * It uses the ProgressColor enum for color and updates the boss bar for the player.
 */
public class PaperProgressBar extends BukkitProgressBar {
    private final AdventureMessageFormatter<?> messageFormatter;
    private final BossBar bossBar;

    public PaperProgressBar(CraftEngineConverterPluginInterface plugin, Builder builder) {
        super(plugin, builder);
        if (plugin.getMessageFormatter() instanceof AdventureMessageFormatter<?> adventureFormatter) {
            this.messageFormatter = adventureFormatter;
        } else {
            throw new IllegalStateException("Plugin's message formatter must be an instance of AdventureMessageFormatter");
        }
        if (this.isNotNull(builder.player)) {
            BossBar.Color color = BossBar.Color.BLUE;
            if (this.isNotNull(builder.progressColor)) {
                switch (builder.progressColor) {
                    case GREEN -> color = BossBar.Color.GREEN;
                    case RED, DARK_RED -> color = BossBar.Color.RED;
                    case GOLD, YELLOW -> color = BossBar.Color.YELLOW;
                    case DARK_PURPLE -> color = BossBar.Color.PURPLE;
                    case LIGHT_PURPLE -> color = BossBar.Color.PINK;
                    case WHITE -> color = BossBar.Color.WHITE;
                    default -> {
                    }
                }
            }
            this.bossBar = BossBar.bossBar(this.messageFormatter.getComponent(this.isNotNull(builder.prefix) ? builder.prefix : "Progress"), 0f, color, BossBar.Overlay.PROGRESS);
        } else {
            this.bossBar = null;
        }
    }

    @Override
    public void start() {
        super.start();
        if (this.isNotNull(this.player) && this.isNotNull(this.bossBar) && this.player.isOnline()) {
            this.player.showBossBar(this.bossBar);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.isNotNull(this.player) && this.isNotNull(this.bossBar) && this.player.isOnline()) {
            this.player.hideBossBar(this.bossBar);
        }
    }

    @Override
    public void displayProgress() {
        if (this.isNotNull(this.bossBar) && this.isNotNull(this.player) && this.player.isOnline()) {
            float progress = Math.clamp((float) this.getCurrent() / this.getTotal(), 0f, 1f);
            if (this.bossBar.progress() == progress) {
                return;
            }
            this.bossBar.progress(progress);
            this.bossBar.name(this.messageFormatter.getComponent(this.getProgress()));
        } else {
            super.displayProgress();
        }
    }
}

