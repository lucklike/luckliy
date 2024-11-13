package com.luckyframework.httpclient.proxy.spel;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpEL表达式，变量声明
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 15:49
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(SpELImports.class)
public @interface SpELImport {

    /**
     * 导入一组Class，Class中的静态方法和静态变量会被加入到SpEL环境变量中
     */
    @AliasFor("classes")
    Class<?>[] value() default {};

    /**
     * 声明一个Root变量
     */
    String[] root() default {};

    /**
     * 声明一个普通变量
     */
    String[] var() default {};

    /**
     * 声明一个Root字面量，不会进行SpEL解析
     */
    String[] rootLit() default {};

    /**
     * 声明一个普通字面量，不会进行SpEL解析
     */
    String[] varLit() default {};

    /**
     * 导入一组Class，Class中的静态方法和静态变量会被加入到SpEL环境变量中
     */
    Class<?>[] classes() default {};

    /**
     * 导入一组依赖包
     * <pre>
     *     例如:
     *     导包前：
     *     创建一个ArrayList集合的SpEL表达式为：
     *      <b>#{new java.util.ArrayList()}</b>
     *
     *     当此处导入java.util包后可以简写为：
     *      <b>#{new ArrayList()}</b>
     * </pre>
     */
    String[] pack() default {};

    /**
     * 属性名与属性值之间的分隔符
     */
    String separator() default "=";


}
