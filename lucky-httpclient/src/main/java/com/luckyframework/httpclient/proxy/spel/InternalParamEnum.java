package com.luckyframework.httpclient.proxy.spel;

import org.springframework.util.Assert;

/**
 * 内部变量枚举
 */
public enum InternalParamEnum implements InternalParamMark {

    /**
     * 变量名以'$'开头的变量
     */
    $("$", ""),

    /**
     * 变量名以'__$'开头以'$__'结尾的变量
     */
    __$$__("__$", "$__"),

    /**
     * 变量名以'$'开头以'$'结尾的变量
     */
    $$("$", "$");


    private final String prefix;
    private final String suffix;

    InternalParamEnum(String prefix, String suffix) {
        Assert.notNull(prefix, "prefix must not be null");
        Assert.notNull(suffix, "suffix must not be null");
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    public static boolean isInternalParam(String param) {
        for (InternalParamEnum value : values()) {
            if (value.internalParam(param)) {
                return true;
            }
        }
        return false;
    }
}
