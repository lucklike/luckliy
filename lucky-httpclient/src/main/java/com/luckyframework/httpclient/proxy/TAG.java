package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.ClassContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;

/**
 * 注解标签，复用注释
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/26 01:25
 */
public class TAG {

    //------------------------------------------------------------------------------------
    //                            SpEL Env Tags
    //------------------------------------------------------------------------------------

    /**
     * rootVarName:   通过{@code HttpClientProxyObjectFactory.addSpringElRootVariable}系列方法导入的参数，例如：{@code #{appKey}}
     */
    public static final String SPRING_ROOT_VAL = "rootVarName:   通过{@code HttpClientProxyObjectFactory.addSpringElRootVariable}系列方法导入的参数，例如：{@code #{appKey}}";

    /**
     * #varName:   通过{@code HttpClientProxyObjectFactory.addSpringElVariable}系列方法导入的参数，例如：{@code #{#privateKey}}；以及通过{@code HttpClientProxyObjectFactory.addSpringElFunction}系列方法导入的函数，例如：{@code #{#TO_JSON(obj)}}
     */
    public static final String SPRING_VAL = "#varName:   通过{@code HttpClientProxyObjectFactory.addSpringElVariable}系列方法导入的参数，例如：{@code #{#privateKey}}；以及通过{@code HttpClientProxyObjectFactory.addSpringElFunction}系列方法导入的函数，例如：{@code #{#TO_JSON(obj)}}";

    //------------------------------------------------------------------------------------
    //                            Context Tags
    //------------------------------------------------------------------------------------


    /**
     * $mc$:      当前方法上下文{@link MethodContext}
     */
    public static final String METHOD_CONTEXT = "$mc$:      当前方法上下文{@link MethodContext}";

    /**
     * $cc$:      当前类上下文{@link ClassContext}
     */
    public static final String CLASS_CONTEXT = "$cc$:      当前类上下文{@link ClassContext}";

    /**
     * $method$:  当前执行的接口方法实例{@link Method}
     */
    public static final String METHOD = "$method$:  当前执行的接口方法实例{@link Method}";

    /**
     * $class$:   当前执行的接口所在类{@link Class}
     */
    public static final String CLASS = "$class$:   当前执行的接口所在类{@link Class}";

    /**
     * $this$:    当前接口的代理对象{@link MethodContext#getProxyObject()}
     */
    public static final String THIS = "$this$:    当前接口的代理对象{@link MethodContext#getProxyObject()}";

    /**
     * paramName_type:        参数名称为paramName的参数类型{@link ResolvableType}
     */
    public static final String PARAM_TYPE = "paramName_type:        参数名称为paramName的参数类型{@link ResolvableType}";

    /**
     * pn:        参数列表第n个参数(eg: p0, p1 注：参数索引是从0开始的){@link Object}
     */
    public static final String PN = "pn:        参数列表第n个参数(eg: p0, p1 注：参数索引是从0开始的){@link Object}";

    /**
     * pn_type:     参数列表第n个参数的参数类型(eg: p1_type, p2_type 注：参数索引是从0开始的){@link ResolvableType}
     */
    public static final String PN_TYPE = "pn_type:     参数列表第n个参数的参数类型(eg: p1_type, p2_type 注：参数索引是从0开始的){@link ResolvableType}";

    /**
     * paramName: 参数名称为paramName的参数{@link Object}
     */
    public static final String PARAM_NAME = "paramName: 参数名称为paramName的参数{@link Object}";

    //------------------------------------------------------------------------------------
    //                            Request Tags
    //------------------------------------------------------------------------------------

    /**
     * $req$:            当前响应对应的请求信息{@link Request}
     */
    public static final String REQUEST = "$req$:            当前响应对应的请求信息{@link Request}";

    /**
     * $url$:            当前请求的URL信息{@link String}
     */
    public static final String REQUEST_URL = "$url$:            当前请求的URL信息{@link String}";

    /**
     * $reqMethod$:      当前请求的类型{@link RequestMethod}
     */
    public static final String REQUEST_METHOD = "$reqMethod$:      当前请求的类型{@link RequestMethod}";

    /**
     * $query$:          当前请求的Query参数部分{@link Request#getSimpleQueries()}
     */
    public static final String REQUEST_QUERY = "$query$:          当前请求的Query参数部分{@link Request#getSimpleQueries()}";

    /**
     * $path$:           当前请求的路径参数部分{@link Request#getPathParameters()}
     */
    public static final String REQUEST_PATH = "$path$:           当前请求的路径参数部分{@link Request#getPathParameters()}";

    /**
     * $form$:           当前请求的表单参数部分{@link Request#getFormParameters()}
     */
    public static final String REQUEST_FORM = "$form$:           当前请求的表单参数部分{@link Request#getFormParameters()}";


    /**
     * $reqHeader$:      当前请求的请求头参数部分{@link Request#getSimpleHeaders()}
     */
    public static final String REQUEST_HEADER = "$reqHeader$:      当前请求的请求头参数部分{@link Request#getSimpleHeaders()}";

    /**
     * $reqCookie$:      当前请求的Cookie参数部分{@link Request#getSimpleCookies()}
     */
    public static final String REQUEST_COOKIE = "$reqCookie$:      当前请求的Cookie参数部分{@link Request#getSimpleCookies()}";


    //------------------------------------------------------------------------------------
    //                            Response Tags
    //------------------------------------------------------------------------------------

    /**
     * $resp$:           当前响应信息{@link Response}
     */
    public static final String RESPONSE = "$resp$:           当前响应信息{@link Response}";

    /**
     * $status$:         当前响应的状态码{@link Response#getStatus()}
     */
    public static final String RESPONSE_STATUS = "$status$:         当前响应的状态码{@link Response#getStatus()}";

    /**
     * $contentLength$:  当前响应的Content-Length{@link Response#getContentLength()}
     */
    public static final String CONTENT_LENGTH = "$contentLength$:  当前响应的Content-Length{@link Response#getContentLength()}";

    /**
     * $contentType$:    当前响应的Content-Type{@link Response#getContentType()}
     */
    public static final String CONTENT_TYPE = "$contentType$:    当前响应的Content-Type{@link Response#getContentType()}";

    /**
     * $respHeader$:     当前响应头信息{@link HttpHeaderManager#getSimpleHeaders()}
     */
    public static final String RESPONSE_HEADER = "$respHeader$:     当前响应头信息{@link HttpHeaderManager#getSimpleHeaders()}";

    /**
     * $respCookie$:     当前响应Cookie信息{@link Response#getSimpleCookies()}
     */
    public static final String RESPONSE_COOKIE = "$respCookie$:     当前响应Cookie信息{@link Response#getSimpleCookies()}";

    /**
     * $body$:           当前响应的响应体部分{@link Response#getEntity(Class)}
     */
    public static final String RESPONSE_BODY = "$body$:           当前响应的响应体部分{@link Response#getEntity(Class)}";

    //------------------------------------------------------------------------------------
    //                            Exception Tags
    //------------------------------------------------------------------------------------

    /**
     * $throwable$:     异常实例对象{@link Throwable}
     */
    public static final String THROWABLE = "$throwable$:     异常实例对象{@link Throwable}";
}
