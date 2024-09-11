package com.luckyframework.httpclient.proxy.logging;

/**
 * 抽象的日志处理器
 */
public class FontUtil {

    /**
     * 白色
     */
    private final String COLOR_WHITE = "37";

    /**
     * 蓝青色
     */
    private final String COLOR_CYAN = "36";

    /**
     * 红色
     */
    private final String COLOR_RED = "31";

    /**
     * 黄色
     */
    private final String COLOR_YELLOW = "33";

    /**
     * 绿色
     */
    private final String COLOR_GREEN = "32";





    //-----------------------------------------------------------------------------------------
    //                                      前景色
    //-----------------------------------------------------------------------------------------

    protected String getWhiteStr(String txt) {
        return getColorStr(COLOR_WHITE, txt);
    }

    protected String getCyanStr(String txt) {
        return getColorStr(COLOR_CYAN, txt);
    }

    protected String getRedStr(String txt) {
        return getColorStr(COLOR_RED, txt);
    }

    protected String getYellowStr(String txt) {
        return getColorStr(COLOR_YELLOW, txt);
    }

    protected String getGreenStr(String txt) {
        return getColorStr(COLOR_GREEN, txt);
    }


    private String getColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, true);
    }

    //-----------------------------------------------------------------------------------------
    //                                      背景色
    //-----------------------------------------------------------------------------------------

    protected String getBackWhiteStr(String txt) {
        return getBackColorStr(COLOR_WHITE, txt);
    }

    protected String getBackCyanStr(String txt) {
        return getBackColorStr(COLOR_CYAN, txt);
    }

    protected String getBackRedStr(String txt) {
        return getBackColorStr(COLOR_RED, txt);
    }

    protected String getBackYellowStr(String txt) {
        return getBackColorStr(COLOR_YELLOW, txt);
    }

    protected String getBackGreenStr(String txt) {
        return getBackColorStr(COLOR_GREEN, txt);
    }

    //-----------------------------------------------------------------------------------------
    //                                     下划线
    //-----------------------------------------------------------------------------------------

    protected String getWhiteUnderline(String txt) {
        return getUnderlineColorString(COLOR_WHITE, txt);
    }

    protected String getCyanUnderline(String txt) {
        return getUnderlineColorString(COLOR_CYAN, txt);
    }

    protected String getRedUnderline(String txt) {
        return getUnderlineColorString(COLOR_RED, txt);
    }

    protected String getYellowUnderline(String txt) {
        return getUnderlineColorString(COLOR_YELLOW, txt);
    }

    protected String getGreenUnderline(String txt) {
        return getUnderlineColorString(COLOR_GREEN, txt);
    }

    //-----------------------------------------------------------------------------------------
    //                                    Private
    //-----------------------------------------------------------------------------------------

    private String getColorString(String colorCore, String text, boolean isReversal) {
        String reversalCore = isReversal ? "7" : "1";
        return "\033[" + reversalCore + ";" + colorCore + "m" + text + "\033[0m";
    }

    private String getUnderlineColorString(String colorCore, String text) {
        return "\033[4;1;" + colorCore + "m" + text + "\033[0m";
    }

    private String getBackColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, false);
    }

}
