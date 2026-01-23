package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.meta.RequestMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法名自动推导为URL
 * <pre>
 *     方法名规则：
 *     {RequestMethod}${path1}_{path2}_...._{pathn}
 *
 *     例如：
 *     1.
 *     post$user_getList
 *     ->
 *     POST  /user/getList
 *
 *     2.getList
 *     ->
 *     method /getList
 *
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface UseAutoUrlDerivationInsurance {

    /**
     * 是否启用自动URL推导
     */
    boolean value() default true;

    /**
     * 默认使用的请求方式
     */
    RequestMethod defaultMethod() default RequestMethod.POST;
}
