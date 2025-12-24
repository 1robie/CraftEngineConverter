package fr.robie.craftengineconverter.common.enums;

import fr.robie.craftengineconverter.common.progress.ProgressBarUtils;
import org.jetbrains.annotations.Contract;

import static fr.robie.craftengineconverter.common.progress.BukkitProgressBar.ProgressColor;

public enum ConverterOptions implements ProgressBarUtils {
    ALL(),
    ITEMS(ProgressColor.GREEN),
    EMOJIS(ProgressColor.AQUA),
    IMAGES(ProgressColor.GOLD),
    LANGUAGES(ProgressColor.BLUE),
    SOUNDS(ProgressColor.LIGHT_PURPLE),
    PACKS(ProgressColor.RED);
    private ProgressColor progressColor;
    private ProgressColor emptyColor;
    private ProgressColor percentColor;
    private char progressChar = '█';
    private char emptyChar = '░';
    private int barWidth = 50;

    ConverterOptions() {
        this.progressColor = ProgressColor.GREEN;
        this.emptyColor = ProgressColor.DARK_GRAY;
        this.percentColor = ProgressColor.YELLOW;
    }

    ConverterOptions(ProgressColor progressColor) {
        this.progressColor = progressColor;
        this.emptyColor = ProgressColor.DARK_GRAY;
        this.percentColor = ProgressColor.YELLOW;
    }

    @Override
    public ProgressColor getProgressColor() {
        return this.progressColor;
    }

    @Override
    public ProgressColor getEmptyColor() {
        return this.emptyColor;
    }

    @Override
    public ProgressColor getPercentColor() {
        return this.percentColor;
    }

    @Override
    public char getProgressChar() {
        return this.progressChar;
    }

    @Override
    public char getEmptyChar() {
        return this.emptyChar;
    }

    @Override
    public int getBarWidth() {
        return this.barWidth;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setProgressColor(ProgressColor color) {
        this.progressColor = color;
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setEmptyColor(ProgressColor color) {
        this.emptyColor = color;
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setPercentColor(ProgressColor color) {
        this.percentColor = color;
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setProgressChar(char progressChar) {
        this.progressChar = progressChar;
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setEmptyChar(char emptyChar) {
        this.emptyChar = emptyChar;
        return this;
    }

    @Override
    @Contract(value = "_ -> this", mutates = "this")
    public ProgressBarUtils setBarWidth(int barWidth) {
        this.barWidth = barWidth;
        return this;
    }

}
