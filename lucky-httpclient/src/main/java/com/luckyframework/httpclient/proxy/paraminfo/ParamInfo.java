package com.luckyframework.httpclient.proxy.paraminfo;

/**
 * 静态参数信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/22 00:33
 */
public class ParamInfo {

    private final Object name;
    private final Object value;
    public ParamInfo(Object name, Object value) {
        this.name = name;
        this.value = value;
    }

    public Object getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
