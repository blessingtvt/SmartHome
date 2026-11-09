package com.smarthome.common.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类。系统统一使用 Asia/Shanghai 时区，
 * 对外接口统一使用 epoch 毫秒时间戳，入库统一使用 LocalDateTime。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** epoch 毫秒 -> LocalDateTime（Asia/Shanghai） */
    public static LocalDateTime toLocalDateTime(Long epochMillis) {
        if (epochMillis == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZONE);
    }

    /** LocalDateTime -> epoch 毫秒（Asia/Shanghai） */
    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant().toEpochMilli();
    }

    /** LocalDateTime -> 字符串，用于展示 */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }

    /** 当前时间（Asia/Shanghai） */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }
}
