package com.luckyframework.idea.spel.symbol

/**
 * 运行时符号扫描的源码桩。
 *
 * 模拟框架的真实声明形态：
 *
 * - `@SpelSymbol` 字段常量 → 变量；
 * - `HttpClientProxyObjectFactory#importCommonFunction` 中
 *   `addSpringElFunctionClass(X.class)` → 默认注册函数；
 * - 函数类按 `@FunctionAlias`/`@FunctionFilter`/`@Namespace` 展开。
 *
 * 类型统一用 `typeExpression` 写全名，避免测试工程没有挂载框架 jar 时
 * 类字面量无法解析；`SpelRuntimeSymbols` 会去掉包名，断言语义与真实框架一致。
 */
object SpelRuntimeStubs {

    const val VARIABLE_PATH =
        "com/luckyframework/httpclient/proxy/spel/InternalRootVarName.java"

    const val INTERNAL_PATH =
        "com/luckyframework/httpclient/proxy/spel/InternalVarName.java"

    const val ORDINARY_PATH =
        "com/luckyframework/httpclient/proxy/spel/OrdinaryVarName.java"

    const val FUNCTIONS_PATH =
        "com/luckyframework/httpclient/proxy/function/CommonFunctions.java"

    const val SERIALIZATION_FUNCTIONS_PATH =
        "com/luckyframework/httpclient/proxy/function/SerializationFunctions.java"

    const val CIPHER_FUNCTIONS_PATH =
        "com/luckyframework/httpclient/proxy/function/CipherFunctions.java"

    const val FACTORY_PATH =
        "com/luckyframework/httpclient/proxy/HttpClientProxyObjectFactory.java"

    val STUBS: Map<String, String> by lazy {
        mapOf(
        VARIABLE_PATH to ROOT_VARIABLES,
        INTERNAL_PATH to INTERNAL_VARIABLES,
        ORDINARY_PATH to ORDINARY_VARIABLES,
        FUNCTIONS_PATH to FUNCTIONS,
        SERIALIZATION_FUNCTIONS_PATH to SERIALIZATION_FUNCTIONS,
        CIPHER_FUNCTIONS_PATH to CIPHER_FUNCTIONS,
        FACTORY_PATH to FACTORY,
        "com/luckyframework/httpclient/proxy/spel/FunctionAlias.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface FunctionAlias {
    String value();
}
""",
        "com/luckyframework/httpclient/proxy/spel/FunctionFilter.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface FunctionFilter {
}
""",
        "com/luckyframework/httpclient/proxy/spel/Namespace.java" to """
package com.luckyframework.httpclient.proxy.spel;

public @interface Namespace {
    String value() default "";
}
"""
        )
    }

    private val ROOT_VARIABLES = """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbolKind;

@LuckyComponent(type = LuckyComponentType.CONFIG)
public class InternalRootVarName {

    /** 当前API接口的代理对象。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.CLASS)
    public static final String ${'$'}_THIS_${'$'} = "${'$'}this${'$'}";

    /** HttpClient代理对象工厂。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory", from = Lifecycle.CLASS)
    public static final String ${'$'}_HTTP_PROXY_FACTORY_${'$'} = "${'$'}httpProxyFactory${'$'}";

    /** 当前层级捕获到的异常对象。 */
    @SpelSymbol(typeExpression = "java.lang.Throwable", from = Lifecycle.THROWABLE)
    public static final String ${'$'}_THROWABLE_${'$'} = "${'$'}err${'$'}";

    /** 最外层捕获到的异常对象。 */
    @SpelSymbol(typeExpression = "java.lang.Throwable", from = Lifecycle.THROWABLE)
    public static final String ${'$'}_ROOT_THROWABLE_${'$'} = "${'$'}rootErr${'$'}";

    /** 当前方法上下文。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.MethodContext", from = Lifecycle.METHOD)
    public static final String ${'$'}_METHOD_CONTEXT_${'$'} = "${'$'}mc${'$'}";

    /** 当前方法元数据上下文。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.MethodMetaContext", from = Lifecycle.METHOD_META)
    public static final String ${'$'}_METHOD_META_CONTEXT_${'$'} = "${'$'}mec${'$'}";

    /** 当前方法的参数数组。 */
    @SpelSymbol(typeExpression = "java.lang.Object[]", from = Lifecycle.METHOD)
    public static final String ${'$'}_METHOD_ARGS_${'$'} = "${'$'}args${'$'}";

    /** 当前值上下文。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.ValueContext", from = Lifecycle.METHOD)
    public static final String ${'$'}_VALUE_CONTEXT_${'$'} = "${'$'}vc${'$'}";

    /** 参数上下文中的参数索引。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.Integer", from = Lifecycle.METHOD)
    public static final String _PARAM_CONTEXT_INDEX_ = "_index_";

    /** 参数上下文中的参数值。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.Object", from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_VALUE_ = "_value_";

    /** 参数上下文中的原始参数值。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.Object", from = Lifecycle.METHOD)
    public static final String _${'$'}VALUE_CONTEXT_SOURCE_VALUE${'$'}_ = "_${'$'}value${'$'}_";

    /** 参数上下文中的参数名。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.String", from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_NAME_ = "_name_";

    /** 参数上下文中的参数类型。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "org.springframework.core.ResolvableType", from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_TYPE_ = "_type_";

    /** 当前API接口的描述信息。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.generalapi.describe.ApiDescribe", from = Lifecycle.METHOD)
    public static final String ${'$'}_API_${'$'} = "${'$'}api${'$'}";

    /** 当前方法对象。 */
    @SpelSymbol(typeExpression = "java.lang.reflect.Method", from = Lifecycle.METHOD_META)
    public static final String ${'$'}_METHOD_${'$'} = "${'$'}method${'$'}";

    /** 当前方法的返回类型。 */
    @SpelSymbol(typeExpression = "org.springframework.core.ResolvableType", from = Lifecycle.METHOD)
    public static final String ${'$'}_METHOD_RETURN_TYPE_${'$'} = "${'$'}mrt${'$'}";

    /** 当前方法完成结果转换后的返回类型。 */
    @SpelSymbol(typeExpression = "org.springframework.core.ResolvableType", from = Lifecycle.METHOD)
    public static final String ${'$'}_METHOD_CONVERT_RETURN_TYPE_${'$'} = "${'$'}mcrt${'$'}";

    /** 当前方法的参数类型数组。 */
    @SpelSymbol(typeExpression = "org.springframework.core.ResolvableType[]", from = Lifecycle.METHOD_META)
    public static final String ${'$'}_METHOD_PARAM_TYPES_${'$'} = "${'$'}mpts${'$'}";

    /** 当前方法的参数名数组。 */
    @SpelSymbol(typeExpression = "java.lang.String[]", from = Lifecycle.METHOD_META)
    public static final String ${'$'}_METHOD_PARAM_NAMES_${'$'} = "${'$'}pmns${'$'}";

    /** 当前API接口的Class对象。 */
    @SpelSymbol(typeExpression = "java.lang.Class", from = Lifecycle.CLASS)
    public static final String ${'$'}_CLASS_${'$'} = "${'$'}class${'$'}";

    /** 当前上下文对象。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.Context", from = Lifecycle.CLASS)
    public static final String ${'$'}_CURRENT_CONTEXT_${'$'} = "${'$'}CC${'$'}";

    /** 当前类级上下文。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.ClassContext", from = Lifecycle.CLASS)
    public static final String ${'$'}_CLASS_CONTEXT_${'$'} = "${'$'}cc${'$'}";

    /** 当前参数上下文。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.ParameterContext", from = Lifecycle.METHOD)
    public static final String ${'$'}_PARAM_CONTEXT_${'$'} = "${'$'}pc${'$'}";

    /** 当前请求对象。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.core.meta.Request", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_${'$'} = "${'$'}req${'$'}";

    /** 当前响应对象。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.core.meta.Response", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_${'$'} = "${'$'}resp${'$'}";

    /** 响应体的流式视图。 */
    @SpelSymbol(typeExpression = "java.io.InputStream", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_STREAM_BODY_${'$'} = "${'$'}streamBody${'$'}";

    /** 响应体的字节数组视图。 */
    @SpelSymbol(typeExpression = "byte[]", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_BYTE_BODY_${'$'} = "${'$'}byteBody${'$'}";

    /** 响应体的字符串视图。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_STRING_BODY_${'$'} = "${'$'}stringBody${'$'}";

    /** 响应体按JSON转换后的对象。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_JSON_BODY_${'$'} = "${'$'}jsonBody${'$'}";

    /** 响应体按XML转换后的对象。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_XML_BODY_${'$'} = "${'$'}xmlBody${'$'}";

    /** 响应体按Java反序列化后的对象。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_JAVA_BODY_${'$'} = "${'$'}javaBody${'$'}";

    /** 响应体。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_BODY_${'$'} = "${'$'}body${'$'}";

    /** 响应状态码（{@code ${'$'}status${'$'}}）。 */
    @SpelSymbol(typeExpression = "java.lang.Integer", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_STATUS_${'$'} = "${'$'}status${'$'}";

    /** 响应内容类型。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.core.meta.ContentType", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_CONTENT_TYPE_${'$'} = "${'$'}contentType${'$'}";

    /** 响应内容长度。 */
    @SpelSymbol(typeExpression = "java.lang.Long", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_CONTENT_LENGTH_${'$'} = "${'$'}contentLength${'$'}";

    /** 响应头。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_HEADER_${'$'} = "${'$'}respHeader${'$'}";

    /** 响应Cookie。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_RESPONSE_COOKIE_${'$'} = "${'$'}respCookie${'$'}";

    /** 重定向过程中经过的URL链路。 */
    @SpelSymbol(typeExpression = "java.util.List<java.lang.String>", from = Lifecycle.RESPONSE_INIT)
    public static final String ${'$'}_REQUEST_REDIRECT_URL_CHAIN_${'$'} = "${'$'}redirectChain${'$'}";

    /** 方法返回值。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.METHOD_RESULT)
    public static final String ${'$'}_METHOD_RESULT_${'$'} = "${'$'}result${'$'}";

    /** 请求方法。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.core.meta.RequestMethod", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_METHOD_${'$'} = "${'$'}reqMethod${'$'}";

    /** 请求URL。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_URL_${'$'} = "${'$'}url${'$'}";

    /** 请求URL的路径部分。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_URL_PATH_${'$'} = "${'$'}urlPath${'$'}";

    /** 请求查询参数。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_QUERY_${'$'} = "${'$'}query${'$'}";

    /** 请求路径参数。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_PATH_${'$'} = "${'$'}path${'$'}";

    /** 请求头。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_HEADER_${'$'} = "${'$'}reqHeader${'$'}";

    /** 请求Cookie。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_COOKIE_${'$'} = "${'$'}reqCookie${'$'}";

    /** 请求表单参数。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_FORM_${'$'} = "${'$'}form${'$'}";

    /** 请求multipart表单参数。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_MULTIPART_FORM_${'$'} = "${'$'}multiForm${'$'}";

    /** 执行方法上下文初始化的线程。 */
    @SpelSymbol(typeExpression = "java.lang.Thread", from = Lifecycle.METHOD)
    public static final String ${'$'}_METHOD_CONTENT_INIT_THREAD_${'$'} = "${'$'}mcInitThread${'$'}";

    /** 执行当前请求的线程。 */
    @SpelSymbol(typeExpression = "java.lang.Thread", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_REQUEST_THREAD_${'$'} = "${'$'}requestThread${'$'}";

    /** 请求级别的临时变量容器。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_VAR_${'$'} = "__${'$'}Val${'$'}__";

    /** 当前请求关联的Bean对象。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.REQUEST_INIT)
    public static final String ${'$'}_BEAN_${'$'} = "${'$'}bean${'$'}";
}
"""

    private val INTERNAL_VARIABLES = """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbolKind;

@LuckyComponent(type = LuckyComponentType.CONFIG)
public class InternalVarName {

    /** 默认响应转换器。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.configapi.ConfigurationApiFunctionalSupport.ConfigurationApiResponseConvert",
            from = Lifecycle.REQUEST_INIT)
    public static final String __${'$'}REQ_DEFAULT${'$'}__ = "__${'$'}default${'$'}__";

    /** SSE响应转换器。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.sse.SseResponseConvert", from = Lifecycle.REQUEST_INIT)
    public static final String __${'$'}SSE_STREAM${'$'}__ = "__${'$'}sseStream${'$'}__";

    /** SSE事件监听器。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.sse.standard.StandardEventListener", from = Lifecycle.REQUEST)
    public static final String __${'$'}LISTENER_VAR${'$'}__ = "__${'$'}eventListener${'$'}__";

    /** 是否异步请求的标记。 */
    @SpelSymbol(typeExpression = "java.lang.Boolean", from = Lifecycle.REQUEST)
    public static final String __${'$'}ASYNC_TAG${'$'}__ = "__${'$'}async${'$'}__";

    /** 异步执行器名称。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.REQUEST)
    public static final String __${'$'}ASYNC_EXECUTOR${'$'}__ = "__${'$'}asyncExecutor${'$'}__";

    /** 异步并发数。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.REQUEST)
    public static final String __${'$'}ASYNC_CONCURRENCY${'$'}__ = "__${'$'}asyncConcurrency${'$'}__";

    /** 异步模式。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.async.Model", from = Lifecycle.REQUEST)
    public static final String __${'$'}ASYNC_MODEL${'$'}__ = "__${'$'}asyncModel${'$'}__";

    /** HTTP执行器。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.core.executor.HttpExecutor", from = Lifecycle.REQUEST)
    public static final String __${'$'}HTTP_EXECUTOR${'$'}__ = "__${'$'}httpExecutor${'$'}__";

    /** 重试开关。 */
    @SpelSymbol(typeExpression = "java.lang.Boolean", from = Lifecycle.REQUEST)
    public static final String __${'$'}RETRY_SWITCH${'$'}__ = "__${'$'}retrySwitch${'$'}__";

    /** 重试任务名称。 */
    @SpelSymbol(typeExpression = "java.lang.String", from = Lifecycle.REQUEST)
    public static final String __${'$'}RETRY_TASK_NAME${'$'}__ = "__${'$'}retryTaskName${'$'}__";

    /** 重试次数。 */
    @SpelSymbol(typeExpression = "java.lang.Integer", from = Lifecycle.REQUEST)
    public static final String __${'$'}RETRY_COUNT${'$'}__ = "__${'$'}retryCount${'$'}__";

    /** 重试前执行的回调函数。 */
    @SpelSymbol(typeExpression = "java.util.function.Function", from = Lifecycle.REQUEST)
    public static final String __${'$'}RETRY_RUN_BEFORE_RETRY_FUNCTION${'$'}__ = "__${'$'}retryRunBeforeRetryContextFunction${'$'}__";

    /** 重试决策函数。 */
    @SpelSymbol(typeExpression = "java.util.function.Function", from = Lifecycle.REQUEST)
    public static final String __${'$'}RETRY_DECIDER_FUNCTION${'$'}__ = "__${'$'}retryRetryDeciderContextFunction${'$'}__";

    /** 是否为Mock响应。 */
    @SpelSymbol(typeExpression = "java.lang.Boolean", from = Lifecycle.RESPONSE_INIT)
    public static final String __${'$'}IS_MOCK${'$'}__ = "__${'$'}isMock${'$'}__";

    /** Mock响应工厂。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.mock.MockResponseFactory", from = Lifecycle.REQUEST)
    public static final String __${'$'}MOCK_RESPONSE_FACTORY${'$'}__ = "__${'$'}mockResponseFactory${'$'}__";

    /** 结果转换元数据。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.context.ConvertMetaData", from = Lifecycle.METHOD_RESULT)
    public static final String __${'$'}CONVERT_META_TYP${'$'}__ = "__${'$'}convertMetaType${'$'}__";

    /** parameterInstance 函数名常量，不作为变量。 */
    @SpelSymbol(kind = SpelSymbolKind.FUNCTION, typeExpression = "com.luckyframework.httpclient.proxy.context.FunExecutor",
            from = Lifecycle.REQUEST_INIT)
    public static final String __${'$'}PARAMETER_INSTANCE_FUNCTION${'$'}__ = "parameterInstance";
}
"""

    private val ORDINARY_VARIABLES = """
package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;

@LuckyComponent(type = LuckyComponentType.CONFIG)
public class OrdinaryVarName {

    /** 响应耗时统计。 */
    @SpelSymbol(typeExpression = "com.luckyframework.httpclient.proxy.slow.ResponseTimeSpent", from = Lifecycle.RESPONSE_INIT)
    public static final String _${'$'}RESPONSE_TIME_SPENT${'$'}_ = "_${'$'}responseTimeSpent${'$'}_";

    /** 当前重试序号。 */
    @SpelSymbol(typeExpression = "java.lang.Integer", from = Lifecycle.RETRY)
    public static final String _${'$'}RETRY_COUNT${'$'}_ = "_${'$'}retryCount${'$'}_";

    /** 重试任务的结果。 */
    @SpelSymbol(typeExpression = "com.luckyframework.retry.TaskResult", from = Lifecycle.RETRY)
    public static final String _${'$'}TASK_RESULT${'$'}_ = "_${'$'}taskResult${'$'}_";

    /** 响应对象是否已被修改。 */
    @SpelSymbol(typeExpression = "java.lang.Boolean", from = Lifecycle.RESPONSE)
    public static final String _${'$'}RESPONSE_CHANGE${'$'}_ = "_${'$'}responseChange${'$'}_";
}
"""

    private val FUNCTIONS = """
package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;

public class CommonFunctions {

    /** 获取当前时间毫秒 */
    public static long time() {
        return 0L;
    }

    /** 格式化日期 */
    @FunctionAlias("format_date")
    public static String formatDate(Object date, String pattern) {
        return "";
    }

    /** 字符串插值 */
    public static String str(String template, Object... args) {
        return template;
    }

    /** 字符串相等判断 */
    @FunctionAlias("str_eq")
    public static boolean strEquals(Object left, Object right) {
        return false;
    }

    /** 字符串不等判断 */
    @FunctionAlias("str_ne")
    public static boolean strNotEquals(Object left, Object right) {
        return false;
    }

    /** 计算MD5 */
    public static String md5(Object value, String... salt) {
        return "";
    }
}
"""

    private val SERIALIZATION_FUNCTIONS = """
package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;

@Namespace("__Func::Serialization__")
public class SerializationFunctions {

    /** Base64解码为字节数组 */
    public static byte[] _base64(Object object, String... charsets) {
        return new byte[0];
    }

    /** Base64解码为字符串 */
    @FunctionAlias("_base64_str")
    public static String _base64Str(Object object, String... charsets) {
        return "";
    }

    /** URL解码 */
    public static String _url(String str, String... charset) {
        return str;
    }
}
"""

    private val CIPHER_FUNCTIONS = """
package com.luckyframework.httpclient.proxy.function;

import com.luckyframework.httpclient.proxy.spel.FunctionAlias;
import com.luckyframework.httpclient.proxy.spel.Namespace;

@Namespace("__Func::Cipher__")
public class CipherFunctions {

    /** 模版编码 */
    @FunctionAlias("temp_encode")
    public static String tempEncode(String sourceTxt, String template) {
        return sourceTxt;
    }
}
"""

    private val FACTORY = """
package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.proxy.function.CipherFunctions;
import com.luckyframework.httpclient.proxy.function.CommonFunctions;
import com.luckyframework.httpclient.proxy.function.SerializationFunctions;

public class HttpClientProxyObjectFactory {

    public void importCommonFunction() {
        addSpringElFunctionClass(CommonFunctions.class);
        addSpringElFunctionClass(SerializationFunctions.class);
        addSpringElFunctionClass(CipherFunctions.class);
    }

    public void addSpringElFunctionClass(Class<?> functionClass) {
    }
}
"""
}
