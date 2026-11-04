package com.luckyframework.httpclient.generalapi.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI工具必填参数标记注解，等价于{@code @Param(required = true)}（非校验注解，与lucky-validator的{@code @Required}无关）。
 * <pre>
 *     必填项最终落在其所属层的required数组上：方法参数层的必填进入根schema的required，
 *     POJO字段层的必填进入该对象schema的required。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see Param
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Param(required = true)
public @interface Required {
}
