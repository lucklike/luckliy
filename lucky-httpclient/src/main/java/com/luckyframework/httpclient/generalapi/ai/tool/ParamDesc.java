package com.luckyframework.httpclient.generalapi.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI工具参数描述注解的简写形式，只声明描述文本（非校验注解）。
 * <pre>
 *     {@code @ParamDesc("城市名称")} 等价于 {@code @Param(desc = "城市名称")}，
 *     与 {@link Param} 同时标注时以 {@link Param} 的desc取值为准。
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
public @interface ParamDesc {

    /**
     * 参数描述文本
     */
    String value();
}
