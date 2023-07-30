package com.luckyframework.common;

import com.luckyframework.reflect.ClassUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

/**
 * 控制台，用于在控制台打印彩色日志
 * @author fk7075
 * @version 1.0
 * @date 2020/11/10 9:30
 */
public abstract class Console {

    /**
     * 控制台打印带有颜色的内容
     * @param printContext  打印内容
     * @param color         颜色
     */
    public static void printColor(Object printContext, Color color){
        switch (color){
            case RED        : printRed(printContext);       return;
            case CYAN       : printCyan(printContext);      return;
            case MULBERRY   : printMulberry(printContext);  return;
            case YELLOW     : printYellow(printContext);    return;
            case GREEN      : printGreen(printContext);     return;
            case WHITE      : printWhite(printContext);     return;
            case BLUE       : printBlue(printContext);      return;
            case BLACK      : printBlack(printContext);     return;
            default         : print(printContext);
        }
    }

    /**
     * 控制台换行打印带有颜色的内容
     * @param printContext  打印内容
     * @param color         颜色
     */
    public static void printlnColor(Object printContext, Color color){
        switch (color){
            case RED        : printlnRed(printContext);       return;
            case CYAN       : printlnCyan(printContext);      return;
            case MULBERRY   : printlnMulberry(printContext);  return;
            case YELLOW     : printlnYellow(printContext);    return;
            case GREEN      : printlnGreen(printContext);     return;
            case WHITE      : printlnWhite(printContext);     return;
            case BLUE       : printlnBlue(printContext);      return;
            case BLACK      : printlnBlack(printContext);     return;
            default         : println(printContext);
        }
    }

    /**
     * 获取带有颜色的字符串
     * @param printContext  打印内容
     * @param color         颜色
     * @return 带有颜色的字符串
     */
    public static String getColorString(Object printContext, Color color){
        switch (color){
            case RED        : return getRedString(printContext);
            case CYAN       : return getCyanString(printContext);
            case MULBERRY   : return getMulberryString(printContext);
            case YELLOW     : return getYellowString(printContext);
            case GREEN      : return getGreenString(printContext);
            case WHITE      : return getWhiteString(printContext);
            case BLUE       : return getBlueString(printContext);
            case BLACK      : return getBlackString(printContext);
            default         : return String.valueOf(printContext);
        }
    }

    /**
     * 控制台换行打印
     * @param object 待打印的对象
     */
    public static void println(Object object){
        System.out.println(object);
    }

    /**
     * 控制台换行打印并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void println(String logTemp, Object...args){
        println(StringUtils.format(logTemp, args));
    }

    /**
     * 控制台打印
     * @param object 待打印的对象
     */
    public static void print(Object object){
        System.out.print(object);
    }

    /**
     * 控制台打印
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void print(String logTemp, Object...args){
        print(StringUtils.format(logTemp, args));
    }

    //-------------------------------------------------------------
    //                      red
    //-------------------------------------------------------------

    /**
     * 输出红色日志
     * @param object 待打印的对象
     */
    public static void printRed(Object object){
        print("\033[1;31m"+object+"\033[0m");
    }

    /**
     * 控制台打印红色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printRed(String logTemp, Object ...args){
        printRed(StringUtils.format(logTemp, args));
    }

    /**
     * 控制台换行打印红色日志
     * @param object 待打印的对象
     */
    public static void printlnRed(Object object){
        println("\033[1;31m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印红色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnRed(String logTemp, Object ...args){
        printlnRed(StringUtils.format(logTemp, args));
    }

    /**
     * 返回红色字符
     * @param object 输入对象
     * @return 红色字符串
     */
    public static String getRedString(Object object){
        return "\033[1;31m"+object+"\033[0m";
    }


    //-------------------------------------------------------------
    //                      Cyan
    //-------------------------------------------------------------

    /**
     * 输出青蓝色日志
     * @param object 待打印的对象
     */
    public static void printCyan(Object object){
        print("\033[1;36m"+object+"\033[0m");
    }

    /**
     * 控制台打印青蓝色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printCyan(String logTemp, Object ...args){
        printCyan(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出青蓝色日志
     * @param object 待打印的对象
     */
    public static void printlnCyan(Object object){
        println("\033[1;36m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印青蓝色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnCyan(String logTemp, Object ...args){
        printlnCyan(StringUtils.format(logTemp, args));
    }

    /**
     * 返回青蓝色字符
     * @param object 输入对象
     * @return 蓝青色字符串
     */
    public static String getCyanString(Object object){
        return "\033[1;36m"+object+"\033[0m";
    }


    //-------------------------------------------------------------
    //                      Mulberry
    //-------------------------------------------------------------

    /**
     * 输出紫红色日志
     * @param object 待打印的对象
     */
    public static void printMulberry(Object object){
        print("\033[1;35m"+object+"\033[0m");
    }

    /**
     * 控制台打印紫红色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printMulberry(String logTemp, Object ...args){
        printMulberry(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出紫红色日志
     * @param object 待打印的对象
     */
    public static void printlnMulberry(Object object){
        println("\033[1;35m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印紫红色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnMulberry(String logTemp, Object ...args){
        printlnMulberry(StringUtils.format(logTemp, args));
    }


    /**
     * 返回紫红色字符
     * @param object 输入对象
     * @return 紫红色字符串
     */
    public static String getMulberryString(Object object){
        return "\033[1;35m"+object+"\033[0m";
    }

    //-------------------------------------------------------------
    //                      Yellow
    //-------------------------------------------------------------

    /**
     * 输出黄色日志
     * @param object 待打印的对象
     */
    public static void printYellow(Object object){
        print("\033[1;33m"+object+"\033[0m");
    }

    /**
     * 控制台打印黄色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printYellow(String logTemp, Object ...args){
        printYellow(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出黄色日志
     * @param object 待打印的对象
     */
    public static void printlnYellow(Object object){
        println("\033[1;33m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印黄色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnYellow(String logTemp, Object ...args){
        printlnYellow(StringUtils.format(logTemp, args));
    }

    /**
     * 返回黄色字符
     * @param object 输入对象
     * @return 黄色字符串
     */
    public static String getYellowString(Object object){
        return "\033[1;33m"+object+"\033[0m";
    }

    //-------------------------------------------------------------
    //                      Green
    //-------------------------------------------------------------

    /**
     * 输出绿色日志
     * @param object 待打印的对象
     */
    public static void printGreen(Object object){
        print("\033[1;32m"+object+"\033[0m");
    }

    /**
     * 控制台打印绿色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printGreen(String logTemp, Object ...args){
        printGreen(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出绿色日志
     * @param object 待打印的对象
     */
    public static void printlnGreen(Object object){
        println("\033[1;32m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印绿色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnGreen(String logTemp, Object ...args){
        printlnGreen(StringUtils.format(logTemp, args));
    }

    /**
     * 返回绿色字符
     * @param object 输入对象
     * @return 绿色字符串
     */
    public static String getGreenString(Object object){
        return "\033[1;32m"+object+"\033[0m";
    }


    //-------------------------------------------------------------
    //                      White
    //-------------------------------------------------------------

    /**
     * 输出白色日志
     * @param object 待打印的对象
     */
    public static void printWhite(Object object){
        print("\033[1;37m"+object+"\033[0m");
    }

    /**
     * 控制台打印白色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printWhite(String logTemp, Object ...args){
        printWhite(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出白色日志
     * @param object 待打印的对象
     */
    public static void printlnWhite(Object object){
        println("\033[1;37m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印白色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnWhite(String logTemp, Object ...args){
        printlnWhite(StringUtils.format(logTemp, args));
    }

    /**
     * 返回白色字符
     * @param object 输入对象
     * @return 白色字符串
     */
    public static String getWhiteString(Object object){
        return "\033[1;37m"+object+"\033[0m";
    }

    //-------------------------------------------------------------
    //                      Blue
    //-------------------------------------------------------------

    /**
     * 输出蓝色日志
     * @param object 待打印的对象
     */
    public static void printBlue(Object object){
        print("\033[1;34m"+object+"\033[0m");
    }

    /**
     * 控制台打印蓝色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printBlue(String logTemp, Object ...args){
        printBlue(StringUtils.format(logTemp, args));
    }

    /**
     * 换行输出蓝色日志
     * @param object 待打印的对象
     */
    public static void printlnBlue(Object object){
        println("\033[1;34m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印蓝色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnBlue(String logTemp, Object ...args){
        printlnBlue(StringUtils.format(logTemp, args));
    }


    /**
     * 返回蓝色字符
     * @param object 输入对象
     * @return 蓝色字符串
     */
    public static String getBlueString(Object object){
        return "\033[1;34m"+object+"\033[0m";
    }

    //-------------------------------------------------------------
    //                      Black
    //-------------------------------------------------------------

    /**
     * 输出黑色日志
     * @param object 待打印的对象
     */
    public static void printBlack(Object object){
        print("\033[1;30m"+object+"\033[0m");
    }

    /**
     * 控制台打印黑色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printBlack(String logTemp, Object ...args){
        printBlack(StringUtils.format(logTemp, args));
    }


    /**
     * 换行输出黑色日志
     * @param object 待打印的对象
     */
    public static void printlnBlack(Object object){
        println("\033[1;30m"+object+"\033[0m");
    }

    /**
     * 控制台换行打印黑色日志并格式化输出
     * 例如：
     * <p>
     * Console.println("Hello {}, I'm {}", "Jack", "Lucy")<br/>
     * ==>  Hello Jack, I'm Lucy<br/>
     * Console.println("Hello {1}, I'm {0}", "Jack", "Lucy")<br/>
     * ==>  Hello Lucy, I'm Jack<br/>
     *
     * @param logTemp 日志格式
     * @param args  占位符参数
     */
    public static void printlnBlack(String logTemp, Object ...args){
        printlnBlack(StringUtils.format(logTemp, args));
    }

    /**
     * 返回黑色字符
     * @param object 输入对象
     * @return 黑色字符串
     */
    public static String getBlackString(Object object){
        return "\033[1;30m"+object+"\033[0m";
    }

    public static String nextLine(){
        return getScanner().nextLine();
    }

    private static Scanner scanner;

    private static Scanner getScanner(){
        if(scanner == null){
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    /**
     * 将输入的对象以表格的形式输出
     * 1.如果输入是java基本类型/基本类型数组/基本类型集合则会直接打印
     *
     * @param obj 带打印的对象
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
                table.createDateByCollection((Collection<?>) obj);
            } else if (obj instanceof Map){
                table.createDataByMap((Map<?, ?>) obj);
            } else{
                table.createData(obj);
            }
            println(table.format());
        }

    }


    public static void main(String[] args) {

        //-------------------------------//
        print("\n-----------get{}----------\n\n", "颜色字符串");
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
        println("\n-----------get颜色字符串----------\n");
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
        println("\n------------print颜色()-----------\n");
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
        println("\n------------println颜色()-----------\n");
        //-------------------------------//

        printlnRed("----------{}--------", "红  色");
        printlnCyan("---------{}---------","青蓝色");
        printlnMulberry("---------{}---------","紫红色");
        printlnBlack("---------{}---------", "黑  色");
        printlnBlue("---------{}---------", "蓝  色");
        printlnWhite("---------{}---------", "白  色");
        printlnYellow("---------{}---------","黄  色");
        printlnGreen("---------{}---------", "绿  色");

        printlnColor("你好呀", Color.MULBERRY);

    }
}
