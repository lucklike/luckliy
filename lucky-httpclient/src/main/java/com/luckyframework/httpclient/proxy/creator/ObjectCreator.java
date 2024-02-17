package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.context.Context;

import java.util.function.Consumer;

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
     * 用于创建一个对象实例，并对其实例对象进行一些操作
     *
     * @param clazz    实例Class
     * @param msg      对象的创建信息
     * @param context  上下文对象
     * @param scope    对象的作用域
     * @param consumer 实例对象消费者
     * @param <T>      对象实例类型
     * @return 对象实例
     */
    <T> T newObject(Class<T> clazz, String msg, Context context, Scope scope, Consumer<T> consumer);

    /**
     * 用于创建一个对象实例
     *
     * @param clazz   实例Class
     * @param msg     对象的创建信息
     * @param context 上下文对象
     * @param scope   对象的作用域
     * @param <T>      对象实例类型
     * @return 对象实例
     */
    default <T> T newObject(Class<T> clazz, String msg, Context context, Scope scope) {
        return newObject(clazz, msg, context, scope, t -> {
        });
    }

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
