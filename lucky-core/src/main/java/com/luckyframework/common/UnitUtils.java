package com.luckyframework.common;

import java.text.DecimalFormat;

/**
 * 单位工具类
 */
public class UnitUtils {

    /**
     * 返回日时分秒毫秒
     *
     * @param sec 秒数
     * @return 日时分秒毫秒
     */
    public static String secToTime(long sec) {
        return millisToTime(sec * 1000L);
    }


    /**
     * 返回日时分秒毫秒
     *
     * @param ms 毫秒数
     * @return 日时分秒毫秒
     */
    public static String millisToTime(long ms) {

        final long _sec = 1000L;
        final long _min = _sec * 60;
        final long _hour = _min * 60;
        final long _day = _hour * 24;


        long days = ms / _day;//转换天数
        ms = ms % _day;//剩余毫秒数

        long hours = ms / _hour;//转换小时数
        ms = ms % _hour;//剩余毫秒数

        long minutes = ms / _min;//转换分钟
        ms = ms % _min;//剩余毫秒数
        
        long seconds = ms / _sec; //转换秒数
        ms = ms % _sec; //剩余毫秒数

        if (days > 0) {
            return days + "day," + hours + "h," + minutes + "min," + seconds + "s";
        }
        if (hours > 0) {
            return hours + "h," + minutes + "min," + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "min," + seconds + "s";
        }
        if (seconds > 0) {

            return ms > 0
                    ? seconds + "s," + ms + "ms"
                    : seconds + "s";
        }
        return ms + "ms";
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
        final String[] units = new String[]{"b", "KB", "M", "G", "T"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,###.#").format(size / Math.pow(1024, digitGroups)) + units[digitGroups];
    }

    public static void main(String[] args) {
        System.out.println(millisToTime(123123));
    }
}
