package com.luckyframework.httpclient.generalapi.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI工具参数描述注解，标注在方法参数或POJO字段上（非校验注解，与lucky-validator的{@code @Required}无关）。
 * <pre>
 *     name      显式覆盖参数名，缺省时取编译期保留的真实参数名；
 *     desc      参数描述，写入schema对应属性的description；
 *     type      显式覆盖JSON Schema类型名（如"string"），缺省时由Java类型映射；
 *     enumerate 限定取值集合，非空时写入schema的enum数组；
 *     ignore    为true时本参数不进入schema（也不要求模型提供）；
 *     required  为true时本参数进入所属层的required数组。
 *
 *     只想要描述或只想要必填时可用简写注解 {@link ParamDesc} 与 {@link Required}，
 *     它们与本注解同时存在时，本注解的取值优先。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see Tool
 * @see ParamDesc
 * @see Required
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * 参数名，默认为编译期保留的真实参数名
     */
    String name() default "";

    /**
     * 参数描述
     */
    String desc() default "";

    /**
     * 显式指定的JSON Schema类型名，为空时由Java类型映射得出
     */
    String type() default "";

    /**
     * 限定取值集合
     */
    String[] enumerate() default {};

    /**
     * 是否把本参数从工具声明中忽略
     */
    boolean ignore() default false;

    /**
     * 是否为必填参数
     */
    boolean required() default false;
}
