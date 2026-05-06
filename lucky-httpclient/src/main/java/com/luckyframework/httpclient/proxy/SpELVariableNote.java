package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.meta.ContentType;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.describe.ApiDescribe;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.spel.InternalRootVarName;
import com.luckyframework.httpclient.proxy.spel.OrdinaryVarName;
import org.springframework.core.ResolvableType;

import java.io.InputStream;
import java.util.Map;

/**
 * SpEL内部变量相关的注释类
 * <h3>内置的SpEL变量及其解释说明如下：</h3>
 * <pre>
 *     {@link ClassContext}
 *     {@value InternalRootVarName#$_CLASS_CONTEXT_$}               -> {@link ClassContext}
 *     {@value InternalRootVarName#$_HTTP_PROXY_FACTORY_$}          -> {@link HttpClientProxyObjectFactory}
 *     {@value InternalRootVarName#$_THIS_$}                        -> {@code 当前API的代理对象}
 *     {@value InternalRootVarName#$_CLASS_$}                       -> {@code 当前API的Class对象}
 *
 *     {@link MethodMetaContext}
 *     {@value InternalRootVarName#$_METHOD_META_CONTEXT_$}         -> {@link MethodMetaContext}
 *     {@value InternalRootVarName#$_METHOD_$}                      -> {@code 当前API的Method对象}
 *     {@value InternalRootVarName#$_METHOD_RETURN_TYPE_$}          -> {@code 当前API方法的返回值类型}({@link ResolvableType})
 *     {@value InternalRootVarName#$_METHOD_CONVERT_RETURN_TYPE_$}  -> {@code 当前API方法的转换目标类型}({@link ResolvableType})
 *     {@value InternalRootVarName#$_METHOD_PARAM_TYPES_$}          -> {@code 当前API方法的所有参数类型}
 *     {@value InternalRootVarName#$_METHOD_PARAM_NAMES_$}          -> {@code 当前API方法的所有参数名称}
 *
 *     {@link MethodContext}
 *     {@value InternalRootVarName#$_METHOD_CONTEXT_$}              -> {@link MethodContext}
 *     {@value InternalRootVarName#$_METHOD_ARGS_$}                 -> {@code 当前API方法在运行时的参数列表}
 *     {@value InternalRootVarName#$_METHOD_CONTENT_INIT_THREAD_$}  -> {@code 当前API方法初始化时所在的线程}({@link Thread})
 *     {@value InternalRootVarName#$_API_$}                         -> {@code 当前API方法的描述信息}({@link ApiDescribe})
 *
 *     {@link ParameterContext}
 *     {@value InternalRootVarName#$_VALUE_CONTEXT_$}               -> {@code 当前参数对应的}({@link ValueContext})
 *     {@value InternalRootVarName#_VALUE_CONTEXT_NAME_}            -> {@code 当前参数的参数名}
 *     {@value InternalRootVarName#_VALUE_CONTEXT_TYPE_}            -> {@code 当前参数的类型}({@link ResolvableType})
 *     {@value InternalRootVarName#_PARAM_CONTEXT_INDEX_}           -> {@code 当前参数的位置}
 *     {@value InternalRootVarName#_VALUE_CONTEXT_VALUE_}           -> {@code 当前最终参数值}
 *     {@value InternalRootVarName#_$VALUE_CONTEXT_SOURCE_VALUE$_}  -> {@code 当前原始参数值}
 *     {@code $pc$name}                                             -> {@code 名称为name的参数对应的}({@link ParameterContext})
 *     {@code $pc$n}                                                -> {@code 第n个参数对应的}({@link ParameterContext})
 *     {@code name}                                                 -> {@code 名称为name的最终参数值}
 *     {@code $name}                                                -> {@code 名称为name的原始参数值}
 *     {@code $n}                                                   -> {@code 第n个参数的最终值}
 *     {@code $$n}                                                  -> {@code 第n个参数的原始值}
 *     {@code name::type}                                           -> {@code 名称为name的参数的类型}
 *     {@code $n::type}                                             -> {@code 第n个参数的类型}
 *
 *     {@code Context}
 *     {@value InternalRootVarName#$_CURRENT_CONTEXT_$}             -> {@code 当前上下文}({@link Context})
 *
 *     {@code REQUEST}
 *     {@value InternalRootVarName#$_REQUEST_$}                     -> {@code 当前请求对象}({@link Request})
 *     {@value InternalRootVarName#$_REQUEST_URL_$}                 -> {@code 当前请求的URL}({@link String})
 *     {@value InternalRootVarName#$_REQUEST_URL_PATH_$}            -> {@code 当前请求的URL地址的Path部分}({@link String})
 *     {@value InternalRootVarName#$_REQUEST_METHOD_$}              -> {@code 当前请求使用的方法类型}({@link RequestMethod})
 *     {@value InternalRootVarName#$_REQUEST_QUERY_$}               -> {@code 当前请求的Query参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_PATH_$}                -> {@code 当前请求的Path参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_FORM_$}                -> {@code 当前请求的Form表单参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_MULTIPART_FORM_$}      -> {@code 当前请求的Multipart-Form表单参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_HEADER_$}              -> {@code 当前请求的Header参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_COOKIE_$}              -> {@code 当前请求的Cookie参数}({@link Map})
 *     {@value InternalRootVarName#$_REQUEST_THREAD_$}              -> {@code 执行当前请求的线程} ({@link Thread})
 *
 *     {@code RESPONSE}
 *     {@value InternalRootVarName#$_RESPONSE_$}                     -> {@code 当前响应对象}({@link Response})
 *     {@value InternalRootVarName#$_RESPONSE_STATUS_$}              -> {@code 当前响应的状态码}({@link Integer})
 *     {@value InternalRootVarName#$_CONTENT_LENGTH_$}               -> {@code 当前响应体的长度}({@link Long})
 *     {@value InternalRootVarName#$_CONTENT_TYPE_$}                 -> {@code 当前响应体的Content-Type}({@link ContentType})
 *     {@value InternalRootVarName#$_RESPONSE_HEADER_$}              -> {@code 当前响应头Header}({@link Map})
 *     {@value InternalRootVarName#$_RESPONSE_COOKIE_$}              -> {@code 当前响应Cookie}({@link Map})
 *     {@value InternalRootVarName#$_RESPONSE_STREAM_BODY_$}         -> {@code 流式响应体}({@link InputStream})
 *     {@value InternalRootVarName#$_RESPONSE_STRING_BODY_$}         -> {@code 字符串格式的响应体}({@link String})
 *     {@value InternalRootVarName#$_RESPONSE_BYTE_BODY_$}           -> {@code byte[]类型的响应体}
 *     {@value InternalRootVarName#$_RESPONSE_BODY_$}                -> {@code 对象类型的响应体}({@link Object})
 *
 *     {@code OTHERS}
 *     {@value OrdinaryVarName#_$RESPONSE_TIME_SPENT$_}              -> {@code 请求执行时间}({@link Long})
 *     {@value InternalRootVarName#$_THROWABLE_$}                    -> {@code 请求过程中出现的异常}({@link Throwable})
 * </pre>
 */
public abstract class SpELVariableNote {
}
