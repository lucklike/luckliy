package com.luckyframework.httpclient.proxy.spel;

import java.util.ArrayList;
import java.util.List;

public class MethodSpaceConstant {

    /**
     * 公共函数空间
     */
    public final static String COMMON_FUNCTION_SPACE = "__Fun::Common::__";

    public static final List<String> EXTERNAL_SPACES = new ArrayList<>();

    public static void addExternalSpace(String externalSpace) {
        EXTERNAL_SPACES.add(externalSpace);
    }

    public static List<String> getSpaces() {
        List<String> internalVarName = InternalUtils.getInternalVarName(MethodSpaceConstant.class);
        internalVarName.addAll(EXTERNAL_SPACES);
        return internalVarName;
    }


}
