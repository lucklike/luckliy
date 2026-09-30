package com.luckyframework.common;

/**
 * 字体工具类，用于构建带有前景色、背景色(反色方案)、下划线样式的字符串
 * <p>
 * 颜色可通过颜色核心码(例如："31"、"38;5;208")或{@link Color}枚举指定，
 * 使用{@link Color}时可以访问全部命名色
 */
public class FontUtil {


    //-----------------------------------------------------------------------------------------
    //                                      前景色
    //-----------------------------------------------------------------------------------------

    /**
     * 获取带有前景色的字符串(颜色核心码为空时原样返回文本)
     * @param colorCore 颜色核心码(例如："31"、"38;5;208")
     * @param text      文本
     * @return 带有前景色的字符串
     */
    public static String getColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, false);
    }

    /**
     * 获取前景色为黑色的字符串
     * @param txt 文本
     * @return 前景色为黑色的字符串
     */
    public static String getBlackStr(String txt) {
        return getColorStr(Color.BLACK, txt);
    }

    /**
     * 获取前景色为白色的字符串
     * @param txt 文本
     * @return 前景色为白色的字符串
     */
    public static String getWhiteStr(String txt) {
        return getColorStr(Color.WHITE, txt);
    }

    /**
     * 获取前景色为紫红色的字符串
     * @param txt 文本
     * @return 前景色为紫红色的字符串
     */
    public static String getMulberryStr(String txt) {
        return getColorStr(Color.MULBERRY, txt);
    }

    /**
     * 获取前景色为蓝青色的字符串
     * @param txt 文本
     * @return 前景色为蓝青色的字符串
     */
    public static String getCyanStr(String txt) {
        return getColorStr(Color.CYAN, txt);
    }

    /**
     * 获取前景色为红色的字符串
     * @param txt 文本
     * @return 前景色为红色的字符串
     */
    public static String getRedStr(String txt) {
        return getColorStr(Color.RED, txt);
    }

    /**
     * 获取前景色为黄色的字符串
     * @param txt 文本
     * @return 前景色为黄色的字符串
     */
    public static String getYellowStr(String txt) {
        return getColorStr(Color.YELLOW, txt);
    }

    /**
     * 获取前景色为绿色的字符串
     * @param txt 文本
     * @return 前景色为绿色的字符串
     */
    public static String getGreenStr(String txt) {
        return getColorStr(Color.GREEN, txt);
    }

    /**
     * 获取前景色为蓝色的字符串
     * @param txt 文本
     * @return 前景色为蓝色的字符串
     */
    public static String getBlueStr(String txt) {
        return getColorStr(Color.BLUE, txt);
    }

    /**
     * 获取前景色为橙色的字符串
     * @param txt 文本
     * @return 前景色为橙色的字符串
     */
    public static String getOrangeStr(String txt) {
        return getColorStr(Color.ORANGE, txt);
    }

    /**
     * 获取前景色为金色的字符串
     * @param txt 文本
     * @return 前景色为金色的字符串
     */
    public static String getGoldStr(String txt) {
        return getColorStr(Color.GOLD, txt);
    }

    /**
     * 获取前景色为粉色的字符串
     * @param txt 文本
     * @return 前景色为粉色的字符串
     */
    public static String getPinkStr(String txt) {
        return getColorStr(Color.PINK, txt);
    }

    /**
     * 获取前景色为紫色的字符串
     * @param txt 文本
     * @return 前景色为紫色的字符串
     */
    public static String getPurpleStr(String txt) {
        return getColorStr(Color.PURPLE, txt);
    }

    /**
     * 获取前景色为灰色的字符串
     * @param txt 文本
     * @return 前景色为灰色的字符串
     */
    public static String getGrayStr(String txt) {
        return getColorStr(Color.GRAY, txt);
    }

    /**
     * 获取前景色为深灰色的字符串
     * @param txt 文本
     * @return 前景色为深灰色的字符串
     */
    public static String getDarkGrayStr(String txt) {
        return getColorStr(Color.DARK_GRAY, txt);
    }

    /**
     * 获取前景色为浅灰色的字符串
     * @param txt 文本
     * @return 前景色为浅灰色的字符串
     */
    public static String getLightGrayStr(String txt) {
        return getColorStr(Color.LIGHT_GRAY, txt);
    }

    /**
     * 获取前景色为天蓝色的字符串
     * @param txt 文本
     * @return 前景色为天蓝色的字符串
     */
    public static String getSkyBlueStr(String txt) {
        return getColorStr(Color.SKY_BLUE, txt);
    }

    /**
     * 获取前景色为青柠绿的字符串
     * @param txt 文本
     * @return 前景色为青柠绿的字符串
     */
    public static String getLimeStr(String txt) {
        return getColorStr(Color.LIME, txt);
    }

    /**
     * 获取前景色为青绿色的字符串
     * @param txt 文本
     * @return 前景色为青绿色的字符串
     */
    public static String getTealStr(String txt) {
        return getColorStr(Color.TEAL, txt);
    }

    /**
     * 获取前景色为棕色的字符串
     * @param txt 文本
     * @return 前景色为棕色的字符串
     */
    public static String getBrownStr(String txt) {
        return getColorStr(Color.BROWN, txt);
    }



    //-----------------------------------------------------------------------------------------
    //                                      背景色
    //-----------------------------------------------------------------------------------------

    /**
     * 获取带有背景色的字符串，采用反色方案(颜色核心码为空时原样返回文本)
     * @param colorCore 颜色核心码(例如："31"、"38;5;208")
     * @param text      文本
     * @return 带有背景色的字符串
     */
    public static String getBackColorStr(String colorCore, String text) {
        return getColorString(colorCore, text, true);
    }

    /**
     * 获取背景色为黑色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为黑色的字符串
     */
    public static String getBackBlackStr(String txt) {
        return getBackColorStr(Color.BLACK, txt);
    }

    /**
     * 获取背景色为白色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为白色的字符串
     */
    public static String getBackWhiteStr(String txt) {
        return getBackColorStr(Color.WHITE, txt);
    }

    /**
     * 获取背景色为紫红色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为紫红色的字符串
     */
    public static String getBackMulberryStr(String txt) {
        return getBackColorStr(Color.MULBERRY, txt);
    }

    /**
     * 获取背景色为蓝青色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为蓝青色的字符串
     */
    public static String getBackCyanStr(String txt) {
        return getBackColorStr(Color.CYAN, txt);
    }

    /**
     * 获取背景色为红色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为红色的字符串
     */
    public static String getBackRedStr(String txt) {
        return getBackColorStr(Color.RED, txt);
    }

    /**
     * 获取背景色为黄色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为黄色的字符串
     */
    public static String getBackYellowStr(String txt) {
        return getBackColorStr(Color.YELLOW, txt);
    }

    /**
     * 获取背景色为绿色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为绿色的字符串
     */
    public static String getBackGreenStr(String txt) {
        return getBackColorStr(Color.GREEN, txt);
    }

    /**
     * 获取背景色为蓝色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为蓝色的字符串
     */
    public static String getBackBlueStr(String txt) {
        return getBackColorStr(Color.BLUE, txt);
    }

    /**
     * 获取背景色为橙色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为橙色的字符串
     */
    public static String getBackOrangeStr(String txt) {
        return getBackColorStr(Color.ORANGE, txt);
    }

    /**
     * 获取背景色为金色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为金色的字符串
     */
    public static String getBackGoldStr(String txt) {
        return getBackColorStr(Color.GOLD, txt);
    }

    /**
     * 获取背景色为粉色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为粉色的字符串
     */
    public static String getBackPinkStr(String txt) {
        return getBackColorStr(Color.PINK, txt);
    }

    /**
     * 获取背景色为紫色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为紫色的字符串
     */
    public static String getBackPurpleStr(String txt) {
        return getBackColorStr(Color.PURPLE, txt);
    }

    /**
     * 获取背景色为灰色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为灰色的字符串
     */
    public static String getBackGrayStr(String txt) {
        return getBackColorStr(Color.GRAY, txt);
    }

    /**
     * 获取背景色为深灰色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为深灰色的字符串
     */
    public static String getBackDarkGrayStr(String txt) {
        return getBackColorStr(Color.DARK_GRAY, txt);
    }

    /**
     * 获取背景色为浅灰色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为浅灰色的字符串
     */
    public static String getBackLightGrayStr(String txt) {
        return getBackColorStr(Color.LIGHT_GRAY, txt);
    }

    /**
     * 获取背景色为天蓝色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为天蓝色的字符串
     */
    public static String getBackSkyBlueStr(String txt) {
        return getBackColorStr(Color.SKY_BLUE, txt);
    }

    /**
     * 获取背景色为青柠绿的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为青柠绿的字符串
     */
    public static String getBackLimeStr(String txt) {
        return getBackColorStr(Color.LIME, txt);
    }

    /**
     * 获取背景色为青绿色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为青绿色的字符串
     */
    public static String getBackTealStr(String txt) {
        return getBackColorStr(Color.TEAL, txt);
    }

    /**
     * 获取背景色为棕色的字符串(反色方案)
     * @param txt 文本
     * @return 背景色为棕色的字符串
     */
    public static String getBackBrownStr(String txt) {
        return getBackColorStr(Color.BROWN, txt);
    }


    //-----------------------------------------------------------------------------------------
    //                                     下划线
    //-----------------------------------------------------------------------------------------

    /**
     * 获取带有下划线样式的字符串(颜色核心码为空时原样返回文本)
     * @param colorCore 颜色核心码(例如："31"、"38;5;208")
     * @param text      文本
     * @return 带有下划线样式的字符串
     */
    public static String getUnderlineColorString(String colorCore, String text) {
        if (colorCore == null || colorCore.isEmpty()) {
            return text;
        }
        return "\033[4;1;" + colorCore + "m" + text + "\033[0m";
    }

    /**
     * 获取带有黑色下划线样式的字符串
     * @param txt 文本
     * @return 带有黑色下划线样式的字符串
     */
    public static String getBlackUnderline(String txt) {
        return getUnderlineColorString(Color.BLACK, txt);
    }

    /**
     * 获取带有白色下划线样式的字符串
     * @param txt 文本
     * @return 带有白色下划线样式的字符串
     */
    public static String getWhiteUnderline(String txt) {
        return getUnderlineColorString(Color.WHITE, txt);
    }

    /**
     * 获取带有紫红色下划线样式的字符串
     * @param txt 文本
     * @return 带有紫红色下划线样式的字符串
     */
    public static String getMulberryUnderline(String txt) {
        return getUnderlineColorString(Color.MULBERRY, txt);
    }

    /**
     * 获取带有蓝青色下划线样式的字符串
     * @param txt 文本
     * @return 带有蓝青色下划线样式的字符串
     */
    public static String getCyanUnderline(String txt) {
        return getUnderlineColorString(Color.CYAN, txt);
    }

    /**
     * 获取带有红色下划线样式的字符串
     * @param txt 文本
     * @return 带有红色下划线样式的字符串
     */
    public static String getRedUnderline(String txt) {
        return getUnderlineColorString(Color.RED, txt);
    }

    /**
     * 获取带有黄色下划线样式的字符串
     * @param txt 文本
     * @return 带有黄色下划线样式的字符串
     */
    public static String getYellowUnderline(String txt) {
        return getUnderlineColorString(Color.YELLOW, txt);
    }

    /**
     * 获取带有绿色下划线样式的字符串
     * @param txt 文本
     * @return 带有绿色下划线样式的字符串
     */
    public static String getGreenUnderline(String txt) {
        return getUnderlineColorString(Color.GREEN, txt);
    }

    /**
     * 获取带有蓝色下划线样式的字符串
     * @param txt 文本
     * @return 带有蓝色下划线样式的字符串
     */
    public static String getBlueUnderline(String txt) {
        return getUnderlineColorString(Color.BLUE, txt);
    }

    /**
     * 获取带有橙色下划线样式的字符串
     * @param txt 文本
     * @return 带有橙色下划线样式的字符串
     */
    public static String getOrangeUnderline(String txt) {
        return getUnderlineColorString(Color.ORANGE, txt);
    }

    /**
     * 获取带有金色下划线样式的字符串
     * @param txt 文本
     * @return 带有金色下划线样式的字符串
     */
    public static String getGoldUnderline(String txt) {
        return getUnderlineColorString(Color.GOLD, txt);
    }

    /**
     * 获取带有粉色下划线样式的字符串
     * @param txt 文本
     * @return 带有粉色下划线样式的字符串
     */
    public static String getPinkUnderline(String txt) {
        return getUnderlineColorString(Color.PINK, txt);
    }

    /**
     * 获取带有紫色下划线样式的字符串
     * @param txt 文本
     * @return 带有紫色下划线样式的字符串
     */
    public static String getPurpleUnderline(String txt) {
        return getUnderlineColorString(Color.PURPLE, txt);
    }

    /**
     * 获取带有灰色下划线样式的字符串
     * @param txt 文本
     * @return 带有灰色下划线样式的字符串
     */
    public static String getGrayUnderline(String txt) {
        return getUnderlineColorString(Color.GRAY, txt);
    }

    /**
     * 获取带有深灰色下划线样式的字符串
     * @param txt 文本
     * @return 带有深灰色下划线样式的字符串
     */
    public static String getDarkGrayUnderline(String txt) {
        return getUnderlineColorString(Color.DARK_GRAY, txt);
    }

    /**
     * 获取带有浅灰色下划线样式的字符串
     * @param txt 文本
     * @return 带有浅灰色下划线样式的字符串
     */
    public static String getLightGrayUnderline(String txt) {
        return getUnderlineColorString(Color.LIGHT_GRAY, txt);
    }

    /**
     * 获取带有天蓝色下划线样式的字符串
     * @param txt 文本
     * @return 带有天蓝色下划线样式的字符串
     */
    public static String getSkyBlueUnderline(String txt) {
        return getUnderlineColorString(Color.SKY_BLUE, txt);
    }

    /**
     * 获取带有青柠绿下划线样式的字符串
     * @param txt 文本
     * @return 带有青柠绿下划线样式的字符串
     */
    public static String getLimeUnderline(String txt) {
        return getUnderlineColorString(Color.LIME, txt);
    }

    /**
     * 获取带有青绿色下划线样式的字符串
     * @param txt 文本
     * @return 带有青绿色下划线样式的字符串
     */
    public static String getTealUnderline(String txt) {
        return getUnderlineColorString(Color.TEAL, txt);
    }

    /**
     * 获取带有棕色下划线样式的字符串
     * @param txt 文本
     * @return 带有棕色下划线样式的字符串
     */
    public static String getBrownUnderline(String txt) {
        return getUnderlineColorString(Color.BROWN, txt);
    }

    //-----------------------------------------------------------------------------------------
    //                                Color枚举通用入口
    //-----------------------------------------------------------------------------------------

    /**
     * 获取带有前景色的字符串
     * @param color 颜色(支持Color枚举中的全部命名色)
     * @param text  文本
     */
    public static String getColorStr(Color color, String text) {
        return getColorString(color.getColorCore(), text, false);
    }

    /**
     * 获取带有背景色的字符串(采用反色方案)
     * @param color 颜色(支持Color枚举中的全部命名色)
     * @param text  文本
     */
    public static String getBackColorStr(Color color, String text) {
        return getColorString(color.getColorCore(), text, true);
    }

    /**
     * 获取带有下划线样式的字符串
     * @param color 颜色(支持Color枚举中的全部命名色)
     * @param text  文本
     */
    public static String getUnderlineColorString(Color color, String text) {
        return getUnderlineColorString(color.getColorCore(), text);
    }

    //-----------------------------------------------------------------------------------------
    //                                    Private
    //-----------------------------------------------------------------------------------------

    /**
     * 根据颜色核心码构建带有颜色样式的字符串
     * @param colorCore  颜色核心码(为空时原样返回文本)
     * @param text       文本
     * @param isReversal 是否采用反色方案(true-背景色，false-前景色)
     * @return 带有颜色样式的字符串
     */
    private static String getColorString(String colorCore, String text, boolean isReversal) {
        if (colorCore == null || colorCore.isEmpty()) {
            return text;
        }
        String reversalCore = isReversal ? "7" : "1";
        return "\033[" + reversalCore + ";" + colorCore + "m" + text + "\033[0m";
    }

    //-----------------------------------------------------------------------------------------
    //                                    Main(预览测试)
    //-----------------------------------------------------------------------------------------

    /**
     * 预览测试入口：调用全部方法，将带颜色的文字直接输出到控制台
     * @param args 启动参数(忽略)
     */
    public static void main(String[] args) {
        System.out.println("=================== 前景色(19色) ===================");
        System.out.println(getBlackStr("黑色 BLACK"));
        System.out.println(getWhiteStr("白色 WHITE"));
        System.out.println(getMulberryStr("紫红色 MULBERRY"));
        System.out.println(getCyanStr("蓝青色 CYAN"));
        System.out.println(getRedStr("红色 RED"));
        System.out.println(getYellowStr("黄色 YELLOW"));
        System.out.println(getGreenStr("绿色 GREEN"));
        System.out.println(getBlueStr("蓝色 BLUE"));
        System.out.println(getOrangeStr("橙色 ORANGE"));
        System.out.println(getGoldStr("金色 GOLD"));
        System.out.println(getPinkStr("粉色 PINK"));
        System.out.println(getPurpleStr("紫色 PURPLE"));
        System.out.println(getGrayStr("灰色 GRAY"));
        System.out.println(getDarkGrayStr("深灰色 DARK_GRAY"));
        System.out.println(getLightGrayStr("浅灰色 LIGHT_GRAY"));
        System.out.println(getSkyBlueStr("天蓝色 SKY_BLUE"));
        System.out.println(getLimeStr("青柠绿 LIME"));
        System.out.println(getTealStr("青绿色 TEAL"));
        System.out.println(getBrownStr("棕色 BROWN"));

        System.out.println("=================== 背景色(反色方案，19色) ===================");
        System.out.println(getBackBlackStr(" 黑色 BLACK "));
        System.out.println(getBackWhiteStr(" 白色 WHITE "));
        System.out.println(getBackMulberryStr(" 紫红色 MULBERRY "));
        System.out.println(getBackCyanStr(" 蓝青色 CYAN "));
        System.out.println(getBackRedStr(" 红色 RED "));
        System.out.println(getBackYellowStr(" 黄色 YELLOW "));
        System.out.println(getBackGreenStr(" 绿色 GREEN "));
        System.out.println(getBackBlueStr(" 蓝色 BLUE "));
        System.out.println(getBackOrangeStr(" 橙色 ORANGE "));
        System.out.println(getBackGoldStr(" 金色 GOLD "));
        System.out.println(getBackPinkStr(" 粉色 PINK "));
        System.out.println(getBackPurpleStr(" 紫色 PURPLE "));
        System.out.println(getBackGrayStr(" 灰色 GRAY "));
        System.out.println(getBackDarkGrayStr(" 深灰色 DARK_GRAY "));
        System.out.println(getBackLightGrayStr(" 浅灰色 LIGHT_GRAY "));
        System.out.println(getBackSkyBlueStr(" 天蓝色 SKY_BLUE "));
        System.out.println(getBackLimeStr(" 青柠绿 LIME "));
        System.out.println(getBackTealStr(" 青绿色 TEAL "));
        System.out.println(getBackBrownStr(" 棕色 BROWN "));

        System.out.println("=================== 下划线(19色) ===================");
        System.out.println(getBlackUnderline("黑色 BLACK"));
        System.out.println(getWhiteUnderline("白色 WHITE"));
        System.out.println(getMulberryUnderline("紫红色 MULBERRY"));
        System.out.println(getCyanUnderline("蓝青色 CYAN"));
        System.out.println(getRedUnderline("红色 RED"));
        System.out.println(getYellowUnderline("黄色 YELLOW"));
        System.out.println(getGreenUnderline("绿色 GREEN"));
        System.out.println(getBlueUnderline("蓝色 BLUE"));
        System.out.println(getOrangeUnderline("橙色 ORANGE"));
        System.out.println(getGoldUnderline("金色 GOLD"));
        System.out.println(getPinkUnderline("粉色 PINK"));
        System.out.println(getPurpleUnderline("紫色 PURPLE"));
        System.out.println(getGrayUnderline("灰色 GRAY"));
        System.out.println(getDarkGrayUnderline("深灰色 DARK_GRAY"));
        System.out.println(getLightGrayUnderline("浅灰色 LIGHT_GRAY"));
        System.out.println(getSkyBlueUnderline("天蓝色 SKY_BLUE"));
        System.out.println(getLimeUnderline("青柠绿 LIME"));
        System.out.println(getTealUnderline("青绿色 TEAL"));
        System.out.println(getBrownUnderline("棕色 BROWN"));

        System.out.println("=================== 字符串核心码通用入口 ===================");
        System.out.println(getColorStr("31", "核心码31-前景色"));
        System.out.println(getBackColorStr("31", " 核心码31-背景色 "));
        System.out.println(getUnderlineColorString("31", "核心码31-下划线"));
        System.out.println(getColorStr("38;5;208", "核心码38;5;208-256色"));
        System.out.println(getColorStr("", "[空核心码]原样返回文本"));
        System.out.println(getBackColorStr((String) null, "[null核心码]原样返回文本"));
        System.out.println(getUnderlineColorString((String) null, "[null核心码]原样返回文本"));

        System.out.println("=================== Color枚举通用入口(遍历全部20色) ===================");
        for (Color color : Color.values()) {
            System.out.println(getColorStr(color, color.name() + "-前景")
                    + " | "
                    + getBackColorStr(color, " " + color.name() + "-背景 ")
                    + " | "
                    + getUnderlineColorString(color, color.name() + "-下划线"));
        }
    }

}
