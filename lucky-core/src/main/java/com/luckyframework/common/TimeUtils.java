package com.luckyframework.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间工具类
 */
public class TimeUtils {
    public static  final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static  final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static  final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static  final DateTimeFormatter YYYYMMDDHHMMSS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    public static String format_yyyy_MM_dd_HHmmss(LocalDateTime LocalDateTime) {
        return YYYY_MM_DD_HH_MM_SS.format(LocalDateTime);
    }

    public static String format_yyyy_MM_dd_HHmmss() {
        return format_yyyy_MM_dd_HHmmss(LocalDateTime.now());
    }

    public static String format_yyyy_MM_dd(LocalDateTime LocalDateTime) {
        return YYYY_MM_DD.format(LocalDateTime);
    }

    public static String format_yyyy_MM_dd() {
        return format_yyyy_MM_dd(LocalDateTime.now());
    }

    public static String formatYyyyMMdd(LocalDateTime LocalDateTime) {
        return YYYYMMDD.format(LocalDateTime);
    }

    public static String formatYyyyMMddHhmmss() {
        return YYYYMMDDHHMMSS.format(LocalDateTime.now());
    }

    public static String formatYyyyMMddHhmmss(LocalDateTime LocalDateTime) {
        return YYYYMMDDHHMMSS.format(LocalDateTime);
    }

    public static String formatYyyyMMdd() {
        return formatYyyyMMdd(LocalDateTime.now());
    }

    public static String format_yyyy_MM_dd_HHmmss(Date date) {
        return format_yyyy_MM_dd_HHmmss(toLocalDateTime(date));
    }

    public static String format_yyyy_MM_dd(Date date) {
        return format_yyyy_MM_dd(toLocalDateTime(date));
    }

    public static String formatYyyyMMdd(Date date) {
        return formatYyyyMMdd(toLocalDateTime(date));
    }

    public static LocalDateTime toLocalDateTime(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDateTime();
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDateTime LocalDateTime, ZoneId zoneId) {
        return Date.from(LocalDateTime.atZone(zoneId).toInstant());
    }

    public static Date toDate(LocalDateTime LocalDateTime) {
        return Date.from(LocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
