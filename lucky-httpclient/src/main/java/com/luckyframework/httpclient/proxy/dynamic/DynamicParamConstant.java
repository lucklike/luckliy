package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.setter.ParameterSetter;
import com.luckyframework.httpclient.proxy.setter.QueryParameterSetter;
import com.luckyframework.httpclient.proxy.setter.StandardBodyParameterSetter;

import java.util.function.Function;

/**
 * 动态参数解析器、设置器相关的常量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/13 16:57
 */
public class DynamicParamConstant {

    //------------------------------------------------------------------------------------------------------
    //                                      DynamicParamResolver
    //------------------------------------------------------------------------------------------------------

    public static final DynamicParamResolver RETURN_ORIGINAL_RESOLVER = new ReturnOriginalDynamicParamResolver();
//    public static final DynamicParamResolver STANDARD_BINARY_RESOLVER = new StandardBinaryBodyDynamicParamResolver();
//    public static final DynamicParamResolver STANDARD_HTTP_FILE_RESOLVER = new StandardHttpFileDynamicParamResolver();
    public static final DynamicParamResolver LOOK_UP_SPECIAL_ANNOTATION_RESOLVER = new LookUpSpecialAnnotationDynamicParamResolver();


    //------------------------------------------------------------------------------------------------------
    //                                      ParameterSetter
    //------------------------------------------------------------------------------------------------------

    public static final ParameterSetter QUERY_SETTER = new QueryParameterSetter();
//    public static final ParameterSetter STANDARD_HTTP_FILE_SETTER = new StandardHttpFileParameterSetter();
    public static final ParameterSetter STANDARD_BODY_SETTER = new StandardBodyParameterSetter();


    //------------------------------------------------------------------------------------------------------
    //                               DynamicParamResolver-Function
    //------------------------------------------------------------------------------------------------------

    public static final Function<Context, DynamicParamResolver> RETURN_ORIGINAL_RESOLVER_FUNCTION = MC -> RETURN_ORIGINAL_RESOLVER;
//    public static final Function<Context, DynamicParamResolver> STANDARD_BINARY_RESOLVER_FUNCTION = MC -> STANDARD_BINARY_RESOLVER;
//    public static final Function<Context, DynamicParamResolver> STANDARD_HTTP_FILE_RESOLVER_FUNCTION = MC -> STANDARD_HTTP_FILE_RESOLVER;
    public static final Function<Context, DynamicParamResolver> LOOK_UP_SPECIAL_ANNOTATION_RESOLVER_FUNCTION = MC -> LOOK_UP_SPECIAL_ANNOTATION_RESOLVER;


    //------------------------------------------------------------------------------------------------------
    //                                  ParameterSetter-Function
    //------------------------------------------------------------------------------------------------------

    public static final Function<Context, ParameterSetter> QUERY_SETTER_FUNCTION = MC -> QUERY_SETTER;
//    public static final Function<Context, ParameterSetter> STANDARD_HTTP_FILE_SETTER_FUNCTION = MC -> STANDARD_HTTP_FILE_SETTER;
    public static final Function<Context, ParameterSetter> STANDARD_BODY_SETTER_FUNCTION = MC -> STANDARD_BODY_SETTER;
}
