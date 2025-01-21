package com.aristoco.core.utils;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * 时间工具类
 * @author cgw
 */
public class TimeUtils extends LocalDateTimeUtil {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneId.systemDefault().getRules().getOffset(Instant.now());

    /**
     * 将时间戳转换为对应的时间
     * @param epochMilli
     * @return
     */
    public static LocalDateTime toDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), DEFAULT_ZONE_ID);
    }

    /**
     * 根据指定的时间获取时间戳
     * @param dateTime
     * @return
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.toInstant(DEFAULT_ZONE_OFFSET).toEpochMilli();
    }

    /**
     * 判断两个时间是否在同一天
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isSameDay(long startTime, long endTime) {
        LocalDateTime startLocalDateTime = toDateTime(startTime);
        LocalDateTime endLocalDateTime = toDateTime(endTime);
        LocalDate startLocalDate = startLocalDateTime.toLocalDate();
        LocalDate endLocalDate = endLocalDateTime.toLocalDate();
        return startLocalDate.isEqual(endLocalDate);
    }

    /**
     * 判断两个时间是否不在同一天
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isNotSameDay(long startTime, long endTime) {
        return !isSameDay(startTime, endTime);
    }

    /**
     * 判断两个时间是否在同一天
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isSameDay(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDate startLocalDate = startTime.toLocalDate();
        LocalDate endLocalDate = endTime.toLocalDate();
        return startLocalDate.isEqual(endLocalDate);
    }

    /**
     * 判断两个时间是否不在同一天
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean isNotSameDay(LocalDateTime startTime, LocalDateTime endTime) {
        return !isSameDay(startTime, endTime);
    }

    /**
     * 获取当前天
     * @return
     */
    public static int today() {
        return (int) LocalDate.now().toEpochDay();
    }

    /**
     * 获取当前utc时区字符串
     * @return
     */
    public static String getUtcOffsetHourStr() {
        int utcOffsetHour = getUtcOffsetHour();
        String str = "UTC";
        if (utcOffsetHour >= 0) {
            str += "+";
        }
        return str + utcOffsetHour;
    }

    /**
     * 获取当前utc时区
     * @return
     */
    public static int getUtcOffsetHour() {
        return DEFAULT_ZONE_OFFSET.getTotalSeconds() / 3600;
    }

    /**
     * 获取指定时间的格式化时间字符串
     * @param time
     * @return
     */
    public static String getDateFormat(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取当前时间的格式化时间字符串
     * @return
     */
    public static String getNowDateFormat() {
        return getDateFormat(LocalDateTime.now());
    }

    /**
     * 获取当前天的开始时间
     * @param time
     * @return
     */
    public static LocalDateTime getLocalDateTimeWithDayStartTime(LocalDateTime time) {
        return time.toLocalDate().atTime(LocalTime.MIN);
    }

    /**
     * 获取当前天的结束时间
     * @param time
     * @return
     */
    public static LocalDateTime getLocalDateTimeWithDayEndTime(LocalDateTime time) {
        return time.toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * 获取当前天的开始时间
     * @param time
     * @return
     */
    public static Long getTimeWithDayStartTime(LocalDateTime time) {
        return toEpochMilli(time.toLocalDate().atTime(LocalTime.MIN));
    }

    /**
     * 获取当前天的结束时间
     * @param time
     * @return
     */
    public static Long getTimeWithDayEndTime(LocalDateTime time) {
        return toEpochMilli(time.toLocalDate().atTime(LocalTime.MAX));
    }

    /**
     * 获取当前时间的周结束时间（周日的23:59:59.999）
     *
     * @param now 当前的 LocalDateTime 对象
     * @return 本周日的23:59:59.999的 LocalDateTime 对象
     */
    public static LocalDateTime getWeekEndLocalDateTime(LocalDateTime now) {
        // 使用TemporalAdjusters找到下一个或当前的周日
        LocalDateTime sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        // 返回周日的23:59:59.999，通常认为是一天的结束
        return sunday.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }

    /**
     * 获取当前时间的周结束时间（周日的23:59:59.999）
     *
     * @param now 当前的 LocalDateTime 对象
     * @return 本周日的23:59:59.999的 时间戳
     */
    public static Long getWeekEndTime(LocalDateTime now) {
        return toEpochMilli(getWeekEndLocalDateTime(now));
    }
}
