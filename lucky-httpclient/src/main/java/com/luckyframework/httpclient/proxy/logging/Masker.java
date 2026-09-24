package com.luckyframework.httpclient.proxy.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.logging.MaskType.NON;

/**
 * 脱敏配置注解
 * <p>
 * 既可作为元注解组合出复合注解（{@code ANNOTATION_TYPE}），
 * 也可直接标注在方法、类/接口上（可重复标注），例如：
 * <pre>
 * &#64;Masker(keys = {"password", "passwd"}, type = MaskType.FULL)
 * &#64;Masker(keys = "phone", type = MaskType.PHONE)
 * &#64;PrintLog
 * User getUser(...);
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE})
@Inherited
@Repeatable(Maskers.class)
public @interface Masker {

    /**
     * 脱敏处理器,优先级2
     */
    Class<? extends CustomMasker> maskerHandler() default CustomMasker.class;

    /**
     * 脱敏类型,优先级3
     */
    MaskType type() default NON;

    /**
     * 脱敏关键字，大小写不敏感
     */
    String[] keys();


}
