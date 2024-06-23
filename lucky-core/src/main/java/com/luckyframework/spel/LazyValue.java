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

    private V value;
    private final Supplier<V> valueSupplier;
    private final AtomicBoolean init = new AtomicBoolean(false);

    private LazyValue(Supplier<V> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }

    public static <V> LazyValue<V> of(@NonNull Supplier<V> valueSupplier) {
        return new LazyValue<>(valueSupplier);
    }

    public static <V> LazyValue<V> of(V value) {
        return of(() -> value);
    }

    public V getValue() {
        if (init.compareAndSet(false, true)) {
            value = valueSupplier.get();
        }
        return value;
    }

    @Override
    public String toString() {
        if (init.get()) {
            return "[ok] " + value;
        }
        return "[not init] " + valueSupplier;
    }
}
