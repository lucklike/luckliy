package com.luckyframework.httpclient.proxy.spel;

import java.util.ArrayList;
import java.util.List;

/**
 * 变量空间
 */
public class ValueSpaceConstant {

    /**
     * ClassContent相关的变量空间
     */
    public static final String CLASS_CONTENT_SPACE = "__::Val::ClassContent::__";

    /**
     * MethodMetaContext相关的变量空间
     */
    public static final String METHOD_META_CONTEXT_SPACE = "__::Val::MethodMetaContext::__";

    /**
     * MethodContext相关的变量空间
     */
    public static final String METHOD_CONTEXT_SPACE = "__::Val::MethodContext::__";

    /**
     * 方法参数相关的变量空间
     */
    public static final String METHOD_CONTEXT_ARGS_SPACE = "__::Val::MethodArgs::__";

    /**
     * ValueContext相关的变量空间
     */
    public static final String VALUE_CONTENT_SPACE = "__::Val::ValueContext::__";

    /**
     * ParameterContext相关的变量空间
     */
    public static final String PARAM_CONTEXT_SPACE = "__::Val::ParameterContext::__";

    /**
     * Request相关的变量空间
     */
    public static final String REQUEST_SPACE = "__::Val::Request::__";

    /**
     * Response相关的变量空间
     */
    public static final String RESPONSE_SPACE = "__::Val::Response::__";

    /**
     * 拦截器相关的变量空间
     */
    public static final String INTERCEPTOR_SPACE = "__::Val::Interceptor::__";

    /**
     * API描述信息相关的变量空间
     */
    public static final String API_DESC_SPACE = "__::Val::APIDescribe::__";

    /**
     * 外部变量空间
     */
    public static final List<String> EXTERNAL_SPACES = new ArrayList<>();

    /**
     * 添加一个外部空间
     *
     * @param externalSpace 外部空间
     */
    public static void addExternalSpace(String externalSpace) {
        EXTERNAL_SPACES.add(externalSpace);
    }

    /**
     * 返回所有的变量空间
     *
     * @return 所有的变量空间
     */
    public static List<String> getSpaces() {
        List<String> internalVarName = InternalUtils.getInternalVarName(ValueSpaceConstant.class);
        internalVarName.addAll(EXTERNAL_SPACES);
        return internalVarName;
    }
}
