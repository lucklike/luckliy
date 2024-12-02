package com.luckyframework.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class ExceptionUtils {

    /**
     * 得到一个异常类的继承体系
     * ArithmeticException
     * =>
     * ArithmeticException ex RuntimeException ex Exception ex Throwable
     *
     * @param ec 当前异常的Class
     * @return 异常类的继承体系
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends Throwable>> getExceptionFamily(Class<? extends Throwable> ec) {
        List<Class<? extends Throwable>> family = new ArrayList<>();
        family.add(ec);
        if (ec.getSuperclass() == Object.class) {
            return family;
        }
        family.addAll(getExceptionFamily((Class<? extends Throwable>) ec.getSuperclass()));
        return family;
    }

    public static Throwable getCauseThrowable(Throwable e, Class<? extends Throwable> ec) {
        Throwable temp = e;
        while (true) {
            if (temp.getClass() == ec) {
                return temp;
            }
            if (temp.getCause() == null) {
                return e;
            }
            temp = temp.getCause();
        }
    }

    /**
     * 得到关键的异常
     *
     * @param e 程序抛出的异常
     * @return 关键异常
     */
    public static Throwable getCauseThrowable(Throwable e) {
        while (true) {
            if (e.getCause() == null) {
                return e;
            }
            e = e.getCause();
        }
    }

    @SafeVarargs
    public static Throwable getCauseThrowable(Throwable throwable, Class<? extends Throwable>... excludes) {
        if (throwable == null) {
            return null;
        }
        Throwable cause = throwable;
        while (contained(Arrays.asList(excludes), cause.getClass())) {
            if (cause.getCause() == null) {
                return cause;
            }
            cause = cause.getCause();
        }
        return cause;
    }



    /**
     * 判断某个异常是否被包含在一个异常集合中
     *
     * @param throwableCollection 异常集合
     * @param throwableClass      待判断的异常
     * @return 某个异常是否被包含在一个异常集合中
     */
    public static boolean contained(Collection<Class<? extends Throwable>> throwableCollection, Class<? extends Throwable> throwableClass) {
        for (Class<? extends Throwable> elementThrowableClass : throwableCollection) {
            if (throwableClass == elementThrowableClass) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAssignableFrom(Collection<Class<? extends Throwable>> throwableCollection, Class<? extends Throwable> throwableClass) {
        for (Class<? extends Throwable> elementThrowableClass : throwableCollection) {
            if (elementThrowableClass.isAssignableFrom(throwableClass)) {
                return true;
            }
        }
        return false;
    }
}
