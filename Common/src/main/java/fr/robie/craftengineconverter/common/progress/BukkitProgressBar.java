package fr.robie.craftengineconverter.common.progress;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import fr.robie.craftengineconverter.common.CraftEngineConverterPlugin;
import fr.robie.craftengineconverter.common.ObjectUtils;
import fr.robie.craftengineconverter.common.builder.TimerBuilder;
import fr.robie.craftengineconverter.common.logger.LogType;
import fr.robie.craftengineconverter.common.logger.Logger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class BukkitProgressBar extends ObjectUtils {
    private final CraftEngineConverterPlugin plugin;
    private WrappedTask task = null;

    private final int total;
    private int current;
    private long startTimeMillis = 0;
    private final char progressChar;
    private final char emptyChar;
    private final int barWidth;
    private final String progressColor;
    private final String emptyColor;
    private final String percentColor;
    private final String normalColor = ProgressColor.RESET.getCode();
    private final boolean showPercentage;
    private final boolean showCount;
    private final boolean showBar;
    private final boolean showTime;
    private final String prefix;
    private final String suffix;
    private final long updateIntervalTicks;
    private final boolean autoStop;
    protected final @Nullable Player player;

    private long lastUpdateTime = 0;
    private static final long MIN_LOG_INTERVAL_MS = 1000;

    public BukkitProgressBar(CraftEngineConverterPlugin plugin, Builder builder) {
        this.plugin = plugin;
        this.total = builder.total > 0 ? builder.total : 1;
        this.current = Math.max(0, Math.min(builder.current, this.total));
        this.progressChar = builder.progressChar;
        this.emptyChar = builder.emptyChar;
        this.barWidth = builder.barWidth > 0 ? builder.barWidth : 40;
        this.progressColor = builder.progressColor != null ? builder.progressColor.getCode() : "";
        this.emptyColor = builder.emptyColorCode != null ? builder.emptyColorCode.getCode() : "";
        this.percentColor = builder.percentColor != null ? builder.percentColor.getCode() : "";
        this.showPercentage = builder.showPercentage;
        this.showCount = builder.showCount;
        this.showBar = builder.showBar;
        this.showTime = builder.showTime;
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        this.updateIntervalTicks = builder.updateIntervalTicks;
        this.autoStop = builder.autoStop;
        this.player = builder.player;
    }

    /**
     * Starts the automatic progress logging task.
     * The progress bar will be logged at regular intervals.
     */
    public void start() {
        if (isNotNull(this.task)) {
            Logger.info("BukkitProgressBar is already started!", LogType.WARNING);
            return;
        }
        this.task = this.plugin.getFoliaCompatibilityManager()
                .runTimerAsyncWrapped(this::printProgress, 0L, updateIntervalTicks, TimeUnit.MILLISECONDS);
        this.startTimeMillis = System.currentTimeMillis();
    }

    /**
     * Stops the automatic progress logging task.
     */
    public void stop() {
        if (isNotNull(this.task)) {
            this.task.cancel();
            this.task = null;
            if (this.current < this.total) {
                Logger.info("Progress tracking stopped at " + String.format("%.1f%%", getPercentage()));
            }
        }
    }

    /**
     * Prints the current progress to the logger.
     * Includes rate limiting to avoid log spam.
     */
    public void printProgress() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastUpdateTime < this.MIN_LOG_INTERVAL_MS && !isComplete()) {
            return;
        }
        displayProgress();
        this.lastUpdateTime = currentTime;
    }

    /**
     * Forces an immediate progress log, bypassing rate limiting.
     * Useful for important milestones.
     */
    public void printProgressForced() {
        displayProgress();
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void displayProgress(){
        String progressMessage = getProgress();
        if (isNotNull(this.player)){
            this.plugin.getMessageFormatter().sendMessage(this.player, progressMessage);
        } else {
            Logger.info(progressMessage);
        }
    }

    /**
     * Returns the formatted progress bar string.
     */
    public String getProgress() {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            sb.append(prefix).append(" ");
        }

        if (showBar) {
            sb.append("[");
            int filled = (int) ((double) current / total * barWidth);

            if (!progressColor.isEmpty()) sb.append(progressColor);
            sb.append(String.valueOf(progressChar).repeat(Math.max(0, filled)));

            if (!emptyColor.isEmpty()) sb.append(emptyColor);
            sb.append(String.valueOf(emptyChar).repeat(Math.max(0, barWidth - filled)));

            if (!normalColor.isEmpty()) sb.append(normalColor);
            sb.append("] ");
        }

        if (showPercentage) {
            double percentage = (double) current / total * 100;
            if (!percentColor.isEmpty()) sb.append(percentColor);
            sb.append(String.format("%6.2f%%", percentage));
            if (!normalColor.isEmpty()) sb.append(normalColor);
            sb.append(" ");
        }

        if (showCount) {
            sb.append("(").append(current).append("/").append(total).append(")");
        }

        if (suffix != null && !suffix.isEmpty()) {
            sb.append(" ").append(suffix);
        }

        if (showTime){
            sb.append(" | ").append(TimerBuilder.formatTimeAuto(System.currentTimeMillis() - startTimeMillis)).append(" elapsed");
        }

        if (isComplete()) {
            sb.append(" ").append(ProgressColor.GREEN.getCode()).append("✔").append(normalColor);
            if (autoStop) {
                stop();
            }
        }

        return sb.toString().trim();
    }

    /**
     * Increments the progress by 1.
     */
    public void increment() {
        increment(1);
    }

    /**
     * Increments the progress by the specified amount.
     * @param amount the amount to increment
     */
    public void increment(int amount) {
        setCurrent(current + amount);
    }

    /**
     * Sets the current progress value.
     * @param current the new current value (clamped between 0 and total)
     */
    public void setCurrent(int current) {
        int oldCurrent = this.current;
        this.current = Math.max(0, Math.min(current, total));

        if (shouldLogMilestone(oldCurrent, this.current)) {
            printProgressForced();
        }
    }

    private boolean shouldLogMilestone(int oldValue, int newValue) {
        if (total < 5) return false;

        int oldMilestone = (oldValue * 5) / total;
        int newMilestone = (newValue * 5) / total;

        return newMilestone > oldMilestone;
    }

    public int getCurrent() {
        return current;
    }

    public int getTotal() {
        return total;
    }

    public double getPercentage() {
        return (double) current / total * 100;
    }

    public boolean isComplete() {
        return current >= total;
    }

    public long getEstimatedTimeRemaining(long startTimeMillis) {
        if (current == 0) return -1;

        long elapsed = System.currentTimeMillis() - startTimeMillis;
        long totalEstimated = (elapsed * total) / current;
        return totalEstimated - elapsed;
    }

    public static String formatDuration(long millis) {
        if (millis < 0) return "calculating...";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static class Builder {
        protected final int total;
        protected int current = 0;
        protected char progressChar = '█';
        protected char emptyChar = '░';
        protected int barWidth = 40;
        protected boolean showPercentage = true;
        protected boolean showCount = true;
        protected boolean showBar = true;
        protected boolean showTime = true;
        protected String prefix = "";
        protected String suffix = "";
        protected ProgressColor progressColor = ProgressColor.GREEN;
        protected ProgressColor emptyColorCode = ProgressColor.DARK_GRAY;
        protected ProgressColor percentColor = ProgressColor.YELLOW;
        protected long updateIntervalTicks = 1000; // 1 second default
        protected boolean autoStop = true;
        protected Player player = null;

        public Builder(int total) {
            this.total = total;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder current(int current) {
            this.current = current;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder progressChar(char progressChar) {
            this.progressChar = progressChar;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder emptyChar(char emptyChar) {
            this.emptyChar = emptyChar;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder barWidth(int barWidth) {
            this.barWidth = barWidth;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder showPercentage(boolean show) {
            this.showPercentage = show;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder showCount(boolean show) {
            this.showCount = show;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder showBar(boolean show) {
            this.showBar = show;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder showTime(boolean show) {
            this.showTime = show;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder progressColor(ProgressColor color) {
            this.progressColor = color;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder emptyColor(ProgressColor color) {
            this.emptyColorCode = color;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder percentColor(ProgressColor color) {
            this.percentColor = color;
            return this;
        }

        /**
         * Sets the update interval in milliseconds.
         * @param intervalMs interval in milliseconds
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder updateInterval(long intervalMs) {
            this.updateIntervalTicks = intervalMs;
            return this;
        }

        /**
         * Sets whether the progress bar should automatically stop when complete.
         * @param autoStop true to auto-stop (default), false to keep running
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder autoStop(boolean autoStop) {
            this.autoStop = autoStop;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder player(@Nullable Player player) {
            this.player = player;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder options(ProgressBarUtils options){
            this.progressChar = options.getProgressChar();
            this.emptyChar = options.getEmptyChar();
            this.barWidth = options.getBarWidth();
            this.progressColor = options.getProgressColor();
            this.emptyColorCode = options.getEmptyColor();
            this.percentColor = options.getPercentColor();
            return this;
        }

        /**
         * Builds the BukkitProgressBar instance.
         * @param plugin the plugin instance
         */
        public BukkitProgressBar build(CraftEngineConverterPlugin plugin) {
            return plugin.getFoliaCompatibilityManager().isPaper() ?
                    new PaperProgressBar(plugin, this) :
                    new BukkitProgressBar(plugin, this);
        }
    }

    /**
     * Minecraft color codes for progress bar formatting.
     * Uses the § symbol for Bukkit/Spigot compatibility.
     */
    public enum ProgressColor {
        BLACK("0"),
        DARK_BLUE("1"),
        DARK_GREEN("2"),
        DARK_AQUA("3"),
        DARK_RED("4"),
        DARK_PURPLE("5"),
        GOLD("6"),
        GRAY("7"),
        DARK_GRAY("8"),
        BLUE("9"),
        GREEN("a"),
        AQUA("b"),
        RED("c"),
        LIGHT_PURPLE("d"),
        YELLOW("e"),
        WHITE("f"),
        RESET("r"),
        BOLD("l"),
        UNDERLINE("n"),
        ITALIC("o"),
        STRIKETHROUGH("m");

        private final String code;

        ProgressColor(String code) {
            this.code = code;
        }

        public String getCode() {
            return "§" + this.code;
        }

        /**
         * Returns the color code without the § prefix.
         */
        public String getRawCode() {
            return this.code;
        }
    }
}
