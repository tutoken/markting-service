package com.monitor.utils;


import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;

@Slf4j
public class TimeUtil {
    private static final Map<String, Long> timestampMap = new HashMap<>();

    public static final ThreadLocal<DateFormat> DF = ThreadLocal.withInitial(() -> {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    });

    private static final TimeZone timeZone = TimeZone.getTimeZone("GMT+8:00");

    private static long LAST_TIMESTAMP;

    public static void MARK(String tag, String step) {
        if (timestampMap.containsKey(tag)) {
            long t = System.currentTimeMillis();
            log.info(tag + ": " + step + ", time elapse: " + MONITOR(t - LAST_TIMESTAMP));
            LAST_TIMESTAMP = t;
        } else {
            LAST_TIMESTAMP = System.currentTimeMillis();
            timestampMap.put(tag, LAST_TIMESTAMP);
            log.info(tag + " begins at " + GET_DATE_BY_STAMP(LAST_TIMESTAMP));
        }
    }

    public static void END(String tag) {
        log.info(tag + " end at " + GET_DATE_BY_STAMP(System.currentTimeMillis()) + ", time elapse " + MONITOR(System.currentTimeMillis() - timestampMap.get(tag)));
        timestampMap.clear();
    }

    public static String FORMAT(Date date, boolean isFull) {
        String d = DF.get().format(date);
        return isFull ? d.replaceAll(" ", "_") : d.substring(0, d.lastIndexOf(" "));
    }

    public static String FORMAT(Instant instant) {
        return FORMAT(Date.from(instant), false);
    }

    public static Date PARSE(String source) throws ParseException {
        return DF.get().parse(source);
    }

    public static String MONITOR(long stamp) {
        StringBuilder sb = new StringBuilder();

        // Calculate days
        long days = stamp / 86400000;
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day " : " days ");
            stamp %= 86400000;
        }

        // Calculate hours
        long hours = stamp / 3600000;
        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour " : " hours ");
            stamp %= 3600000;
        }

        // Calculate minutes
        long minutes = stamp / 60000;
        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
            stamp %= 60000;
        }

        // Calculate seconds
        long seconds = stamp / 1000;
        if (seconds > 0) {
            sb.append(seconds).append(seconds == 1 ? " second " : " seconds ");
            stamp %= 1000;
        }

        // Calculate milliseconds
        if (stamp > 0) {
            sb.append(stamp).append(" ms");
        }

        // Trim trailing space if any
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    public static String GET_DATE_BY_STAMP(long timestamp) {
        return DF.get().format(timestamp);
    }

    public static Long GET_TIME_STAMP(Date date) {
        return date.getTime();
//        long times = date.getTime();
//        return bool ? times : (times / 1000L);
    }


    public static <T> T calTimeElapse(String operation, Callable<T> callable) {
        long startTime = System.currentTimeMillis();
        try {
            return callable.call();
        } catch (Exception e) {
            log.error(String.format("%s failed, cause:", operation), e);

            return null;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            log.info(operation + " execution time: " + duration + "ms");
        }
    }

    public static Date getTime(int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        return calendar.getTime();
    }
}
