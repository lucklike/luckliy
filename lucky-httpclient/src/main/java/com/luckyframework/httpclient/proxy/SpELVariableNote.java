package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.generalapi.describe.ApiDescribe;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.context.MethodMetaContext;
import com.luckyframework.httpclient.proxy.context.ParameterContext;
import com.luckyframework.httpclient.proxy.spel.InternalRootVarName;
import org.springframework.core.ResolvableType;

/**
 * SpEL内部变量相关的注释类
 * <pre>
 *     内置的SpEL变量及其解释说明如下：
 *
 *     {@link ClassContext}
 *     {@value InternalRootVarName#$_CLASS_CONTEXT_$}               ->  {@link ClassContext}
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
 *     {@value InternalRootVarName#$_UNIQUE_ID_$}                   -> {@code 当前API方法在运行时产生的唯一ID}
 *     {@value InternalRootVarName#$_METHOD_CONTEXT_$}              ->  {@link MethodContext}
 *     {@value InternalRootVarName#$_METHOD_ARGS_$}                 -> {@code 当前API方法在运行时的参数列表}
 *     {@value InternalRootVarName#$_METHOD_CONTENT_INIT_THREAD_$}  -> {@code 当前API方法初始化时所在的线程}({@link Thread})
 *     {@value InternalRootVarName#$_API_$}                         -> {@code 当前API方法的描述信息}({@link ApiDescribe})
 *
 *     {@link ParameterContext}
 *     {@value InternalRootVarName#_PARAM_CONTEXT_INDEX_}           -> {@code 当前参数的位置}
 *     {@value InternalRootVarName#_VALUE_CONTEXT_VALUE_}           -> {@code 当前最终参数值}
 *     {@value InternalRootVarName#_$VALUE_CONTEXT_SOURCE_VALUE$_}  -> {@code 当前原始参数值}
 *     {@code  name}                                                -> {@code 名称为name的最终参数值}
 *     {@code $name }                                               -> {@code 名称为name的原始参数值}
 *     {@code $n}                                                   -> {@code 第n个参数的最终值}
 *     {@code $$n}                                                  -> {@code 第n个参数的原始值}
 *     {@code name::type}                                           -> {@code 名称为name的参数的类型}
 *     {@code $n::type}                                             -> {@code 第n个参数的类型}
 *
 *
 * </pre>
 */
public class SpELVariableNote {
}
