package com.luckyframework.httpclient.proxy.spel;

import java.util.Set;

/**
 * 内部参数名
 */
public class InternalVarName {

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

    public static final String __$IS_MOCK$__ = "__$isMock$__";
    public static final String __$MOCK_RESPONSE_FACTORY$__ = "__$mockResponseFactory$__";

    public static final String __$CONVERT_META_TYP$__ = "__$convertMetaTyp$__";


    /**
     * 获取所有内部变量名称
     *
     * @return 所有内部变量名称
     */
    public static Set<String> getAllInternalVarName() {
        return InternalUtils.getInternalVarName(InternalVarName.class);
    }
}
