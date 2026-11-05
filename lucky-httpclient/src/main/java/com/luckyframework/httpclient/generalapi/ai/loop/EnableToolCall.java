package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.proxy.plugin.Plugin;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 工具调用回环启用注解。
 * <pre>
 *     标注在会话接口的方法上，该方法即获得「发请求 → 模型要求调用工具 → 本地执行 → 结果回填 → 带历史重放」
 *     的有界回环能力，由 {@link ToolCallProxyPlugin} 与 {@link FunctionCallingRunner} 实现；
 *     未标注本注解的方法行为完全不变。
 *
 *     用法：
 *     {@code
 *     @Post("/chat/completions")
 *     @EnableToolCall(tools = {"get_weather"}, registry = "#{myToolRegistry}")
 *     ChatResponse chat(@JsonBody ChatRequest request);
 *     }
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see ToolCallProxyPlugin
 * @see FunctionCallingRunner
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Plugin(pluginClass = ToolCallProxyPlugin.class)
@Combination(Plugin.class)
public @interface EnableToolCall {

    /**
     * 本次调用可用的工具子集名称，为空时取注册表全集
     */
    String[] tools() default {};

    /**
     * 工具注册表的SpEL表达式，为空时使用 {@link com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry#getDefault()}
     */
    String registry() default "";

    /**
     * 回环轮次上限，支持SpEL表达式，默认5轮
     */
    String maxRounds() default "5";

    /**
     * 工具执行失败时的处理策略，默认把失败作为工具结果回填给模型自纠
     */
    ToolCallFailurePolicy failurePolicy() default ToolCallFailurePolicy.CONTINUE;

    /**
     * 厂商方言（{@link com.luckyframework.httpclient.generalapi.ai.vendor.VendorAdapter}）的SpEL表达式。
     * 阻塞式回环不需要填写（报文翻译由响应转换器完成），
     * 流式回环需要它来创建增量拼接器，未填写时会抛出明确异常
     */
    String dialect() default "";
}
