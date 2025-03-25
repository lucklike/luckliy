package com.luckyframework.httpclient.generalapi.openai;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 为OpenAI的Function Calling 中的某个工具函数的参数添加描述信息
 *
 * {@code
 *
 * string、integer、boolean、number
 * {
 *   "name": "get_weather",
 *   "parameters": {
 *     "type": "object",
 *     "properties": {
 *       "city": {"type": "string"},
 *       "temperature_unit": {"type": "string", "enum": ["C", "F"]},
 *       "days": {"type": "number"},
 *       "include_humidity": {"type": "boolean"}
 *     },
 *     "required": ["city"]
 *   }
 * }
 *
 * array:
 * {
 *   "name": "send_emails",
 *   "parameters": {
 *     "type": "object",
 *     "properties": {
 *       "emails": {
 *         "type": "array",
 *         "items": {"type": "string"}
 *       }
 *     },
 *     "required": ["emails"]
 *   }
 * }
 *
 * object:
 * {
 *   "name": "create_user",
 *   "parameters": {
 *     "type": "object",
 *     "properties": {
 *       "user": {
 *         "type": "object",
 *         "properties": {
 *           "name": {"type": "string"},
 *           "age": {"type": "integer"},
 *           "email": {"type": "string"}
 *         },
 *         "required": ["name", "email"]
 *       }
 *     },
 *     "required": ["user"]
 *   }
 * }
 *
 * object-array:
 * {
 *   "name": "register_users",
 *   "parameters": {
 *     "type": "object",
 *     "properties": {
 *       "users": {
 *         "type": "array",
 *         "items": {
 *           "type": "object",
 *           "properties": {
 *             "name": { "type": "string" },
 *             "email": { "type": "string" },
 *             "age": { "type": "integer" }
 *           },
 *           "required": ["name", "email"]
 *         }
 *       }
 *     },
 *     "required": ["users"]
 *   }
 * }
 * }
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Param {

    /**
     * 工具方法的表述信息
     */
    @AliasFor("desc")
    String value() default "";

    /**
     * 工具方法的表述信息
     */
    @AliasFor("value")
    String desc() default "";

    /**
     * 参数名
     */
    String name() default "";

    /**
     * 参数类型，不配置时会根据参数类型自动推断
     * <pre>
     *     string     : 可以使用 enum 限制
     *     number     : 适用于整数和浮点数
     *     integer    : 适用于整型
     *     boolean    : true/false
     *     array      : 需要指定 items 类型
     *     object     : 支持嵌套对象
     *     null       : 但一般不常用
     * </pre>
     */
    String type() default "";

    /**
     * 枚举
     */
    String[] enumerate() default {};

    /**
     * 是否为必填参数，默认false
     */
    boolean required() default false;


}
