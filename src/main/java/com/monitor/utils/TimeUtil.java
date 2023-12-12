package com.monitor.utils;


import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;

@Slf4j
public class TimeUtil {
    private static final Map<String, Long> timestampMap = new HashMap<>();

    public static final ThreadLocal<DateFormat> DF = ThreadLocal.withInitial(() -> {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    });
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
        // within 1 s
        if (stamp < 1000) {
            return stamp + " ms";
        }
        // within 1 min
        else if (stamp < 60000) {
            return stamp / 1000 + " seconds " + MONITOR(stamp % 1000);
        }
        // within 1 hour
        else if (stamp < 3600000) {
            return stamp / 60000 + " minutes " + MONITOR(stamp % 60000);
        }
        // within 1 day
        else if (stamp < 86400000) {
            return stamp / 3600000 + " hours " + MONITOR(stamp % 3600000);
        }
        // longer
        else {
            return stamp / 86400000 + " days " + MONITOR(stamp % 86400000);
        }
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
}
