package com.luckyframework.httpclient.proxy.spel;

import java.util.ArrayList;
import java.util.List;

public class ValueSpaceConstant {

    public static final String CLASS_CONTENT_SPACE = "__::Var::ClassContent::__";

    public static final String METHOD_META_CONTEXT_SPACE = "__::Var::MethodMetaContext::__";

    public static final String METHOD_CONTEXT_SPACE = "__::Var::MethodContext::__";

    public static final String METHOD_CONTEXT_ARGS_SPACE = "__::Var::MethodArgs::__";

    public static final String VALUE_CONTENT_SPACE = "__::Var::ValueContext::__";

    public static final String PARAM_CONTEXT_SPACE = "__::Var::ParameterContext::__";

    public static final String REQUEST_SPACE = "__::Var::Request::__";

    public static final String RESPONSE_SPACE = "__::Var::Response::__";

    public static final String INTERCEPTOR_SPACE = "__::Var::Interceptor::__";

    public static final String API_DESC_SPACE = "__::Var::APIDescribe::__";


    public static final List<String> EXTERNAL_SPACES = new ArrayList<>();

    public static void addExternalSpace(String externalSpace) {
        EXTERNAL_SPACES.add(externalSpace);
    }

    public static List<String> getSpaces() {
        List<String> internalVarName = InternalUtils.getInternalVarName(ValueSpaceConstant.class);
        internalVarName.addAll(EXTERNAL_SPACES);
        return internalVarName;
    }
}
