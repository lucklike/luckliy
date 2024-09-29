package com.luckyframework.spel;

import org.springframework.lang.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 支持懒加载的值对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/06/20 14:07
 */
public class LazyValue<V> {

    /**
     * 真实值
     */
    private V value;

    /**
     * 用于获取真实值的{@link Supplier}
     */
    private final Supplier<V> valueSupplier;

    /**
     * 只计算一次
     */
    private final boolean calculateOnce;

    /**
     * 是否已经初始化的标识
     */
    private final AtomicBoolean init = new AtomicBoolean(false);

    /**
     * 私有构造器
     *
     * @param valueSupplier 用于获取真实值的{@link Supplier}
     */
    private LazyValue(Supplier<V> valueSupplier, boolean calculateOnce) {
        this.valueSupplier = valueSupplier;
        this.calculateOnce = calculateOnce;
    }

    /**
     * 【只计算一次】静态方法，使用{@link Supplier}来构造一个{@link LazyValue}对象
     *
     * @param valueSupplier 用于获取真实值的{@link Supplier}
     * @param <V>           值类型
     * @return LazyValue对象
     */
    public static <V> LazyValue<V> of(@NonNull Supplier<V> valueSupplier) {
        return new LazyValue<>(valueSupplier, true);
    }

    /**
     * 【只计算一次】静态方法，使用真实值对象来构造一个{@link LazyValue}对象
     *
     * @param value 真实值对象
     * @param <V>   值类型
     * @return LazyValue对象
     */
    public static <V> LazyValue<V> of(V value) {
        LazyValue<V> vLazyValue = of(() -> value);
        vLazyValue.getValue();
        return vLazyValue;
    }

    /**
     *  rtc: real-time computing
     * 【实时计算】静态方法，使用真实值对象来构造一个{@link LazyValue}对象
     *
     * @param valueSupplier 用于获取真实值的{@link Supplier}
     * @param <V>           值类型
     * @return LazyValue对象
     */
    public static <V> LazyValue<V> rtc(@NonNull Supplier<V> valueSupplier) {
        return new LazyValue<>(valueSupplier, false);
    }

    /**
     *  rtc: real-time computing
     * 【实时计算】静态方法，使用真实值对象来构造一个{@link LazyValue}对象
     *
     * @param value 真实值对象
     * @param <V>   值类型
     * @return LazyValue对象
     */
    public static <V> LazyValue<V> rtc(V value) {
        return rtc(() -> value);
    }

    /**
     * 获取真实值对象
     * <pre>
     *    只计算一次的情况：
     *      1.如果已经初始化，则直接返回真实值对象
     *      2.没有初始化时，会使用{@link #valueSupplier}来获取真实值对象
     *    非只计算一次的情况：
     *      始终使用{@link #valueSupplier}来获取真实值对象返回
     * </pre>
     *
     * @return 真实值对象
     */
    public V getValue() {
        if (!calculateOnce) {
            return valueSupplier.get();
        }
        if (init.compareAndSet(false, true)) {
            value = valueSupplier.get();
        }
        return value;
    }

    /**
     * 是否只计算一次
     *
     * @return 是否只计算一次
     */
    public boolean isCalculateOnce() {
        return calculateOnce;
    }

    /**
     * 是否已经初始化
     *
     * @return 是否已经初始化
     */
    public boolean isInit() {
        return init.get();
    }

    @Override
    public String toString() {
        if (!calculateOnce) {
            return "[rtc] " + valueSupplier;
        }
        if (isInit()) {
            return "[ok] " + value;
        }
        return "[not init] " + valueSupplier;
    }
}
