package com.luckyframework.httpclient.proxy.spel;

import org.springframework.util.Assert;

/**
 * 内部变量枚举
 */
public enum ProhibitCoverEnum implements ProhibitCoverParamMark {

    /**
     * 变量名以'$'开头的变量
     */
    $("$", ""),

    /**
     * 变量名以'_'开头以'_'结尾的变量
     */
    __("_", "_"),

    /**
     * 变量名以'__'开头以'__'结尾的变量
     */
    ____("__", "__");

    private final String prefix;
    private final String suffix;

    ProhibitCoverEnum(String prefix, String suffix) {
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

    public static boolean isMatch(String param) {
        for (ProhibitCoverEnum value : values()) {
            if (value.match(param)) {
                return true;
            }
        }
        return false;
    }
}
