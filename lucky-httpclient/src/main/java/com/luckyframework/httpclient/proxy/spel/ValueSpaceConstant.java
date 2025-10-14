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
    public static final String CLASS_CONTENT_SPACE = "__::Var::ClassContent::__";

    /**
     * MethodMetaContext相关的变量空间
     */
    public static final String METHOD_META_CONTEXT_SPACE = "__::Var::MethodMetaContext::__";

    /**
     * MethodContext相关的变量空间
     */
    public static final String METHOD_CONTEXT_SPACE = "__::Var::MethodContext::__";

    /**
     * 方法参数相关的变量空间
     */
    public static final String METHOD_CONTEXT_ARGS_SPACE = "__::Var::MethodArgs::__";

    /**
     * ValueContext相关的变量空间
     */
    public static final String VALUE_CONTENT_SPACE = "__::Var::ValueContext::__";

    /**
     * ParameterContext相关的变量空间
     */
    public static final String PARAM_CONTEXT_SPACE = "__::Var::ParameterContext::__";

    /**
     * Request相关的变量空间
     */
    public static final String REQUEST_SPACE = "__::Var::Request::__";

    /**
     * Response相关的变量空间
     */
    public static final String RESPONSE_SPACE = "__::Var::Response::__";

    /**
     * 拦截器相关的变量空间
     */
    public static final String INTERCEPTOR_SPACE = "__::Var::Interceptor::__";

    /**
     * API描述信息相关的变量空间
     */
    public static final String API_DESC_SPACE = "__::Var::APIDescribe::__";

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
