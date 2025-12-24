package fr.robie.craftengineconverter.common.builder;

import fr.robie.craftengineconverter.common.format.Message;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class TimerBuilder {

    public enum TimeUnit {
        YEAR(31536000000L, Message.TIME_YEAR, Message.FORMAT_YEAR, Message.FORMAT_YEARS),
        MONTH(2592000000L, Message.TIME_MONTH, Message.FORMAT_MONTH, Message.FORMAT_MONTHS),
        WEEK(604800000L, Message.TIME_WEEK, Message.FORMAT_WEEK, Message.FORMAT_WEEKS),
        DAY(86400000L, Message.TIME_DAY, Message.FORMAT_DAY, Message.FORMAT_DAYS),
        HOUR(3600000L, Message.TIME_HOUR, Message.FORMAT_HOUR, Message.FORMAT_HOURS),
        MINUTE(60000L, Message.TIME_MINUTE, Message.FORMAT_MINUTE, Message.FORMAT_MINUTES),
        SECOND(1000L, Message.TIME_SECOND, Message.FORMAT_SECOND, Message.FORMAT_SECONDS),
        MILLISECOND(1L, Message.TIME_MILLISECOND, Message.FORMAT_MILLISECOND, Message.FORMAT_MILLISECONDS);

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

        public long getMilliseconds() { return milliseconds; }

        @Deprecated
        public long getSeconds() {
            return milliseconds / 1000L;
        }

        public Message getTimeMessage() { return timeMessage; }

        public String getFormat(long value) {
            return (value <= 1 ? singularFormat : pluralFormat).msg();
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

        String message = maxUnit.getTimeMessage().msg();

        for (int i = 0; i < unitsToInclude.size(); i++) {
            TimeUnit unit = unitsToInclude.get(i);
            String placeholder = "%" + unit.name().toLowerCase() + "%";
            message = message.replace(placeholder, unit.getFormat(values.get(i)));
        }

        return format(String.format(message, values.toArray()));
    }

    /**
     * Automatically choose the best time unit based on duration
     */
    @Contract(pure = true)
    public static String formatTimeAuto(long milliseconds) {
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
            message = message.replace(" 00 " + unit.singularFormat.msg(), "");
            message = message.replace(" 00 " + unit.pluralFormat.msg(), "");
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

        timeString = timeString.toLowerCase().trim();

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
                String unit = matcher.group(2).toLowerCase();

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
            case "ms", "milli", "millis", "millisecond", "milliseconds", "milliseconde", "millisecondes" -> TimeUnit.MILLISECOND;
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
            return parseTime(timeString, defaultUnit);
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
            if (autoSelect) {
                return formatTimeAuto(milliseconds);
            } else {
                String result = formatTime(milliseconds, maxUnit);
                return hideZeroValues ? format(result) : result;
            }
        }
    }
}