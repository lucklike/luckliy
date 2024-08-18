package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * 响应扩展处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/18 20:17
 */
@FunctionalInterface
public interface ResponseConvertHandle<R, T> {

    /**
     * 响应扩展处理
     *
     * @param context  当前方法上下文
     * @param response 当前响应实例
     * @param config   扩展配置
     */
    R handle(MethodContext context, Response response, T config);

    /**
     * 获取配置类型
     *
     * @return 配置类型
     */
    default Type getType() {
        Class<?> thisClass = this.getClass();
        ResolvableType resolvableType = ResolvableType.forClass(ResponseConvertHandle.class, thisClass);
        return resolvableType.getGeneric(1).getType();
    }
}
