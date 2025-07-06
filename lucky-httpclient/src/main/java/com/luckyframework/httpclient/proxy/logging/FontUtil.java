package com.luckyframework.httpclient.proxy.logging;

/**
 * 抽象的日志处理器
 */
public class FontUtil {

    /**
     * 黑色
     */
    public static final String COLOR_BLACK = "30";

    /**
     * 白色
     */
    public static final String COLOR_WHITE = "37";

    /**
     * 紫红色
     */
    public static final String COLOR_MULBERRY = "35";

    /**
     * 蓝青色
     */
    public static final String COLOR_CYAN = "36";

    /**
     * 红色
     */
    public static final String COLOR_RED = "31";

    /**
     * 黄色
     */
    public static final String COLOR_YELLOW = "33";

    /**
     * 绿色
     */
    public static final String COLOR_GREEN = "32";

    /**
     * 蓝色
     */
    public static final String BLUE = "34";


    //-----------------------------------------------------------------------------------------
    //                                      前景色
    //-----------------------------------------------------------------------------------------

    public static String getColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, false);
    }

    public static String getBlackStr(String txt) {
        return getColorStr(COLOR_BLACK, txt);
    }

    public static String getWhiteStr(String txt) {
        return getColorStr(COLOR_WHITE, txt);
    }

    public static String getMulberryStr(String txt) {
        return getColorStr(COLOR_MULBERRY, txt);
    }

    public static String getCyanStr(String txt) {
        return getColorStr(COLOR_CYAN, txt);
    }

    public static String getRedStr(String txt) {
        return getColorStr(COLOR_RED, txt);
    }

    public static String getYellowStr(String txt) {
        return getColorStr(COLOR_YELLOW, txt);
    }

    public static String getGreenStr(String txt) {
        return getColorStr(COLOR_GREEN, txt);
    }

    public static String getBlueStr(String txt) {
        return getColorStr(BLUE, txt);
    }



    //-----------------------------------------------------------------------------------------
    //                                      背景色
    //-----------------------------------------------------------------------------------------

    public static String getBackColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, true);
    }

    public static String getBackBlackStr(String txt) {
        return getBackColorStr(COLOR_BLACK, txt);
    }

    public static String getBackWhiteStr(String txt) {
        return getBackColorStr(COLOR_WHITE, txt);
    }

    public static String getBackMulberryStr(String txt) {
        return getBackColorStr(COLOR_MULBERRY, txt);
    }

    public static String getBackCyanStr(String txt) {
        return getBackColorStr(COLOR_CYAN, txt);
    }

    public static String getBackRedStr(String txt) {
        return getBackColorStr(COLOR_RED, txt);
    }

    public static String getBackYellowStr(String txt) {
        return getBackColorStr(COLOR_YELLOW, txt);
    }

    public static String getBackGreenStr(String txt) {
        return getBackColorStr(COLOR_GREEN, txt);
    }

    public static String getBackBlueStr(String txt) {
        return getBackColorStr(BLUE, txt);
    }


    //-----------------------------------------------------------------------------------------
    //                                     下划线
    //-----------------------------------------------------------------------------------------

    public static String getUnderlineColorString(String colorCore, String text) {
        return "\033[4;1;" + colorCore + "m" + text + "\033[0m";
    }

    public static String getBlackUnderline(String txt) {
        return getUnderlineColorString(COLOR_BLACK, txt);
    }

    public static String getWhiteUnderline(String txt) {
        return getUnderlineColorString(COLOR_WHITE, txt);
    }

    public static String getMulberryUnderline(String txt) {
        return getUnderlineColorString(COLOR_MULBERRY, txt);
    }

    public static String getCyanUnderline(String txt) {
        return getUnderlineColorString(COLOR_CYAN, txt);
    }

    public static String getRedUnderline(String txt) {
        return getUnderlineColorString(COLOR_RED, txt);
    }

    public static String getYellowUnderline(String txt) {
        return getUnderlineColorString(COLOR_YELLOW, txt);
    }

    public static String getGreenUnderline(String txt) {
        return getUnderlineColorString(COLOR_GREEN, txt);
    }

    public static String getBlueUnderline(String txt) {
        return getUnderlineColorString(BLUE, txt);
    }

    //-----------------------------------------------------------------------------------------
    //                                    Private
    //-----------------------------------------------------------------------------------------

    private static String getColorString(String colorCore, String text, boolean isReversal) {
        String reversalCore = isReversal ? "7" : "1";
        return "\033[" + reversalCore + ";" + colorCore + "m" + text + "\033[0m";
    }

}
