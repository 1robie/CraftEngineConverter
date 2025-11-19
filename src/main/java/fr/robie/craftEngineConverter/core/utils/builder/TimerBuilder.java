package fr.robie.craftEngineConverter.core.utils.builder;


import fr.robie.craftEngineConverter.core.utils.format.Message;

import java.util.ArrayList;
import java.util.List;

public class TimerBuilder {

    public enum TimeUnit {
        YEAR(31536000L, Message.TIME_YEAR, Message.FORMAT_YEAR, Message.FORMAT_YEARS),
        MONTH(2592000L, Message.TIME_MONTH, Message.FORMAT_MONTH, Message.FORMAT_MONTHS),
        WEEK(604800L, Message.TIME_WEEK, Message.FORMAT_WEEK, Message.FORMAT_WEEKS),
        DAY(86400L, Message.TIME_DAY, Message.FORMAT_DAY, Message.FORMAT_DAYS),
        HOUR(3600L, Message.TIME_HOUR, Message.FORMAT_HOUR, Message.FORMAT_HOURS),
        MINUTE(60L, Message.TIME_MINUTE, Message.FORMAT_MINUTE, Message.FORMAT_MINUTES),
        SECOND(1L, Message.TIME_SECOND, Message.FORMAT_SECOND, Message.FORMAT_SECONDS);

        private final long seconds;
        private final Message timeMessage;
        private final Message singularFormat;
        private final Message pluralFormat;

        TimeUnit(long seconds, Message timeMessage, Message singularFormat, Message pluralFormat) {
            this.seconds = seconds;
            this.timeMessage = timeMessage;
            this.singularFormat = singularFormat;
            this.pluralFormat = pluralFormat;
        }

        public long getSeconds() { return seconds; }
        public Message getTimeMessage() { return timeMessage; }
        public String getFormat(long value) {
            return (value <= 1 ? singularFormat : pluralFormat).msg();
        }
    }

    /**
     * Format time with all units up to the specified maximum unit
     */
    public static String formatTime(long milliseconds, TimeUnit maxUnit) {
        long totalSeconds = milliseconds / 1000L;

        List<TimeUnit> unitsToInclude = getUnitsFromMaxUnit(maxUnit);

        List<Long> values = new ArrayList<>();
        long remaining = totalSeconds;

        for (TimeUnit unit : unitsToInclude) {
            long value = remaining / unit.getSeconds();
            values.add(value);
            remaining %= unit.getSeconds();
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
    public static String formatTimeAuto(long seconds) {
        if (seconds < 60) {
            return formatTime(seconds * 1000L, TimeUnit.SECOND);
        } else if (seconds < 3600) {
            return formatTime(seconds * 1000L, TimeUnit.MINUTE);
        } else if (seconds < 86400) {
            return formatTime(seconds * 1000L, TimeUnit.HOUR);
        } else if (seconds < 604800) { // Less than a week
            return formatTime(seconds * 1000L, TimeUnit.DAY);
        } else if (seconds < 2592000) { // Less than a month
            return formatTime(seconds * 1000L, TimeUnit.WEEK);
        } else if (seconds < 31536000) { // Less than a year
            return formatTime(seconds * 1000L, TimeUnit.MONTH);
        } else {
            return formatTime(seconds * 1000L, TimeUnit.YEAR);
        }
    }

    /**
     * Legacy methods for backward compatibility
     */
    public static String getFormatLongDays(long temps) {
        return formatTime(temps, TimeUnit.DAY);
    }

    public static String getFormatLongHours(long temps) {
        return formatTime(temps, TimeUnit.HOUR);
    }

    public static String getFormatLongMinutes(long temps) {
        return formatTime(temps, TimeUnit.MINUTE);
    }

    public static String getFormatLongSecondes(long temps) {
        return formatTime(temps, TimeUnit.SECOND);
    }

    public static String getStringTime(long second) {
        return formatTimeAuto(second);
    }

    /**
     * Get list of units from maxUnit down to seconds
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
            return value * defaultUnit.getSeconds() * 1000L;
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
                    totalMilliseconds += value * timeUnit.getSeconds() * 1000L;
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

        public Builder maxUnit(TimeUnit maxUnit) {
            this.maxUnit = maxUnit;
            return this;
        }

        public Builder autoSelectUnit() {
            this.autoSelect = true;
            return this;
        }

        public Builder showZeroValues() {
            this.hideZeroValues = false;
            return this;
        }

        public String build() {
            if (autoSelect) {
                return formatTimeAuto(milliseconds / 1000L);
            } else {
                String result = formatTime(milliseconds, maxUnit);
                return hideZeroValues ? format(result) : result;
            }
        }
    }
}
