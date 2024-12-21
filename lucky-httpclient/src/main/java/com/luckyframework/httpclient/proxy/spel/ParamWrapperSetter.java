package com.luckyframework.httpclient.proxy.spel;

@FunctionalInterface
public interface ParamWrapperSetter {

    /**
     * 参数设置，可以通过此接口来加入更多的扩展参数
     *
     * @param paramWrapper 现有参数包装对象
     */
    void setting(MutableMapParamWrapper paramWrapper);
}
