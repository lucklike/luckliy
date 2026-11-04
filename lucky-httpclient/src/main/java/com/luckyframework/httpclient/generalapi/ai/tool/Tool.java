package com.luckyframework.httpclient.generalapi.ai.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI工具声明注解，标注在方法上即可生成一条工具声明（非校验注解）。
 * <pre>
 *     工具名默认取方法名，{@code name} 可显式覆盖；描述取自 {@code desc}，
 *     缺失时注册阶段抛异常并指出具体方法。参数上的描述与约束见 {@link Param}。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see Param
 * @see ToolRegistry#register(Object)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * 工具名，默认为方法名
     */
    String name() default "";

    /**
     * 工具描述，供模型判断何时调用本工具
     */
    String desc() default "";
}
