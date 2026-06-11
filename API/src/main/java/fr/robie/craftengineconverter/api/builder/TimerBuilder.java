package fr.robie.craftengineconverter.api.builder;

import fr.robie.craftengineconverter.api.format.Message;
import fr.robie.messageflow.formatter.Placeholder;
import fr.robie.messageflow.logger.Logger;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerBuilder {

    public enum TimeUnit {
        YEAR(31536000000L, Message.TIME__FORMAT__YEAR, Message.TIME__UNIT__YEAR, Message.TIME__UNIT__YEARS),
        MONTH(2592000000L, Message.TIME__FORMAT__MONTH, Message.TIME__UNIT__MONTH, Message.TIME__UNIT__MONTHS),
        WEEK(604800000L, Message.TIME__FORMAT__WEEK, Message.TIME__UNIT__WEEK, Message.TIME__UNIT__WEEKS),
        DAY(86400000L, Message.TIME__FORMAT__DAY, Message.TIME__UNIT__DAY, Message.TIME__UNIT__DAYS),
        HOUR(3600000L, Message.TIME__FORMAT__HOUR, Message.TIME__UNIT__HOUR, Message.TIME__UNIT__HOURS),
        MINUTE(60000L, Message.TIME__FORMAT__MINUTE, Message.TIME__UNIT__MINUTE, Message.TIME__UNIT__MINUTES),
        SECOND(1000L, Message.TIME__FORMAT__SECOND, Message.TIME__UNIT__SECOND, Message.TIME__UNIT__SECONDS),
        MILLISECOND(1L, Message.TIME__FORMAT__MILLISECOND, Message.TIME__UNIT__MILLISECOND, Message.TIME__UNIT__MILLISECONDS);

        private final long milliseconds;
        private final Message timeMessage;
        private final Message singularFormat;
        private final Message pluralFormat;

        TimeUnit(long milliseconds, Message timeMessage, Message singularFormat, Message pluralFormat) {
            this.milliseconds = milliseconds;
            this.timeMessage = timeMessage;
            this.singularFormat = singularFormat;
            this.pluralFormat = pluralFormat;
        }

        public long getMilliseconds() {
            return this.milliseconds;
        }

        @Deprecated
        public long getSeconds() {
            return this.milliseconds / 1000L;
        }

        public Message getTimeMessage() {
            return this.timeMessage;
        }

        public String getFormat(long value) {
            return (value <= 1 ? this.singularFormat : this.pluralFormat).firstLine();
        }
    }

    /**
     * Format time with all units up to the specified maximum unit
     */
    @Contract(pure = true)
    public static String formatTime(long milliseconds, TimeUnit maxUnit) {
        List<TimeUnit> unitsToInclude = getUnitsFromMaxUnit(maxUnit);

        List<Long> values = new ArrayList<>();
        long remaining = milliseconds;

        for (TimeUnit unit : unitsToInclude) {
            long value = remaining / unit.getMilliseconds();
            values.add(value);
            remaining %= unit.getMilliseconds();
        }

        String message = maxUnit.getTimeMessage().firstLine();

        Logger.info("Formatting time: " + milliseconds + " ms with max unit: " + maxUnit.name());
        Logger.info("Calculated values: " + values);
        Placeholder.Builder placeholderBuilder = new Placeholder.Builder();
        for (int i = 0; i < unitsToInclude.size(); i++) {
            TimeUnit unit = unitsToInclude.get(i);
            placeholderBuilder.register(unit.name().toLowerCase(Locale.ROOT), unit.getFormat(values.get(i)));
            Logger.info("Registered placeholder: " + unit.name().toLowerCase(Locale.ROOT) + " -> " + unit.getFormat(values.get(i)));
        }
        Placeholder placeholder = placeholderBuilder.build();

        String parsed = placeholder.parse(message);

        try {
            return format(String.format(parsed, values.toArray()));
        } catch (Exception e) {
            Logger.error("Error formatting time: " + e.getMessage(), e);
            return format(parsed);
        }
    }

    /**
     * Automatically choose the best time unit based on duration
     */
    @Contract(pure = true)
    public static String formatTimeAuto(long milliseconds) {
        Logger.info("Formatting time: " + milliseconds + " ms");
        long seconds = milliseconds / 1000L;

        if (milliseconds < 1000) {
            return formatTime(milliseconds, TimeUnit.MILLISECOND);
        } else if (seconds < 60) {
            return formatTime(milliseconds, TimeUnit.SECOND);
        } else if (seconds < 3600) {
            return formatTime(milliseconds, TimeUnit.MINUTE);
        } else if (seconds < 86400) {
            return formatTime(milliseconds, TimeUnit.HOUR);
        } else if (seconds < 604800) {
            return formatTime(milliseconds, TimeUnit.DAY);
        } else if (seconds < 2592000) {
            return formatTime(milliseconds, TimeUnit.WEEK);
        } else if (seconds < 31536000) {
            return formatTime(milliseconds, TimeUnit.MONTH);
        } else {
            return formatTime(milliseconds, TimeUnit.YEAR);
        }
    }

    @Contract(pure = true)
    public static String getFormatLongDays(long temps) {
        return formatTime(temps, TimeUnit.DAY);
    }

    @Contract(pure = true)
    public static String getFormatLongHours(long temps) {
        return formatTime(temps, TimeUnit.HOUR);
    }

    @Contract(pure = true)
    public static String getFormatLongMinutes(long temps) {
        return formatTime(temps, TimeUnit.MINUTE);
    }

    @Contract(pure = true)
    public static String getFormatLongSecondes(long temps) {
        return formatTime(temps, TimeUnit.SECOND);
    }

    @Contract(pure = true)
    public static String getStringTime(long second) {
        return formatTimeAuto(second * 1000L);
    }

    /**
     * Get list of units from maxUnit down to smallest unit
     */
    private static List<TimeUnit> getUnitsFromMaxUnit(TimeUnit maxUnit) {
        List<TimeUnit> result = new ArrayList<>();
        boolean found = false;

        for (TimeUnit unit : TimeUnit.values()) {
            if (unit == maxUnit) {
                found = true;
            }
            if (found) {
                result.add(unit);
            }
        }

        return result;
    }

    /**
     * Remove zero values from the formatted string
     */
    @Contract(pure = true)
    public static String format(String message) {
        for (TimeUnit unit : TimeUnit.values()) {
            message = message.replace(" 00 " + unit.singularFormat.firstLine(), "");
            message = message.replace(" 00 " + unit.pluralFormat.firstLine(), "");
        }
        message = message.replaceAll("\\s+", " ").trim();

        return message;
    }

    public static long parseTime(String timeString) {
        return parseTime(timeString, TimeUnit.SECOND);
    }

    public static long parseTime(String timeString, TimeUnit defaultUnit) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0L;
        }

        timeString = timeString.toLowerCase(Locale.ROOT).trim();

        if (isNumeric(timeString)) {
            long value = Long.parseLong(timeString);
            return value * defaultUnit.getMilliseconds();
        }

        long totalMilliseconds = 0L;

        String regex = "(\\d+)\\s*([a-zA-Z]+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(timeString);

        while (matcher.find()) {
            try {
                long value = Long.parseLong(matcher.group(1));
                String unit = matcher.group(2).toLowerCase(Locale.ROOT);

                TimeUnit timeUnit = parseUnit(unit);
                if (timeUnit != null) {
                    totalMilliseconds += value * timeUnit.getMilliseconds();
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return totalMilliseconds;
    }

    private static TimeUnit parseUnit(String unit) {
        return switch (unit) {
            case "y", "year", "years", "année", "années", "an", "ans" -> TimeUnit.YEAR;
            case "mo", "month", "months", "mois" -> TimeUnit.MONTH;
            case "w", "week", "weeks", "semaine", "semaines" -> TimeUnit.WEEK;
            case "d", "day", "days", "jour", "jours" -> TimeUnit.DAY;
            case "h", "hour", "hours", "heure", "heures" -> TimeUnit.HOUR;
            case "m", "min", "minute", "minutes" -> TimeUnit.MINUTE;
            case "s", "sec", "second", "seconds", "seconde", "secondes" -> TimeUnit.SECOND;
            case "ms", "milli", "millis", "millisecond", "milliseconds", "milliseconde", "millisecondes" ->
                    TimeUnit.MILLISECOND;
            default -> null;
        };
    }

    private static boolean isNumeric(String str) {
        try {
            Long.parseLong(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parse a time string and convert it to ticks (1 tick = 50ms = 1/20 second)
     * Supports formats like:
     * - 20t (20 ticks)
     * - 20ms (20 milliseconds)
     * - 20.2s (20.2 seconds = 20 seconds + 4 ticks)
     * - 20.1m (20.1 minutes = 20 minutes + 6 seconds)
     * - Also supports h (hours), d (days), w (weeks), mo (months)
     *
     * @param timeString the time string to parse
     * @return the number of ticks
     */
    public static long parseTimeToTicks(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return 0L;
        }

        timeString = timeString.toLowerCase(Locale.ROOT).trim();

        // Check if it's a plain number (default to ticks)
        if (isNumeric(timeString)) {
            return Long.parseLong(timeString);
        }

        long totalTicks = 0L;

        // Regex to match decimal numbers followed by unit
        String regex = "(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(timeString);

        while (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2).toLowerCase(Locale.ROOT);

                long ticks = parseUnitToTicks(unit, value);
                totalTicks += ticks;
            } catch (NumberFormatException ignored) {
            }
        }

        return totalTicks;
    }

    /**
     * Parse a unit and value to ticks
     * 1 tick = 50ms = 1/20 second
     */
    private static long parseUnitToTicks(String unit, double value) {
        return switch (unit) {
            case "t", "tick", "ticks" -> (long) value;
            case "ms", "milli", "millis", "millisecond", "milliseconds" ->
                    (long) (value / 50.0);                 // 1 tick = 50ms
            case "s", "sec", "second", "seconds", "seconde", "secondes" ->
                    (long) (value * 20.0);                 // 1 second = 20 ticks
            case "m", "min", "minute", "minutes" ->
                    (long) (value * 20.0 * 60.0);                                 // 1 minute = 1200 ticks
            case "h", "hour", "hours", "heure", "heures" ->
                    (long) (value * 20.0 * 60.0 * 60.0);                  // 1 hour = 72000 ticks
            case "d", "day", "days", "jour", "jours" ->
                    (long) (value * 20.0 * 60.0 * 60.0 * 24.0);               // 1 day = 1728000 ticks
            case "w", "week", "weeks", "semaine", "semaines" ->
                    (long) (value * 20.0 * 60.0 * 60.0 * 24.0 * 7.0); // 1 week
            case "mo", "month", "months", "mois" ->
                    (long) (value * 20.0 * 60.0 * 60.0 * 24.0 * 30.0);            // 1 month (30 days)
            default -> 0L;
        };
    }

    public static class Parser {
        private final String timeString;
        private TimeUnit defaultUnit = TimeUnit.SECOND;

        public Parser(String timeString) {
            this.timeString = timeString;
        }

        public Parser defaultUnit(TimeUnit defaultUnit) {
            this.defaultUnit = defaultUnit;
            return this;
        }

        public long parse() {
            return parseTime(this.timeString, this.defaultUnit);
        }
    }

    public static class Builder {
        private final long milliseconds;
        private TimeUnit maxUnit = TimeUnit.DAY;
        private boolean autoSelect = false;
        private boolean hideZeroValues = true;

        public Builder(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        @Contract("_->this")
        public Builder maxUnit(TimeUnit maxUnit) {
            this.maxUnit = maxUnit;
            return this;
        }

        @Contract("->this")
        public Builder autoSelectUnit() {
            this.autoSelect = true;
            return this;
        }

        @Contract("->this")
        public Builder showZeroValues() {
            this.hideZeroValues = false;
            return this;
        }

        public String build() {
            if (this.autoSelect) {
                return formatTimeAuto(this.milliseconds);
            } else {
                String result = formatTime(this.milliseconds, this.maxUnit);
                return this.hideZeroValues ? format(result) : result;
            }
        }
    }
}