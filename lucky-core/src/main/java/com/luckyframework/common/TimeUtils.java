package com.luckyframework.common;

import java.time.LocalDate;
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


    public static String format_yyyy_MM_dd_HHmmss(LocalDate localDate) {
        return YYYY_MM_DD_HH_MM_SS.format(localDate);
    }

    public static String format_yyyy_MM_dd_HHmmss() {
        return format_yyyy_MM_dd_HHmmss(LocalDate.now());
    }

    public static String format_yyyy_MM_dd(LocalDate localDate) {
        return YYYY_MM_DD.format(localDate);
    }

    public static String format_yyyy_MM_dd() {
        return format_yyyy_MM_dd(LocalDate.now());
    }

    public static String formatYyyyMMdd(LocalDate localDate) {
        return YYYYMMDD.format(localDate);
    }

    public static String formatYyyyMMdd() {
        return formatYyyyMMdd(LocalDate.now());
    }

    public static String format_yyyy_MM_dd_HHmmss(Date date) {
        return format_yyyy_MM_dd_HHmmss(toLocalDate(date));
    }

    public static String format_yyyy_MM_dd(Date date) {
        return format_yyyy_MM_dd(toLocalDate(date));
    }

    public static String formatYyyyMMdd(Date date) {
        return formatYyyyMMdd(toLocalDate(date));
    }

    public static LocalDate toLocalDate(Date date, ZoneId zoneId) {
        return date.toInstant().atZone(zoneId).toLocalDate();
    }

    public static LocalDate toLocalDate(Date date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }

    public static Date toDate(LocalDate localDate, ZoneId zoneId) {
        return Date.from(localDate.atStartOfDay(zoneId).toInstant());
    }

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
