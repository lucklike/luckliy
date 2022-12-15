package com.luckyframework.common;

/**
 * 公共工具类
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 12:04
 */
public abstract class CommonUtils {

    /**
     * 如果满足条件则执行给定的逻辑，否则不执行
     * @param b         是否满足条件
     * @param runnable  给定的逻辑
     */
    public static void trueIsRunning(boolean b, Runnable runnable){
        if(b){
            runnable.run();
        }
    }

    /**
     * 如果主流程执行出现异常则执行异常流程
     * @param mainRunnable              主流程
     * @param exceptionAfterRunnable    异常流程
     */
    public static void exceptionAfterRunning(Runnable mainRunnable, Runnable exceptionAfterRunnable){
        try {
            mainRunnable.run();
        }catch (Throwable e){
            exceptionAfterRunnable.run();;
        }
    }
}
