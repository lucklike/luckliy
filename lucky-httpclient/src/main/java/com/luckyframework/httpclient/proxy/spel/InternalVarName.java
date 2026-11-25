package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.async.Model;
import com.luckyframework.httpclient.proxy.context.ConvertMetaData;
import com.luckyframework.httpclient.proxy.context.FunExecutor;
import com.luckyframework.httpclient.proxy.mock.MockResponseFactory;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbolKind;
import com.luckyframework.httpclient.proxy.sse.SseResponseConvert;
import com.luckyframework.httpclient.proxy.sse.standard.StandardEventListener;

import java.util.function.Function;

/**
 * 内部参数名
 */
@LuckyComponent(type = LuckyComponentType.CONFIG, group = "config", priority = 4)
public class InternalVarName {

    /**
     * {@code ConfigApi#type}的默认取值，对应默认响应转换器。
     * <p>具体实现类{@code ConfigurationApiResponseConvert}是包级可见的内部类，
     * 无法跨包引用，这里用{@code typeExpression}写全名。
     */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.configapi.ConfigurationApiFunctionalSupport.ConfigurationApiResponseConvert",
            from = Lifecycle.REQUEST_INIT)
    public static final String __$REQ_DEFAULT$__ = "__$default$__";

    /**
     * {@code ConfigApi#type}的SSE取值，对应SSE响应转换器。
     */
    @SpelSymbol(type = SseResponseConvert.class, from = Lifecycle.REQUEST_INIT)
    public static final String __$SSE_STREAM$__ = "__$sseStream$__";

    /** SSE事件监听器。 */
    @SpelSymbol(type = StandardEventListener.class, from = Lifecycle.REQUEST)
    public static final String __$LISTENER_VAR$__ = "__$eventListener$__";

    /** 是否异步请求的标记。 */
    @SpelSymbol(type = Boolean.class, from = Lifecycle.REQUEST)
    public static final String __$ASYNC_TAG$__ = "__$async$__";

    /** 异步执行器名称。 */
    @SpelSymbol(type = String.class, from = Lifecycle.REQUEST)
    public static final String __$ASYNC_EXECUTOR$__ = "__$asyncExecutor$__";

    /** 异步并发数。 */
    @SpelSymbol(type = String.class, from = Lifecycle.REQUEST)
    public static final String __$ASYNC_CONCURRENCY$__ = "__$asyncConcurrency$__";

    /** 异步模式。 */
    @SpelSymbol(type = Model.class, from = Lifecycle.REQUEST)
    public static final String __$ASYNC_MODEL$__ = "__$asyncModel$__";

    /** HTTP执行器。 */
    @SpelSymbol(type = HttpExecutor.class, from = Lifecycle.REQUEST)
    public static final String __$HTTP_EXECUTOR$__ = "__$httpExecutor$__";

    /** 重试开关。 */
    @SpelSymbol(type = Boolean.class, from = Lifecycle.REQUEST)
    public static final String __$RETRY_SWITCH$__ = "__$retrySwitch$__";

    /** 重试任务名称。 */
    @SpelSymbol(type = String.class, from = Lifecycle.REQUEST)
    public static final String __$RETRY_TASK_NAME$__ = "__$retryTaskName$__";

    /** 重试次数。 */
    @SpelSymbol(type = Integer.class, from = Lifecycle.REQUEST)
    public static final String __$RETRY_COUNT$__ = "__$retryCount$__";

    /** 重试前执行的回调函数。 */
    @SpelSymbol(type = Function.class, from = Lifecycle.REQUEST)
    public static final String __$RETRY_RUN_BEFORE_RETRY_FUNCTION$__ = "__$retryRunBeforeRetryContextFunction$__";

    /** 重试决策函数。 */
    @SpelSymbol(type = Function.class, from = Lifecycle.REQUEST)
    public static final String __$RETRY_DECIDER_FUNCTION$__ = "__$retryRetryDeciderContextFunction$__";

    /** 是否为Mock响应。 */
    @SpelSymbol(type = Boolean.class, from = Lifecycle.RESPONSE_INIT)
    public static final String __$IS_MOCK$__ = "__$isMock$__";

    /** Mock响应工厂。 */
    @SpelSymbol(type = MockResponseFactory.class, from = Lifecycle.REQUEST)
    public static final String __$MOCK_RESPONSE_FACTORY$__ = "__$mockResponseFactory$__";

    /** 结果转换元数据。 */
    @SpelSymbol(type = ConvertMetaData.class, from = Lifecycle.METHOD_RESULT)
    public static final String __$CONVERT_META_TYP$__ = "__$convertMetaType$__";

    /**
     * 扩展函数{@code parameterInstance}的函数名常量。
     * <p>这里标注的是常量本身，不代表函数声明；函数是否可用及其生命周期
     * 由{@code @SPELImport}与{@code HttpClientProxyObjectFactory#addSpringElFunctionClass(Class)}决定。
     */
    @SpelSymbol(kind = SpelSymbolKind.FUNCTION, type = FunExecutor.class, from = Lifecycle.REQUEST_INIT)
    public static final String __$PARAMETER_INSTANCE_FUNCTION$__ = "parameterInstance";

}
