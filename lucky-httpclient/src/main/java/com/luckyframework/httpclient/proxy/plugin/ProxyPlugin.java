package com.luckyframework.httpclient.proxy.plugin;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 代理插件
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/28 00:44
 */
@FunctionalInterface
public interface ProxyPlugin {

    /**
     * 初始化标志
     */
    AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 以插件的方式运行当前正在执行的方法<br/>
     * 示例：
     * <pre>
     * {@code
     *      Object decorate(ProxyDecorator decorator) throws Throwable {
     *          System.out.println("目标方法执行前执行");
     *          // 执行目标方法
     *          decorator.proceed();
     *          System.out.println("目标方法执行后执行");
     *      }
     * }
     * </pre>
     *
     * @param decorator 代理装饰器
     * @return 执行结果
     * @throws Throwable 执行过程中可能产生的异常
     */
    Object decorate(ProxyDecorator decorator) throws Throwable;

    /**
     * 初始化方法
     *
     * @param meta 执行元数据
     */
    default void init(ExecuteMeta meta) {
        if (initialized.compareAndSet(false, true)) {
            initOneOnly(meta);
        }
    }

    /**
     * 只执行一次的初始化方法
     *
     * @param meta 执行元数据
     */
    default void initOneOnly(ExecuteMeta meta) {

    }

    /**
     * 匹配逻辑，满足该条件之后插件逻辑才会执行
     *
     * @param meta 执行元数据
     * @return 匹配结果
     */
    default boolean match(ExecuteMeta meta) {
        return true;
    }

    /**
     * 获取插件的唯一标识
     *
     * @return 插件的唯一标识
     */
    default String uniqueIdentification() {
        return getClass().getName();
    }
}
