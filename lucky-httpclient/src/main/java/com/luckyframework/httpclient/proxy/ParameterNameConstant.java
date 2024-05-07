package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;

/**
 * 参数名常量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/9 17:05
 */
public class ParameterNameConstant {

    private ParameterNameConstant(){}

    public static final String SET_TAG = "[SET]";
    public static final String REMOVE_TAG = "[-]";

    public static final String THIS = "$this$";
    public static final String THROWABLE = "$throwable$";

    public static final String SPRING_EL_ENV = "$val$";
    public static final String ANNOTATION_CONTEXT = "$ac$";
    public static final String METHOD_CONTEXT = "$mc$";
    public static final String VALUE_CONTEXT = "$vc$";
    public static final String PARAM_CONTEXT_INDEX = "_index_";
    public static final String VALUE_CONTEXT_VALUE = "_value_";
    public static final String VALUE_CONTEXT_NAME = "_name_";
    public static final String VALUE_CONTEXT_TYPE = "_type_";
    public static final String CLASS_CONTEXT = "$cc$";
    public static final String CONTEXT = "$context$";
    public static final String CONTEXT_ANNOTATED_ELEMENT = "$contextAnnotatedElement$";

    public static final String METHOD = "$method$";
    public static final String CLASS = "$class$";

    public static final String REQUEST = "$req$";
    public static final String RESPONSE = "$resp$";
    public static final String VOID_RESPONSE = "$voidResp$";
    public static final String RESPONSE_BODY = "$body$";
    public static final String RESPONSE_STATUS = "$status$";
    public static final String CONTENT_TYPE = "$contentType$";
    public static final String CONTENT_LENGTH = "$contentLength$";
    public static final String RESPONSE_HEADER = "$respHeader$";
    public static final String RESPONSE_COOKIE = "$respCookie$";
    public static final String ANNOTATION_INSTANCE = "$ann$";

    public static final String REQUEST_METHOD = "$reqMethod$";
    public static final String REQUEST_URL = "$url$";
    public static final String REQUEST_QUERY = "$query$";
    public static final String REQUEST_PATH = "$path$";
    public static final String REQUEST_HEADER = "$reqHeader$";
    public static final String REQUEST_COOKIE = "$reqCookie$";
    public static final String REQUEST_FORM = "$form$";

    public static final String LUCKY_VERSION = "2.1.0";
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String LUCKY_USER_AGENT = StringUtils.format("Lucky-HttpClient/{} (Java/{})", LUCKY_VERSION, JAVA_VERSION);
}
