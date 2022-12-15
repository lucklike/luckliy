package com.luckyframework.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ExceptionUtils {

    /**
     * 得到一个异常类的继承体系
     * ArithmeticException
     * =>
     * ArithmeticException ex RuntimeException ex Exception ex Throwable
     * @param ec 当前异常的Class
     * @return
     */
    public static List<Class<? extends Throwable>> getExceptionFamily(Class<? extends Throwable> ec) {
        List<Class<? extends Throwable>> family = new ArrayList<>();
        family.add(ec);
        if (ec.getSuperclass() == Object.class) {
            return family;
        }
        getExceptionFamily((Class<? extends Throwable>) ec.getSuperclass()).stream().forEach(family::add);
        return family;
    }

    /**
     * 得到关键的异常
     * @param e 程序抛出的异常
     * @return
     */
    public static Throwable getCauseThrowable(Throwable e){
        while (true){
            if(e.getCause() == null){
                return e;
            }
            e=e.getCause();
        }
    }

    /**
     * 判断某个异常是否被包含在一个异常集合中
     * @param exCollection 异常集合
     * @param ex 待判断的异常
     * @return
     */
    public static boolean contained(Collection<Class<? extends Throwable>> exCollection, Class<? extends Throwable> ex){
        for (Class<? extends Throwable> ec : exCollection) {
            if(ex==ec){
                return true;
            }
        }
        return false;
    }
}
