package com.luckyframework.common;

import java.text.DecimalFormat;

/**
 * 单位工具类
 */
public class UnitUtils {
    private static final long FILE_UNIT = 1024;
    private static final long TIME_UNIT = 60;


    /**
     * 返回日时分秒
     *
     * @param second 秒数
     * @return 日时分秒
     */
    public static String secondToTime(long second) {

        long days = second / 86400;//转换天数
        second = second % 86400;//剩余秒数

        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数

        long minutes = second / 60;//转换分钟
        second = second % 60;//剩余秒数

        if (days > 0) {
            return days + "day" + hours + "h" + minutes + "min" + second + "s";
        }
        if (hours > 0) {
            return hours + "h" + minutes + "min" + second + "s";
        }
        if (minutes > 0) {
            return minutes + "min" + second + "s";
        }
        return second + "s";
    }


    /**
     * 文件大小智能转换
     * 会将文件大小转换为最大满足单位
     *
     * @param size（文件大小，单位为B）
     * @return 文件大小
     */
    public static String byteTo(long size) {
        if (size <= 0) {
            return "0";
        }
        final String[] units = new String[]{"b", "kb", "M", "G", "T"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,###.#").format(size / Math.pow(1024, digitGroups)) + units[digitGroups];
    }

    public static void main(String[] args) {
        System.out.println(secondToTime(123123));
    }
}
