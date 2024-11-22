package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * 内部参数名
 */
public class InternalParamName {

    public static final String $_THIS_$ = "$this$";
    public static final String $_THROWABLE_$ = "$throwable$";

    public static final String $_EXE_TIME_$ = "$exeTime$";
    public static final String $_METHOD_CONTEXT_$ = "$mc$";
    public static final String $_VALUE_CONTEXT_$ = "$vc$";
    public static final String _PARAM_CONTEXT_INDEX_ = "_index_";
    public static final String _VALUE_CONTEXT_VALUE_ = "_value_";
    public static final String _$VALUE_CONTEXT_SOURCE_VALUE$_ = "_$value$_";
    public static final String _VALUE_CONTEXT_NAME_ = "_name_";
    public static final String _VALUE_CONTEXT_TYPE_ = "_type_";


    public static final String $_METHOD_$ = "$method$";
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

    public static final String __$REQ_DEFAULT$__ = "__$default$__";
    public static final String __$REQ_SSE$__ = "__$sse$__";
    public static final String __$LISTENER_VAR$__ = "__$eventListener$__";


    public static final String __$ASYNC_TAG$__ = "__$async$__";
    public static final String __$ASYNC_EXECUTOR$__ = "__$asyncExecutor$__";
    public static final String __$HTTP_EXECUTOR$__ = "__$httpExecutor$__";

    public static final String __$RETRY_SWITCH$__ = "__$retrySwitch$__";
    public static final String __$RETRY_TASK_NAME$__ = "__$retryTaskName$__";
    public static final String __$RETRY_COUNT$__ = "__$retryCount$__";
    public static final String __$RETRY_RUN_BEFORE_RETRY_FUNCTION$__ = "__$retryRunBeforeRetryContextFunction$__";
    public static final String __$RETRY_DECIDER_FUNCTION$__ = "__$retryRetryDeciderContextFunction$__";

    public static final String __$MOCK_RESPONSE_FACTORY$__ = "__$mockResponseFactory$__";


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static Set<String> getAllInternalParamName() {
        // 内部变量名
        Set<String> internalParamNameSet = new HashSet<>();
        Field[] fields = ClassUtils.getAllFields(InternalParamName.class);
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object value = FieldUtils.getValue(null, field);
            if (value instanceof String) {
                internalParamNameSet.add((String) value);
            }
        }
        return internalParamNameSet;
    }
}
