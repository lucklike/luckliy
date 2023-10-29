package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 条件选择器中的分支定义
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 02:22
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Branch {

    /**
     * <pre>
     *     断言SpEL表达式, <b>SpEL表达式部分需要写在#{}中</b>
     *     返回值必须是boolean类型
     * </pre>
     */
    String assertion();

    /**
     * 结果表达式，这里允许使用SpEL表达式来生成一个默认值，<b>SpEL表达式部分需要写在#{}中</b>
     */
    String result();


}
