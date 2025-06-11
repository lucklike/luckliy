package com.luckyframework.httpclient.proxy.spel;

import java.util.Set;

/**
 * 内部参数名
 */
public class InternalRootVarName {

    public static final String $_THIS_$ = "$this$";
    public static final String $_HTTP_PROXY_FACTORY_$ = "$httpProxyFactory$";
    public static final String $_THROWABLE_$ = "$throwable$";

    public static final String $_EXE_TIME_$ = "$exeTime$";
    public static final String $_METHOD_CONTEXT_$ = "$mc$";
    public static final String $_METHOD_META_CONTEXT_$ = "$mec$";
    public static final String $_METHOD_ARGS_$ = "$args$";
    public static final String $_UNIQUE_ID_$ = "$unique_id$";
    public static final String $_VALUE_CONTEXT_$ = "$vc$";
    public static final String _PARAM_CONTEXT_INDEX_ = "_index_";
    public static final String _VALUE_CONTEXT_VALUE_ = "_value_";
    public static final String _$VALUE_CONTEXT_SOURCE_VALUE$_ = "_$value$_";
    public static final String _VALUE_CONTEXT_NAME_ = "_name_";
    public static final String _VALUE_CONTEXT_TYPE_ = "_type_";


    public static final String $_METHOD_$ = "$method$";
    public static final String $_METHOD_RETURN_TYPE_$ = "$mrt$";
    public static final String $_METHOD_REAL_RETURN_TYPE_$ = "$mrrt$";
    public static final String $_METHOD_PARAM_TYPES_$ = "$mpts$";
    public static final String $_METHOD_PARAM_NAMES_$ = "$pmns$";
    public static final String $_CLASS_$ = "$class$";

    public static final String $_CLASS_CONTEXT_$ = "$cc$";

    public static final String $_REQUEST_$ = "$req$";
    public static final String $_RESPONSE_$ = "$resp$";
    public static final String $_RESPONSE_STREAM_BODY_$ = "$streamBody$";
    public static final String $_RESPONSE_BYTE_BODY_$ = "$byteBody$";
    public static final String $_RESPONSE_STRING_BODY_$ = "$stringBody$";
    public static final String $_RESPONSE_BODY_$ = "$body$";
    public static final String $_RESPONSE_STATUS_$ = "$status$";
    public static final String $_CONTENT_TYPE_$ = "$contentType$";
    public static final String $_CONTENT_LENGTH_$ = "$contentLength$";
    public static final String $_RESPONSE_HEADER_$ = "$respHeader$";
    public static final String $_RESPONSE_COOKIE_$ = "$respCookie$";
    public static final String $_REQUEST_REDIRECT_URL_CHAIN_$ = "$redirectChain$";

    public static final String $_REQUEST_METHOD_$ = "$reqMethod$";
    public static final String $_REQUEST_URL_$ = "$url$";
    public static final String $_REQUEST_URL_PATH_$ = "$urlPath$";
    public static final String $_REQUEST_QUERY_$ = "$query$";
    public static final String $_REQUEST_PATH_$ = "$path$";
    public static final String $_REQUEST_HEADER_$ = "$reqHeader$";
    public static final String $_REQUEST_COOKIE_$ = "$reqCookie$";
    public static final String $_REQUEST_FORM_$ = "$form$";


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static Set<String> getAllInternalRootVarName() {
        return InternalUtils.getInternalVarName(InternalRootVarName.class);
    }
}
