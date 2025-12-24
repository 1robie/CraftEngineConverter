package fr.robie.craftengineconverter.common.progress;

@SuppressWarnings("UnusedReturnValue")
public interface ProgressBarUtils {
    BukkitProgressBar.ProgressColor getProgressColor();
    BukkitProgressBar.ProgressColor getEmptyColor();
    BukkitProgressBar.ProgressColor getPercentColor();
    char getProgressChar();
    char getEmptyChar();
    int getBarWidth();

    ProgressBarUtils setProgressColor(BukkitProgressBar.ProgressColor color);
    ProgressBarUtils setEmptyColor(BukkitProgressBar.ProgressColor color);
    ProgressBarUtils setPercentColor(BukkitProgressBar.ProgressColor color);
    ProgressBarUtils setProgressChar(char progressChar);
    ProgressBarUtils setEmptyChar(char emptyChar);
    ProgressBarUtils setBarWidth(int barWidth);
}
