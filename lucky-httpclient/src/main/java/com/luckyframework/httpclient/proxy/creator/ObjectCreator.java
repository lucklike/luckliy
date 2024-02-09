package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.context.Context;

/**
 * 对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 02:34
 */
@FunctionalInterface
public interface ObjectCreator {

    /**
     * 用于创建一个对象实例
     *
     * @param clazz   实例Class
     * @param msg     对象的创建信息
     * @param context 上下文对象
     * @param scope   对象的作用域
     * @return 对象实例
     */
    Object newObject(Class<?> clazz, String msg, Context context, Scope scope);

    /**
     * 使用{@link ObjectGenerate @ObjectGenerate}实例和上下文{@link Context}对象构建一个对象
     *
     * @param generate 对象生成器注解实例
     * @param context  上下文对账
     * @return ObjectGenerate中指定类的对象实例
     */
    default Object newObject(ObjectGenerate generate, Context context) {
        return newObject(generate.clazz(), generate.msg(), context, generate.scope());
    }
}
