package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.spel.hook.callback.Var;
import org.springframework.lang.Nullable;

/**
 * 参数转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/15 00:47
 */
public interface ParameterConvert {

    /**
     * 是否可以转换
     *
     * @param context 值上下文
     * @param value   参数值
     * @return 是否可以转换
     */
    boolean canConvert(ValueContext context, @Nullable Object value);

    /**
     * 转换逻辑
     *
     * @param context 值上下文
     * @param value   参数值
     * @return 转换后的参数值
     */
    Object convert(ValueContext context, @Nullable Object value);
}
