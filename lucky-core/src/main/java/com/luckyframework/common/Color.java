package com.luckyframework.common;

/**
 * 控制台颜色枚举
 * <p>
 * 基础色({@link #DEFAULT}、{@link #RED}、{@link #CYAN}、{@link #MULBERRY}、{@link #YELLOW}、
 * {@link #GREEN}、{@link #WHITE}、{@link #BLUE}、{@link #BLACK})使用ANSI 4bit颜色码；
 * 扩展命名色({@link #ORANGE}、{@link #GOLD}、{@link #PINK}、{@link #PURPLE}、{@link #GRAY}、
 * {@link #DARK_GRAY}、{@link #LIGHT_GRAY}、{@link #SKY_BLUE}、{@link #LIME}、{@link #TEAL}、
 * {@link #BROWN})使用ANSI 256颜色码，需要终端支持256色(IDE控制台与现代终端均支持)
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/27 11:41
 */
public enum Color {

    /** 默认颜色(无着色)*/
    DEFAULT(""),
    /** 红色*/
    RED("31"),
    /** 青蓝色*/
    CYAN("36"),
    /** 紫红色*/
    MULBERRY("35"),
    /** 黄色*/
    YELLOW("33"),
    /** 绿色*/
    GREEN("32"),
    /** 白*/
    WHITE("37"),
    /** 蓝色*/
    BLUE("34"),
    /** 黑色*/
    BLACK("30"),
    /** 橙色(ANSI 256色:208)*/
    ORANGE("38;5;208"),
    /** 金色(ANSI 256色:220)*/
    GOLD("38;5;220"),
    /** 粉色(ANSI 256色:205)*/
    PINK("38;5;205"),
    /** 紫色(ANSI 256色:135)*/
    PURPLE("38;5;135"),
    /** 灰色(ANSI 256色:245)*/
    GRAY("38;5;245"),
    /** 深灰色(ANSI 256色:240)*/
    DARK_GRAY("38;5;240"),
    /** 浅灰色(ANSI 256色:250)*/
    LIGHT_GRAY("38;5;250"),
    /** 天蓝色(ANSI 256色:117)*/
    SKY_BLUE("38;5;117"),
    /** 青柠绿(ANSI 256色:118)*/
    LIME("38;5;118"),
    /** 青绿色(ANSI 256色:30)*/
    TEAL("38;5;30"),
    /** 棕色(ANSI 256色:130)*/
    BROWN("38;5;130");

    /** 颜色核心码(ANSI前景色参数)，DEFAULT为空串表示无着色*/
    private final String colorCore;

    Color(String colorCore) {
        this.colorCore = colorCore;
    }

    /**
     * 获取颜色核心码
     * <p>
     * 可嵌入自定义ANSI序列中，例如：前景色"\033[1;" + core + "m"、
     * 下划线"\033[4;1;" + core + "m"、背景色(反色)"\033[7;" + core + "m"
     *
     * @return 颜色核心码(例如："31"、"38;5;208")，DEFAULT返回空串
     */
    public String getColorCore() {
        return colorCore;
    }

    /**
     * 获取颜色对应的完整ANSI码(加粗前景色)
     * @return ANSI颜色码(例如："\033[1;31m")，DEFAULT返回空串
     */
    public String getAnsiCode() {
        return colorCore.isEmpty() ? "" : "\033[1;" + colorCore + "m";
    }
}
