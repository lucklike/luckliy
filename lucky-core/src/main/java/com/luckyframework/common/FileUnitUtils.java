package com.luckyframework.common;

import com.luckyframework.conversion.JavaConversion;

/**
 * 文件单位转换工具类
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/3/9 下午11:04
 */
public abstract class FileUnitUtils {

    private final static long UNIT = 1024;

    public static long toKb(String fileUnit) {
        String size = fileUnit;
        size = size.trim().toUpperCase();
        if (size.endsWith("KB")) {
            return JavaConversion.fromString(getNumber(size, "KB"), long.class, true);
        }

        if (size.endsWith("K")) {
            return JavaConversion.fromString(getNumber(size, "K"), long.class, true);
        }

        if (size.endsWith("MB")) {
            return mbToKkb(JavaConversion.fromString(getNumber(size, "MB"), long.class, true));
        }

        if (size.endsWith("M")) {
            return mbToKkb(JavaConversion.fromString(getNumber(size, "M"), long.class, true));
        }

        if (size.endsWith("GB")) {
            return gbToKb(JavaConversion.fromString(getNumber(size, "GB"), long.class, true));
        }

        if (size.endsWith("G")) {
            return gbToKb(JavaConversion.fromString(getNumber(size, "G"), long.class, true));
        }
        if (size.endsWith("TB")) {
            return tbToKb(JavaConversion.fromString(getNumber(size, "TB"), long.class, true));
        }

        if (size.endsWith("T")) {
            return tbToKb(JavaConversion.fromString(getNumber(size, "T"), long.class, true));
        }
        try {
            return JavaConversion.fromString(size, long.class, true);
        } catch (Exception e) {
            throw new RuntimeException("Wrong file unit: `" + fileUnit + "`");
        }
    }

    public static long mbToKkb(long mb) {
        return mb * UNIT;
    }

    public static long gbToKb(long gb) {
        return gb * UNIT * UNIT;
    }

    public static long tbToKb(long tb) {
        return tb * UNIT * UNIT * UNIT;
    }

    private static String getNumber(String baseData, String unit) {
        return baseData.substring(0, baseData.length() - unit.length()).trim();
    }

    public static void main(String[] args) {
        String fileUnit = "(15*4-20)T";
        System.out.println(toKb(fileUnit));
        long i = 1024 * 1024 * 1024;
        System.out.println(i);
    }

}
