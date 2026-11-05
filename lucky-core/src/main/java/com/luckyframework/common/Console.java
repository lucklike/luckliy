package com.luckyframework.common;

import com.luckyframework.reflect.ClassUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

/**
 * 控制台输出工具类，封装标准输出上的文本打印与彩色着色能力。
 *
 * <p>着色基于ANSI转义序列，提供两套使用方式：
 * <ul>
 *     <li><b>基础色快捷方法</b>：每种基础色都提供 printXxx(不换行)、printlnXxx(换行)、
 *     getXxxString(仅返回着色文本)三组方法，以及对应的格式化重载，
 *     例如 {@link #printRed(Object)}、{@link #printlnGreen(String, Object...)}、{@link #getBlueString(Object)}</li>
 *     <li><b>命名色通用方法</b>：{@link #printColor(Object, Color)}、{@link #printlnColor(Object, Color)}、
 *     {@link #getColorString(Object, Color)} 配合 {@link Color} 枚举使用，可访问全部颜色(含256色扩展命名色)</li>
 * </ul>
 *
 * <p>格式化占位符规则与 {@link StringUtils#format(String, Object...)} 一致：
 * <pre>
 * Console.println("Hello {}, I'm {}", "Jack", "Lucy");    // Hello Jack, I'm Lucy
 * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy"); // Hello Lucy, I'm Jack
 * </pre>
 *
 * <p>使用注意：
 * <ul>
 *     <li>基础色使用ANSI 4bit颜色码，扩展命名色使用ANSI 256色码，终端不支持时扩展色会被忽略或降级显示</li>
 *     <li>{@link Color#DEFAULT} 表示不着色，此时文本原样输出</li>
 *     <li>着色文本仅在支持ANSI转义的终端中生效，输出重定向到文件时会保留原始转义字符</li>
 * </ul>
 *
 * <p>本类仅提供静态方法，不允许被实例化。
 *
 * @author fk7075
 * @version 1.0
 * @date 2020/11/10 9:30
 */
public abstract class Console {

    /** 颜色重置的ANSI码，用于结束一次着色*/
    private static final String RESET = "\033[0m";

    //-----------------------------------------------------------------------------------------
    //                                      通用输出
    //-----------------------------------------------------------------------------------------

    /**
     * 在控制台打印文本(不换行)
     * @param object 待打印的对象，为 null 时输出 "null"
     */
    public static void print(Object object){
        System.out.print(object);
    }

    /**
     * 在控制台打印格式化文本(不换行)
     * <p>
     * Console.print("Hello {}, I'm {}", "Jack", "Lucy")  →  Hello Jack, I'm Lucy
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void print(String logTemp, Object...args){
        print(StringUtils.format(logTemp, args));
    }

    /**
     * 在控制台换行打印文本
     * @param object 待打印的对象，为 null 时输出 "null"
     */
    public static void println(Object object){
        System.out.println(object);
    }

    /**
     * 在控制台换行打印格式化文本
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")  →  Hello Jack, I'm Lucy
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void println(String logTemp, Object...args){
        println(StringUtils.format(logTemp, args));
    }

    //-----------------------------------------------------------------------------------------
    //                                      通用着色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印指定颜色的文本(不换行)
     * @param printContext 待打印的内容
     * @param color        颜色，{@link Color#DEFAULT} 表示不着色
     */
    public static void printColor(Object printContext, Color color){
        print(getColorString(printContext, color));
    }

    /**
     * 换行打印指定颜色的文本
     * @param printContext 待打印的内容
     * @param color        颜色，{@link Color#DEFAULT} 表示不着色
     */
    public static void printlnColor(Object printContext, Color color){
        println(getColorString(printContext, color));
    }

    /**
     * 打印指定颜色的格式化文本(不换行)
     * <p>
     * Console.printColor("Hello {}", Color.ORANGE, "Jack")  →  Hello Jack(橙色，不换行)
     *
     * @param logTemp 日志格式
     * @param color   颜色
     * @param args    占位符参数
     */
    public static void printColor(String logTemp, Color color, Object...args){
        printColor(StringUtils.format(logTemp, args), color);
    }

    /**
     * 换行打印指定颜色的格式化文本
     * <p>
     * Console.printlnColor("Hello {}", Color.ORANGE, "Jack")  →  Hello Jack(橙色，换行)
     *
     * @param logTemp 日志格式
     * @param color   颜色
     * @param args    占位符参数
     */
    public static void printlnColor(String logTemp, Color color, Object...args){
        printlnColor(StringUtils.format(logTemp, args), color);
    }

    /**
     * 获取带有指定颜色的文本
     * @param printContext 待着色内容
     * @param color        颜色，{@link Color#DEFAULT} 表示不着色
     * @return 带有ANSI着色码的文本；颜色为 {@link Color#DEFAULT} 时原样返回
     */
    public static String getColorString(Object printContext, Color color){
        String ansiCode = color.getAnsiCode();
        return ansiCode.isEmpty() ? String.valueOf(printContext) : ansiCode + printContext + RESET;
    }

    //-----------------------------------------------------------------------------------------
    //                                        红  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印红色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printRed(Object object){
        printColor(object, Color.RED);
    }

    /**
     * 打印红色格式化文本(不换行)
     * <p>
     * Console.printRed("Hello {}", "Jack")  →  Hello Jack(红色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printRed(String logTemp, Object ...args){
        printRed(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印红色文本
     * @param object 待打印的对象
     */
    public static void printlnRed(Object object){
        printlnColor(object, Color.RED);
    }

    /**
     * 换行打印红色格式化文本
     * <p>
     * Console.printlnRed("Hello {}", "Jack")  →  Hello Jack(红色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnRed(String logTemp, Object ...args){
        printlnRed(StringUtils.format(logTemp, args));
    }

    /**
     * 获取红色文本
     * @param object 待着色对象
     * @return 带有红色ANSI着色码的文本
     */
    public static String getRedString(Object object){
        return getColorString(object, Color.RED);
    }

    //-----------------------------------------------------------------------------------------
    //                                        青蓝色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印青蓝色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printCyan(Object object){
        printColor(object, Color.CYAN);
    }

    /**
     * 打印青蓝色格式化文本(不换行)
     * <p>
     * Console.printCyan("Hello {}", "Jack")  →  Hello Jack(青蓝色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printCyan(String logTemp, Object ...args){
        printCyan(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印青蓝色文本
     * @param object 待打印的对象
     */
    public static void printlnCyan(Object object){
        printlnColor(object, Color.CYAN);
    }

    /**
     * 换行打印青蓝色格式化文本
     * <p>
     * Console.printlnCyan("Hello {}", "Jack")  →  Hello Jack(青蓝色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnCyan(String logTemp, Object ...args){
        printlnCyan(StringUtils.format(logTemp, args));
    }

    /**
     * 获取青蓝色文本
     * @param object 待着色对象
     * @return 带有青蓝色ANSI着色码的文本
     */
    public static String getCyanString(Object object){
        return getColorString(object, Color.CYAN);
    }

    //-----------------------------------------------------------------------------------------
    //                                        紫红色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印紫红色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printMulberry(Object object){
        printColor(object, Color.MULBERRY);
    }

    /**
     * 打印紫红色格式化文本(不换行)
     * <p>
     * Console.printMulberry("Hello {}", "Jack")  →  Hello Jack(紫红色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printMulberry(String logTemp, Object ...args){
        printMulberry(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印紫红色文本
     * @param object 待打印的对象
     */
    public static void printlnMulberry(Object object){
        printlnColor(object, Color.MULBERRY);
    }

    /**
     * 换行打印紫红色格式化文本
     * <p>
     * Console.printlnMulberry("Hello {}", "Jack")  →  Hello Jack(紫红色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnMulberry(String logTemp, Object ...args){
        printlnMulberry(StringUtils.format(logTemp, args));
    }

    /**
     * 获取紫红色文本
     * @param object 待着色对象
     * @return 带有紫红色ANSI着色码的文本
     */
    public static String getMulberryString(Object object){
        return getColorString(object, Color.MULBERRY);
    }

    //-----------------------------------------------------------------------------------------
    //                                        黄  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印黄色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printYellow(Object object){
        printColor(object, Color.YELLOW);
    }

    /**
     * 打印黄色格式化文本(不换行)
     * <p>
     * Console.printYellow("Hello {}", "Jack")  →  Hello Jack(黄色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printYellow(String logTemp, Object ...args){
        printYellow(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印黄色文本
     * @param object 待打印的对象
     */
    public static void printlnYellow(Object object){
        printlnColor(object, Color.YELLOW);
    }

    /**
     * 换行打印黄色格式化文本
     * <p>
     * Console.printlnYellow("Hello {}", "Jack")  →  Hello Jack(黄色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnYellow(String logTemp, Object ...args){
        printlnYellow(StringUtils.format(logTemp, args));
    }

    /**
     * 获取黄色文本
     * @param object 待着色对象
     * @return 带有黄色ANSI着色码的文本
     */
    public static String getYellowString(Object object){
        return getColorString(object, Color.YELLOW);
    }

    //-----------------------------------------------------------------------------------------
    //                                        绿  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印绿色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printGreen(Object object){
        printColor(object, Color.GREEN);
    }

    /**
     * 打印绿色格式化文本(不换行)
     * <p>
     * Console.printGreen("Hello {}", "Jack")  →  Hello Jack(绿色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printGreen(String logTemp, Object ...args){
        printGreen(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印绿色文本
     * @param object 待打印的对象
     */
    public static void printlnGreen(Object object){
        printlnColor(object, Color.GREEN);
    }

    /**
     * 换行打印绿色格式化文本
     * <p>
     * Console.printlnGreen("Hello {}", "Jack")  →  Hello Jack(绿色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnGreen(String logTemp, Object ...args){
        printlnGreen(StringUtils.format(logTemp, args));
    }

    /**
     * 获取绿色文本
     * @param object 待着色对象
     * @return 带有绿色ANSI着色码的文本
     */
    public static String getGreenString(Object object){
        return getColorString(object, Color.GREEN);
    }

    //-----------------------------------------------------------------------------------------
    //                                        白  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印白色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printWhite(Object object){
        printColor(object, Color.WHITE);
    }

    /**
     * 打印白色格式化文本(不换行)
     * <p>
     * Console.printWhite("Hello {}", "Jack")  →  Hello Jack(白色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printWhite(String logTemp, Object ...args){
        printWhite(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印白色文本
     * @param object 待打印的对象
     */
    public static void printlnWhite(Object object){
        printlnColor(object, Color.WHITE);
    }

    /**
     * 换行打印白色格式化文本
     * <p>
     * Console.printlnWhite("Hello {}", "Jack")  →  Hello Jack(白色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnWhite(String logTemp, Object ...args){
        printlnWhite(StringUtils.format(logTemp, args));
    }

    /**
     * 获取白色文本
     * @param object 待着色对象
     * @return 带有白色ANSI着色码的文本
     */
    public static String getWhiteString(Object object){
        return getColorString(object, Color.WHITE);
    }

    //-----------------------------------------------------------------------------------------
    //                                        蓝  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印蓝色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printBlue(Object object){
        printColor(object, Color.BLUE);
    }

    /**
     * 打印蓝色格式化文本(不换行)
     * <p>
     * Console.printBlue("Hello {}", "Jack")  →  Hello Jack(蓝色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printBlue(String logTemp, Object ...args){
        printBlue(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印蓝色文本
     * @param object 待打印的对象
     */
    public static void printlnBlue(Object object){
        printlnColor(object, Color.BLUE);
    }

    /**
     * 换行打印蓝色格式化文本
     * <p>
     * Console.printlnBlue("Hello {}", "Jack")  →  Hello Jack(蓝色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnBlue(String logTemp, Object ...args){
        printlnBlue(StringUtils.format(logTemp, args));
    }

    /**
     * 获取蓝色文本
     * @param object 待着色对象
     * @return 带有蓝色ANSI着色码的文本
     */
    public static String getBlueString(Object object){
        return getColorString(object, Color.BLUE);
    }

    //-----------------------------------------------------------------------------------------
    //                                        黑  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印黑色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printBlack(Object object){
        printColor(object, Color.BLACK);
    }

    /**
     * 打印黑色格式化文本(不换行)
     * <p>
     * Console.printBlack("Hello {}", "Jack")  →  Hello Jack(黑色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printBlack(String logTemp, Object ...args){
        printBlack(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印黑色文本
     * @param object 待打印的对象
     */
    public static void printlnBlack(Object object){
        printlnColor(object, Color.BLACK);
    }

    /**
     * 换行打印黑色格式化文本
     * <p>
     * Console.printlnBlack("Hello {}", "Jack")  →  Hello Jack(黑色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnBlack(String logTemp, Object ...args){
        printlnBlack(StringUtils.format(logTemp, args));
    }

    /**
     * 获取黑色文本
     * @param object 待着色对象
     * @return 带有黑色ANSI着色码的文本
     */
    public static String getBlackString(Object object){
        return getColorString(object, Color.BLACK);
    }

    //-----------------------------------------------------------------------------------------
    //                                        橙  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印橙色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printOrange(Object object){
        printColor(object, Color.ORANGE);
    }

    /**
     * 打印橙色格式化文本(不换行)
     * <p>
     * Console.printOrange("Hello {}", "Jack")  →  Hello Jack(橙色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printOrange(String logTemp, Object ...args){
        printOrange(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印橙色文本
     * @param object 待打印的对象
     */
    public static void printlnOrange(Object object){
        printlnColor(object, Color.ORANGE);
    }

    /**
     * 换行打印橙色格式化文本
     * <p>
     * Console.printlnOrange("Hello {}", "Jack")  →  Hello Jack(橙色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnOrange(String logTemp, Object ...args){
        printlnOrange(StringUtils.format(logTemp, args));
    }

    /**
     * 获取橙色文本
     * @param object 待着色对象
     * @return 带有橙色ANSI着色码的文本
     */
    public static String getOrangeString(Object object){
        return getColorString(object, Color.ORANGE);
    }

    //-----------------------------------------------------------------------------------------
    //                                        金  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印金色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printGold(Object object){
        printColor(object, Color.GOLD);
    }

    /**
     * 打印金色格式化文本(不换行)
     * <p>
     * Console.printGold("Hello {}", "Jack")  →  Hello Jack(金色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printGold(String logTemp, Object ...args){
        printGold(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印金色文本
     * @param object 待打印的对象
     */
    public static void printlnGold(Object object){
        printlnColor(object, Color.GOLD);
    }

    /**
     * 换行打印金色格式化文本
     * <p>
     * Console.printlnGold("Hello {}", "Jack")  →  Hello Jack(金色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnGold(String logTemp, Object ...args){
        printlnGold(StringUtils.format(logTemp, args));
    }

    /**
     * 获取金色文本
     * @param object 待着色对象
     * @return 带有金色ANSI着色码的文本
     */
    public static String getGoldString(Object object){
        return getColorString(object, Color.GOLD);
    }

    //-----------------------------------------------------------------------------------------
    //                                        粉  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印粉色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printPink(Object object){
        printColor(object, Color.PINK);
    }

    /**
     * 打印粉色格式化文本(不换行)
     * <p>
     * Console.printPink("Hello {}", "Jack")  →  Hello Jack(粉色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printPink(String logTemp, Object ...args){
        printPink(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印粉色文本
     * @param object 待打印的对象
     */
    public static void printlnPink(Object object){
        printlnColor(object, Color.PINK);
    }

    /**
     * 换行打印粉色格式化文本
     * <p>
     * Console.printlnPink("Hello {}", "Jack")  →  Hello Jack(粉色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnPink(String logTemp, Object ...args){
        printlnPink(StringUtils.format(logTemp, args));
    }

    /**
     * 获取粉色文本
     * @param object 待着色对象
     * @return 带有粉色ANSI着色码的文本
     */
    public static String getPinkString(Object object){
        return getColorString(object, Color.PINK);
    }

    //-----------------------------------------------------------------------------------------
    //                                        紫  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印紫色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printPurple(Object object){
        printColor(object, Color.PURPLE);
    }

    /**
     * 打印紫色格式化文本(不换行)
     * <p>
     * Console.printPurple("Hello {}", "Jack")  →  Hello Jack(紫色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printPurple(String logTemp, Object ...args){
        printPurple(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印紫色文本
     * @param object 待打印的对象
     */
    public static void printlnPurple(Object object){
        printlnColor(object, Color.PURPLE);
    }

    /**
     * 换行打印紫色格式化文本
     * <p>
     * Console.printlnPurple("Hello {}", "Jack")  →  Hello Jack(紫色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnPurple(String logTemp, Object ...args){
        printlnPurple(StringUtils.format(logTemp, args));
    }

    /**
     * 获取紫色文本
     * @param object 待着色对象
     * @return 带有紫色ANSI着色码的文本
     */
    public static String getPurpleString(Object object){
        return getColorString(object, Color.PURPLE);
    }

    //-----------------------------------------------------------------------------------------
    //                                        灰  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印灰色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printGray(Object object){
        printColor(object, Color.GRAY);
    }

    /**
     * 打印灰色格式化文本(不换行)
     * <p>
     * Console.printGray("Hello {}", "Jack")  →  Hello Jack(灰色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printGray(String logTemp, Object ...args){
        printGray(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印灰色文本
     * @param object 待打印的对象
     */
    public static void printlnGray(Object object){
        printlnColor(object, Color.GRAY);
    }

    /**
     * 换行打印灰色格式化文本
     * <p>
     * Console.printlnGray("Hello {}", "Jack")  →  Hello Jack(灰色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnGray(String logTemp, Object ...args){
        printlnGray(StringUtils.format(logTemp, args));
    }

    /**
     * 获取灰色文本
     * @param object 待着色对象
     * @return 带有灰色ANSI着色码的文本
     */
    public static String getGrayString(Object object){
        return getColorString(object, Color.GRAY);
    }

    //-----------------------------------------------------------------------------------------
    //                                        深  灰
    //-----------------------------------------------------------------------------------------

    /**
     * 打印深灰色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printDarkGray(Object object){
        printColor(object, Color.DARK_GRAY);
    }

    /**
     * 打印深灰色格式化文本(不换行)
     * <p>
     * Console.printDarkGray("Hello {}", "Jack")  →  Hello Jack(深灰色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printDarkGray(String logTemp, Object ...args){
        printDarkGray(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印深灰色文本
     * @param object 待打印的对象
     */
    public static void printlnDarkGray(Object object){
        printlnColor(object, Color.DARK_GRAY);
    }

    /**
     * 换行打印深灰色格式化文本
     * <p>
     * Console.printlnDarkGray("Hello {}", "Jack")  →  Hello Jack(深灰色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnDarkGray(String logTemp, Object ...args){
        printlnDarkGray(StringUtils.format(logTemp, args));
    }

    /**
     * 获取深灰色文本
     * @param object 待着色对象
     * @return 带有深灰色ANSI着色码的文本
     */
    public static String getDarkGrayString(Object object){
        return getColorString(object, Color.DARK_GRAY);
    }

    //-----------------------------------------------------------------------------------------
    //                                        浅  灰
    //-----------------------------------------------------------------------------------------

    /**
     * 打印浅灰色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printLightGray(Object object){
        printColor(object, Color.LIGHT_GRAY);
    }

    /**
     * 打印浅灰色格式化文本(不换行)
     * <p>
     * Console.printLightGray("Hello {}", "Jack")  →  Hello Jack(浅灰色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printLightGray(String logTemp, Object ...args){
        printLightGray(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印浅灰色文本
     * @param object 待打印的对象
     */
    public static void printlnLightGray(Object object){
        printlnColor(object, Color.LIGHT_GRAY);
    }

    /**
     * 换行打印浅灰色格式化文本
     * <p>
     * Console.printlnLightGray("Hello {}", "Jack")  →  Hello Jack(浅灰色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnLightGray(String logTemp, Object ...args){
        printlnLightGray(StringUtils.format(logTemp, args));
    }

    /**
     * 获取浅灰色文本
     * @param object 待着色对象
     * @return 带有浅灰色ANSI着色码的文本
     */
    public static String getLightGrayString(Object object){
        return getColorString(object, Color.LIGHT_GRAY);
    }

    //-----------------------------------------------------------------------------------------
    //                                        天  蓝
    //-----------------------------------------------------------------------------------------

    /**
     * 打印天蓝色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printSkyBlue(Object object){
        printColor(object, Color.SKY_BLUE);
    }

    /**
     * 打印天蓝色格式化文本(不换行)
     * <p>
     * Console.printSkyBlue("Hello {}", "Jack")  →  Hello Jack(天蓝色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printSkyBlue(String logTemp, Object ...args){
        printSkyBlue(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印天蓝色文本
     * @param object 待打印的对象
     */
    public static void printlnSkyBlue(Object object){
        printlnColor(object, Color.SKY_BLUE);
    }

    /**
     * 换行打印天蓝色格式化文本
     * <p>
     * Console.printlnSkyBlue("Hello {}", "Jack")  →  Hello Jack(天蓝色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnSkyBlue(String logTemp, Object ...args){
        printlnSkyBlue(StringUtils.format(logTemp, args));
    }

    /**
     * 获取天蓝色文本
     * @param object 待着色对象
     * @return 带有天蓝色ANSI着色码的文本
     */
    public static String getSkyBlueString(Object object){
        return getColorString(object, Color.SKY_BLUE);
    }

    //-----------------------------------------------------------------------------------------
    //                                        青  柠
    //-----------------------------------------------------------------------------------------

    /**
     * 打印青柠绿色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printLime(Object object){
        printColor(object, Color.LIME);
    }

    /**
     * 打印青柠绿色格式化文本(不换行)
     * <p>
     * Console.printLime("Hello {}", "Jack")  →  Hello Jack(青柠绿色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printLime(String logTemp, Object ...args){
        printLime(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印青柠绿色文本
     * @param object 待打印的对象
     */
    public static void printlnLime(Object object){
        printlnColor(object, Color.LIME);
    }

    /**
     * 换行打印青柠绿色格式化文本
     * <p>
     * Console.printlnLime("Hello {}", "Jack")  →  Hello Jack(青柠绿色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnLime(String logTemp, Object ...args){
        printlnLime(StringUtils.format(logTemp, args));
    }

    /**
     * 获取青柠绿色文本
     * @param object 待着色对象
     * @return 带有青柠绿色ANSI着色码的文本
     */
    public static String getLimeString(Object object){
        return getColorString(object, Color.LIME);
    }

    //-----------------------------------------------------------------------------------------
    //                                        青  绿
    //-----------------------------------------------------------------------------------------

    /**
     * 打印青绿色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printTeal(Object object){
        printColor(object, Color.TEAL);
    }

    /**
     * 打印青绿色格式化文本(不换行)
     * <p>
     * Console.printTeal("Hello {}", "Jack")  →  Hello Jack(青绿色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printTeal(String logTemp, Object ...args){
        printTeal(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印青绿色文本
     * @param object 待打印的对象
     */
    public static void printlnTeal(Object object){
        printlnColor(object, Color.TEAL);
    }

    /**
     * 换行打印青绿色格式化文本
     * <p>
     * Console.printlnTeal("Hello {}", "Jack")  →  Hello Jack(青绿色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnTeal(String logTemp, Object ...args){
        printlnTeal(StringUtils.format(logTemp, args));
    }

    /**
     * 获取青绿色文本
     * @param object 待着色对象
     * @return 带有青绿色ANSI着色码的文本
     */
    public static String getTealString(Object object){
        return getColorString(object, Color.TEAL);
    }

    //-----------------------------------------------------------------------------------------
    //                                        棕  色
    //-----------------------------------------------------------------------------------------

    /**
     * 打印棕色文本(不换行)
     * @param object 待打印的对象
     */
    public static void printBrown(Object object){
        printColor(object, Color.BROWN);
    }

    /**
     * 打印棕色格式化文本(不换行)
     * <p>
     * Console.printBrown("Hello {}", "Jack")  →  Hello Jack(棕色，不换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printBrown(String logTemp, Object ...args){
        printBrown(StringUtils.format(logTemp, args));
    }

    /**
     * 换行打印棕色文本
     * @param object 待打印的对象
     */
    public static void printlnBrown(Object object){
        printlnColor(object, Color.BROWN);
    }

    /**
     * 换行打印棕色格式化文本
     * <p>
     * Console.printlnBrown("Hello {}", "Jack")  →  Hello Jack(棕色，换行)
     *
     * @param logTemp 日志格式
     * @param args    占位符参数
     */
    public static void printlnBrown(String logTemp, Object ...args){
        printlnBrown(StringUtils.format(logTemp, args));
    }

    /**
     * 获取棕色文本
     * @param object 待着色对象
     * @return 带有棕色ANSI着色码的文本
     */
    public static String getBrownString(Object object){
        return getColorString(object, Color.BROWN);
    }

    //-----------------------------------------------------------------------------------------
    //                                      控制台交互
    //-----------------------------------------------------------------------------------------

    /**
     * 读取控制台输入的一行文本(阻塞等待用户输入)
     * @return 用户输入的一行文本
     */
    public static String nextLine(){
        return getScanner().nextLine();
    }

    /** 标准输入扫描器，首次使用时延迟初始化*/
    private static Scanner scanner;

    /**
     * 获取标准输入扫描器(延迟初始化，全局复用)
     * @return 标准输入扫描器
     */
    private static Scanner getScanner(){
        if(scanner == null){
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    //-----------------------------------------------------------------------------------------
    //                                      表格输出
    //-----------------------------------------------------------------------------------------

    /**
     * 将对象以表格的形式输出
     * <p>
     * 输出方式按对象类型自动选择：
     * <ul>
     *     <li>对象为 null：直接输出 "null"</li>
     *     <li>简单基础类型：直接输出其字符串形式</li>
     *     <li>数组：按数组元素构建表格</li>
     *     <li>Collection：按集合元素构建表格</li>
     *     <li>Map：按键值对构建表格</li>
     *     <li>普通对象：按对象属性构建表格</li>
     * </ul>
     *
     * @param obj 待打印的对象
     */
    public static void printTable(Object obj){
        if(obj == null){
            println("null");
        }else if(ClassUtils.isSimpleBaseType(obj.getClass())){
            println(String.valueOf(obj));
        }else{
            Table table = new Table();
            if(obj.getClass().isArray()){
                table.createDataByArray((Object[]) obj);
            } else if(obj instanceof Collection){
                table.createDataByCollection((Collection<?>) obj);
            } else if (obj instanceof Map){
                table.createDataByMap((Map<?, ?>) obj);
            } else{
                table.createData(obj);
            }
            println(table.format());
        }
    }

    //-----------------------------------------------------------------------------------------
    //                                      颜色效果演示
    //-----------------------------------------------------------------------------------------

    /**
     * 控制台颜色效果演示入口
     * @param args 启动参数(未使用)
     */
    public static void main(String[] args) {

        //-------------------------------//
        println("\n-----------基础色：printXxx(不换行)-----------\n");
        //-------------------------------//
        printRed("---------红  色---------\n");
        printCyan("---------青蓝色---------\n");
        printMulberry("---------紫红色---------\n");
        printBlack("---------黑  色---------\n");
        printBlue("---------蓝  色---------\n");
        printWhite("---------白  色---------\n");
        printYellow("---------黄  色---------\n");
        printGreen("---------绿  色---------\n");

        //-------------------------------//
        println("\n-----------基础色：getXxxString----------\n");
        //-------------------------------//
        println("---------"+ getRedString("红  色")+"---------");
        println("---------"+ getCyanString("青蓝色")+"---------");
        println("---------"+ getMulberryString("紫红色")+"---------");
        println("---------"+ getBlackString("黑  色")+"---------");
        println("---------"+ getBlueString("蓝  色")+"---------");
        println("---------"+ getWhiteString("白  色")+"---------");
        println("---------"+ getYellowString("黄  色")+"---------");
        println("---------"+ getGreenString("绿  色")+"---------");

        //-------------------------------//
        println("\n------------基础色：printXxx格式化-----------\n");
        //-------------------------------//
        printRed("----------{}--------\n", "红  色");
        printCyan("---------{}---------\n","青蓝色");
        printMulberry("---------{}---------\n","紫红色");
        printBlack("---------{}---------\n", "黑  色");
        printBlue("---------{}---------\n", "蓝  色");
        printWhite("---------{}---------\n", "白  色");
        printYellow("---------{}---------\n","黄  色");
        printGreen("---------{}---------\n", "绿  色");

        //-------------------------------//
        println("\n------------基础色：printlnXxx-----------\n");
        //-------------------------------//
        printlnRed("----------{}--------", "红  色");
        printlnCyan("---------{}---------","青蓝色");
        printlnMulberry("---------{}---------","紫红色");
        printlnBlack("---------{}---------", "黑  色");
        printlnBlue("---------{}---------", "蓝  色");
        printlnWhite("---------{}---------", "白  色");
        printlnYellow("---------{}---------","黄  色");
        printlnGreen("---------{}---------", "绿  色");

        //-------------------------------//
        println("\n-----------扩展命名色：printlnXxx-----------\n");
        //-------------------------------//
        printlnOrange("---------{}---------", "橙  色");
        printlnGold("---------{}---------", "金  色");
        printlnPink("---------{}---------", "粉  色");
        printlnPurple("---------{}---------", "紫  色");
        printlnGray("---------{}---------", "灰  色");
        printlnDarkGray("---------{}---------", "深  灰");
        printlnLightGray("---------{}---------", "浅  灰");
        printlnSkyBlue("---------{}---------", "天  蓝");
        printlnLime("---------{}---------", "青  柠");
        printlnTeal("---------{}---------", "青  绿");
        printlnBrown("---------{}---------", "棕  色");

        //-------------------------------//
        println("\n-----------通用着色：Color枚举-----------\n");
        //-------------------------------//
        printlnColor("---------{}---------", Color.DEFAULT, "默认色");
        printlnColor("---------{}---------", Color.ORANGE, "橙  色");

        //-------------------------------//
        println("\n-----------Color#getColorCore自定义ANSI序列-----------\n");
        //-------------------------------//
        println("\033[1;" + Color.RED.getColorCore() + "mHELLO WORLD\033[0m");
        println("\033[4;1;" + Color.RED.getColorCore() + "mHELLO WORLD\033[0m");
        println("\033[7;1;" + Color.RED.getColorCore() + "mHELLO WORLD\033[0m");
    }
}
