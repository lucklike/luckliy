package com.luckyframework.httpclient.proxy;

/**
 * 参数名常量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/9 17:05
 */
public class ParameterNameConstant {

    private ParameterNameConstant(){}

    public static final String SPRING_EL_NAME = "$elEnv$";
    public static final String METHOD_CONTEXT = "$mc$";
    public static final String CLASS_CONTEXT = "$cc$";
    public static final String METHOD = "$method$";

    public static final String CLASS = "$class$";
    public static final String REQUEST = "$req$";
    public static final String RESPONSE = "$resp$";
    public static final String RESPONSE_DATA = "$data$";
    public static final String RESPONSE_STATUS = "$status$";
    public static final String RESPONSE_HEADERS = "$headers$";
    public static final String ANNOTATION_INSTANCE = "$ann$";

    public static final String REQUEST_TYPE = "$type$";
    public static final String REQUEST_URL = "$url$";
    public static final String REQUEST_QUERY = "$query$";
    public static final String REQUEST_PATH = "$path$";
    public static final String REQUEST_HEADER = "$headers$";
    public static final String REQUEST_FORM = "$form$";
}