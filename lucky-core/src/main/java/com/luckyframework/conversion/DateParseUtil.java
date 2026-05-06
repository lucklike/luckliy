package com.luckyframework.conversion;

import com.luckyframework.common.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 常用时间字符串转Date的工具类
 */
public class DateParseUtil {

    private static final Map<Pattern, String> DATE_PATTERN_FORMATS = new HashMap<>();

    static {
        // 初始化正则表达式和对应格式
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$"), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"), "yyyy-MM-dd'T'HH:mm:ss'Z'");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm:ss");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"), "yyyy-MM-dd");

        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm:ss");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$"), "yyyy/MM/dd");

        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}$"), "MM/dd/yyyy HH:mm:ss");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}$"), "MM/dd/yyyy HH:mm");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$"), "MM/dd/yyyy");

        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}$"), "dd-MM-yyyy HH:mm:ss");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}$"), "dd-MM-yyyy HH:mm");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{2}-\\d{2}-\\d{4}$"), "dd-MM-yyyy");

        // 中文格式
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}年\\d{1,2}月\\d{1,2}日 \\d{1,2}时\\d{2}分\\d{2}秒$"), "yyyy年M月d日 HH时mm分ss秒");
        DATE_PATTERN_FORMATS.put(Pattern.compile("^\\d{4}年\\d{1,2}月\\d{1,2}日$"), "yyyy年M月d日");
    }

    // 自定义格式
    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_TIME,

            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE,
            DateTimeFormatter.ISO_OFFSET_TIME,

            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ISO_TIME,

            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_ORDINAL_DATE,
            DateTimeFormatter.ISO_WEEK_DATE,
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    };


    /**
     * 自动识别格式并转换为Date
     *
     * @param dateStr 时间字符串
     * @return Date对象，转换失败返回null
     */
    public static Date parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        dateStr = dateStr.trim();

        // 1. 处理时间戳
        if (isNumeric(dateStr)) {
            return parseTimestamp(dateStr);
        }

        // 2. 尝试正则匹配格式
        String format = determineDateFormat(dateStr);
        if (format != null) {
            Date date = parseWithFormat(dateStr, format);
            if (date != null) {
                return date;
            }
        }

        // 3. 尝试Java 8 Time API
        try {
            LocalDateTime localDateTime = parseWithJava8(dateStr);
            if (localDateTime != null) {
                return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            }
        } catch (Exception ignored) {
        }

        // 4. 最后尝试默认解析
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (ParseException ignored) {
        }

        try {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(dateStr);
        } catch (ParseException ignored) {
        }

        return null;
    }

    /**
     * 指定格式解析
     *
     * @param dateStr 时间字符串
     * @param format  格式，如 yyyy-MM-dd HH:mm:ss
     * @return Date对象，转换失败返回null
     */
    public static Date parse(String dateStr, String format) {
        if (dateStr == null || dateStr.trim().isEmpty() || format == null) {
            return null;
        }
        return parseWithFormat(dateStr.trim(), format);
    }

    /**
     * 批量解析，返回第一个成功的结果
     *
     * @param dateStr 时间字符串
     * @param formats 尝试的格式数组
     * @return Date对象，转换失败返回null
     */
    public static Date parseWithFormats(String dateStr, String... formats) {
        if (dateStr == null || dateStr.trim().isEmpty() || formats == null) {
            return null;
        }

        dateStr = dateStr.trim();

        for (String format : formats) {
            Date date = parseWithFormat(dateStr, format);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    /**
     * 使用指定格式解析日期
     */
    private static Date parseWithFormat(String dateStr, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 判断时间字符串格式
     */
    private static String determineDateFormat(String dateStr) {
        for (Map.Entry<Pattern, String> entry : DATE_PATTERN_FORMATS.entrySet()) {
            if (entry.getKey().matcher(dateStr).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析时间戳
     */
    private static Date parseTimestamp(String timestampStr) {
        try {
            long timestamp = Long.parseLong(timestampStr);
            // 判断是秒还是毫秒
            if (timestamp > 1000000000000L) {
                return new Date(timestamp);
            } else if (timestamp > 1000000000L) {
                return new Date(timestamp * 1000);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * 使用Java 8 Time API解析
     */
    private static LocalDateTime parseWithJava8(String dateStr) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    /**
     * 判断字符串是否为数字
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 格式化日期为字符串
     */
    public static String format(Date date, String format) {
        if (date == null || format == null) {
            return null;
        }
        return DateUtils.time(date, format);
    }

    /**
     * 日期时间字符串标准化
     */
    public static String normalize(String dateStr) {
        Date date = parse(dateStr);
        if (date != null) {
            return format(date, "yyyy-MM-dd HH:mm:ss");
        }
        return null;
    }
}