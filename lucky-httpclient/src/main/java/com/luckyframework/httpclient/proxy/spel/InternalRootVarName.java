package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.describe.ApiDescribe;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;
import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponentType;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;
import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbolKind;
import org.springframework.core.ResolvableType;

import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * 内部参数名
 */
@LuckyComponent(type = LuckyComponentType.CONFIG, group = "config", priority = 4)
public class InternalRootVarName {

    /** 当前API接口的代理对象，即 {@code $this$} 引用的实例。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.CLASS)
    public static final String $_THIS_$ = "$this$";

    /** HttpClient代理对象工厂。 */
    @SpelSymbol(type = HttpClientProxyObjectFactory.class, from = Lifecycle.CLASS)
    public static final String $_HTTP_PROXY_FACTORY_$ = "$httpProxyFactory$";

    /** 当前层级捕获到的异常对象（{@code $err$}）。 */
    @SpelSymbol(type = Throwable.class, from = Lifecycle.THROWABLE)
    public static final String $_THROWABLE_$ = "$err$";

    /** 最外层捕获到的异常对象（{@code $rootErr$}）。 */
    @SpelSymbol(type = Throwable.class, from = Lifecycle.THROWABLE)
    public static final String $_ROOT_THROWABLE_$ = "$rootErr$";


    /** 当前方法上下文（{@code $mc$}）。 */
    @SpelSymbol(type = MethodContext.class, from = Lifecycle.METHOD)
    public static final String $_METHOD_CONTEXT_$ = "$mc$";

    /** 当前方法元数据上下文（{@code $mec$}）。 */
    @SpelSymbol(type = MethodMetaContext.class, from = Lifecycle.METHOD_META)
    public static final String $_METHOD_META_CONTEXT_$ = "$mec$";

    /** 当前方法的参数数组（{@code $args$}）。 */
    @SpelSymbol(type = Object[].class, from = Lifecycle.METHOD)
    public static final String $_METHOD_ARGS_$ = "$args$";

    /** 当前值上下文，描述一个待处理的值（{@code $vc$}）。 */
    @SpelSymbol(type = ValueContext.class, from = Lifecycle.METHOD)
    public static final String $_VALUE_CONTEXT_$ = "$vc$";

    /** 参数上下文中的参数索引（{@code _index_}）。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, type = Integer.class, from = Lifecycle.METHOD)
    public static final String _PARAM_CONTEXT_INDEX_ = "_index_";

    /** 参数上下文中的参数值（{@code _value_}）。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.Object", from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_VALUE_ = "_value_";

    /** 参数上下文中的原始参数值（{@code _$value$_}）。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, typeExpression = "java.lang.Object", from = Lifecycle.METHOD)
    public static final String _$VALUE_CONTEXT_SOURCE_VALUE$_ = "_$value$_";

    /** 参数上下文中的参数名（{@code _name_}）。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, type = String.class, from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_NAME_ = "_name_";

    /** 参数上下文中的参数类型（{@code _type_}）。 */
    @SpelSymbol(kind = SpelSymbolKind.ROOT, type = ResolvableType.class, from = Lifecycle.METHOD)
    public static final String _VALUE_CONTEXT_TYPE_ = "_type_";


    /** 当前API接口的描述信息（{@code $api$}）。 */
    @SpelSymbol(type = ApiDescribe.class, from = Lifecycle.METHOD)
    public static final String $_API_$ = "$api$";

    /** 当前方法对象（{@code $method$}）。 */
    @SpelSymbol(type = Method.class, from = Lifecycle.METHOD_META)
    public static final String $_METHOD_$ = "$method$";

    /** 当前方法的返回类型（{@code $mrt$}）。 */
    @SpelSymbol(type = ResolvableType.class, from = Lifecycle.METHOD)
    public static final String $_METHOD_RETURN_TYPE_$ = "$mrt$";

    /** 当前方法完成结果转换后的返回类型（{@code $mcrt$}）。 */
    @SpelSymbol(type = ResolvableType.class, from = Lifecycle.METHOD)
    public static final String $_METHOD_CONVERT_RETURN_TYPE_$ = "$mcrt$";

    /** 当前方法的参数类型数组（{@code $mpts$}）。 */
    @SpelSymbol(type = ResolvableType[].class, from = Lifecycle.METHOD_META)
    public static final String $_METHOD_PARAM_TYPES_$ = "$mpts$";

    /** 当前方法的参数名数组（{@code $pmns$}）。 */
    @SpelSymbol(type = String[].class, from = Lifecycle.METHOD_META)
    public static final String $_METHOD_PARAM_NAMES_$ = "$pmns$";

    /** 当前API接口的Class对象（{@code $class$}）。 */
    @SpelSymbol(type = Class.class, from = Lifecycle.CLASS)
    public static final String $_CLASS_$ = "$class$";

    /** 当前上下文对象（{@code $CC$}）。 */
    @SpelSymbol(type = Context.class, from = Lifecycle.CLASS)
    public static final String $_CURRENT_CONTEXT_$ = "$CC$";

    /** 当前类级上下文（{@code $cc$}）。 */
    @SpelSymbol(type = ClassContext.class, from = Lifecycle.CLASS)
    public static final String $_CLASS_CONTEXT_$ = "$cc$";

    /** 当前参数上下文（{@code $pc$}）。 */
    @SpelSymbol(type = ParameterContext.class, from = Lifecycle.METHOD)
    public static final String $_PARAM_CONTEXT_$ = "$pc$";

    /** 当前请求对象（{@code $req$}）。 */
    @SpelSymbol(type = Request.class, from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_$ = "$req$";

    /** 当前响应对象（{@code $resp$}）。 */
    @SpelSymbol(type = Response.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_$ = "$resp$";

    /** 响应体的流式视图（{@code $streamBody$}）。 */
    @SpelSymbol(type = InputStream.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_STREAM_BODY_$ = "$streamBody$";

    /** 响应体的字节数组视图（{@code $byteBody$}）。 */
    @SpelSymbol(type = byte[].class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_BYTE_BODY_$ = "$byteBody$";

    /** 响应体的字符串视图（{@code $stringBody$}）。 */
    @SpelSymbol(type = String.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_STRING_BODY_$ = "$stringBody$";

    /** 响应体按JSON转换后的对象（{@code $jsonBody$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_JSON_BODY_$ = "$jsonBody$";

    /** 响应体按XML转换后的对象（{@code $xmlBody$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_XML_BODY_$ = "$xmlBody$";

    /** 响应体按Java反序列化后的对象（{@code $javaBody$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_JAVA_BODY_$ = "$javaBody$";

    /** 当前响应体（{@code $body$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_BODY_$ = "$body$";

    /** 响应状态码（{@code $status$}）。 */
    @SpelSymbol(type = Integer.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_STATUS_$ = "$status$";

    /** 响应内容类型（{@code $contentType$}）。 */
    @SpelSymbol(type = ContentType.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_CONTENT_TYPE_$ = "$contentType$";

    /** 响应内容长度（{@code $contentLength$}）。 */
    @SpelSymbol(type = Long.class, from = Lifecycle.RESPONSE_INIT)
    public static final String $_CONTENT_LENGTH_$ = "$contentLength$";

    /** 响应头（{@code $respHeader$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_HEADER_$ = "$respHeader$";

    /** 响应Cookie（{@code $respCookie$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.RESPONSE_INIT)
    public static final String $_RESPONSE_COOKIE_$ = "$respCookie$";

    /** 重定向过程中经过的URL链路（{@code $redirectChain$}）。 */
    @SpelSymbol(typeExpression = "java.util.List<java.lang.String>", from = Lifecycle.RESPONSE_INIT)
    public static final String $_REQUEST_REDIRECT_URL_CHAIN_$ = "$redirectChain$";

    /** 方法返回值（{@code $result$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.METHOD_RESULT)
    public static final String $_METHOD_RESULT_$ = "$result$";

    /** 请求方法（{@code $reqMethod$}）。 */
    @SpelSymbol(type = RequestMethod.class, from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_METHOD_$ = "$reqMethod$";

    /** 请求URL（{@code $url$}）。 */
    @SpelSymbol(type = String.class, from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_URL_$ = "$url$";

    /** 请求URL的路径部分（{@code $urlPath$}）。 */
    @SpelSymbol(type = String.class, from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_URL_PATH_$ = "$urlPath$";

    /** 请求查询参数（{@code $query$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_QUERY_$ = "$query$";

    /** 请求路径参数（{@code $path$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_PATH_$ = "$path$";

    /** 请求头（{@code $reqHeader$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_HEADER_$ = "$reqHeader$";

    /** 请求Cookie（{@code $reqCookie$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_COOKIE_$ = "$reqCookie$";

    /** 请求表单参数（{@code $form$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_FORM_$ = "$form$";

    /** 请求multipart表单参数（{@code $multiForm$}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_MULTIPART_FORM_$ = "$multiForm$";


    /** 执行方法上下文初始化的线程（{@code $mcInitThread$}）。 */
    @SpelSymbol(type = Thread.class, from = Lifecycle.METHOD)
    public static final String $_METHOD_CONTENT_INIT_THREAD_$ = "$mcInitThread$";

    /** 执行当前请求的线程（{@code $requestThread$}）。 */
    @SpelSymbol(type = Thread.class, from = Lifecycle.REQUEST_INIT)
    public static final String $_REQUEST_THREAD_$ = "$requestThread$";


    /** 请求级别的临时变量容器（{@code __$Val$__}）。 */
    @SpelSymbol(typeExpression = "java.util.Map<java.lang.String, java.lang.Object>", from = Lifecycle.REQUEST_INIT)
    public static final String $_VAR_$ = "__$Val$__";

    /** 当前请求关联的Bean对象（{@code $bean$}）。 */
    @SpelSymbol(typeExpression = "java.lang.Object", from = Lifecycle.REQUEST_INIT)
    public static final String $_BEAN_$ = "$bean$";

}
