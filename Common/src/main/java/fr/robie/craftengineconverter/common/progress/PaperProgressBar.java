package fr.robie.craftengineconverter.common.progress;

import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.format.ComponentMeta;
import net.kyori.adventure.bossbar.BossBar;

/**
 * PaperProgressBar displays a progress bar using the Adventure BossBar API for Paper servers.
 * It uses the ProgressColor enum for color and updates the boss bar for the player.
 */
public class PaperProgressBar extends BukkitProgressBar {
    private final ComponentMeta componentMeta;
    private final BossBar bossBar;

    public PaperProgressBar(CraftEngineConverterPlugin plugin, Builder builder) {
        super(plugin, builder);
        if (plugin.getMessageFormatter() instanceof ComponentMeta meta) {
            this.componentMeta = meta;
        } else {
            this.componentMeta = new ComponentMeta();
        }
        if (isNotNull(builder.player)){
            BossBar.Color color = BossBar.Color.BLUE;
            if (isNotNull(builder.progressColor)) {
                switch (builder.progressColor) {
                    case GREEN -> color = BossBar.Color.GREEN;
                    case RED,DARK_RED -> color = BossBar.Color.RED;
                    case GOLD,YELLOW -> color = BossBar.Color.YELLOW;
                    case DARK_PURPLE -> color = BossBar.Color.PURPLE;
                    case LIGHT_PURPLE -> color = BossBar.Color.PINK;
                    case WHITE -> color = BossBar.Color.WHITE;
                    default -> {}
                }
            }
            this.bossBar = BossBar.bossBar(this.componentMeta.getComponent(isNotNull(builder.prefix) ? builder.prefix : "Progress"), 0f, color, BossBar.Overlay.PROGRESS);
        } else {
            this.bossBar = null;
        }
    }

    @Override
    public void start() {
        super.start();
        if (isNotNull(this.player) && isNotNull(this.bossBar) && player.isOnline()) {
            player.showBossBar(this.bossBar);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (isNotNull(this.player) && isNotNull(this.bossBar) && player.isOnline()) {
            player.hideBossBar(this.bossBar);
        }
    }

    @Override
    public void displayProgress() {
        if (isNotNull(this.bossBar) && isNotNull(this.player) && player.isOnline()) {
            float progress = Math.min(1f, Math.max(0f, (float) getCurrent() / getTotal()));
            if (this.bossBar.progress() == progress){
                return;
            }
            this.bossBar.progress(progress);
            this.bossBar.name(this.componentMeta.getComponent(getProgress()));
        } else {
            super.displayProgress();
        }
    }
}

