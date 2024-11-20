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

    public static final String THIS = "$this$";
    public static final String THROWABLE = "$throwable$";

    public static final String ANNOTATION_CONTEXT = "$ac$";
    public static final String METHOD_CONTEXT = "$mc$";
    public static final String VALUE_CONTEXT = "$vc$";
    public static final String PARAM_CONTEXT_INDEX = "_index_";
    public static final String VALUE_CONTEXT_VALUE = "_value_";
    public static final String VALUE_CONTEXT_SOURCE_VALUE = "_$value_";
    public static final String VALUE_CONTEXT_NAME = "_name_";
    public static final String VALUE_CONTEXT_TYPE = "_type_";
    public static final String CLASS_CONTEXT = "$cc$";
    public static final String CONTEXT = "$context$";
    public static final String CONTEXT_ANNOTATED_ELEMENT = "$contextAnnotatedElement$";

    public static final String METHOD = "$method$";
    public static final String CLASS = "$class$";

    public static final String REQUEST = "$req$";
    public static final String RESPONSE = "$resp$";
    public static final String RESPONSE_STREAM_BODY = "$streamBody$";
    public static final String RESPONSE_BYTE_BODY = "$byteBody$";
    public static final String RESPONSE_STRING_BODY = "$stringBody$";
    public static final String RESPONSE_BODY = "$body$";
    public static final String RESPONSE_STATUS = "$status$";
    public static final String CONTENT_TYPE = "$contentType$";
    public static final String CONTENT_LENGTH = "$contentLength$";
    public static final String RESPONSE_HEADER = "$respHeader$";
    public static final String RESPONSE_COOKIE = "$respCookie$";
    public static final String REQUEST_REDIRECT_URL_CHAIN = "$redirectChain$";
    public static final String ANNOTATION_INSTANCE = "$ann$";

    public static final String REQUEST_METHOD = "$reqMethod$";
    public static final String REQUEST_URL = "$url$";
    public static final String REQUEST_URL_PATH = "$urlPath$";
    public static final String REQUEST_QUERY = "$query$";
    public static final String REQUEST_PATH = "$path$";
    public static final String REQUEST_HEADER = "$reqHeader$";
    public static final String REQUEST_COOKIE = "$reqCookie$";
    public static final String REQUEST_FORM = "$form$";

    public static final String REQ_DEFAULT = "default";
    public static final String REQ_SSE = "sse";
    public static final String LISTENER_VAR = "__$eventListener$__";


    public static final String ASYNC_TAG = "__$async$__";
    public static final String ASYNC_EXECUTOR = "__$asyncExecutor$__";
    public static final String HTTP_EXECUTOR = "__$httpExecutor$__";

    public static final String RETRY_SWITCH = "__$retrySwitch$__";
    public static final String RETRY_TASK_NAME = "__$retryTaskName$__";
    public static final String RETRY_COUNT = "__$retryCount$__";
    public static final String RETRY_RUN_BEFORE_RETRY_FUNCTION = "__$retryRunBeforeRetryContextFunction$__";
    public static final String RETRY_DECIDER_FUNCTION = "__$retryRetryDeciderContextFunction$__";

    public static final String MOCK_RESPONSE_FACTORY = "__$mockResponseFactory$__";


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static Set<String> getAllInternalParamName() {
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
