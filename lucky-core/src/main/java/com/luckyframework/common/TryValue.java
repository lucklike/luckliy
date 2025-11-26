package com.luckyframework.common;

/**
 * 尝试值
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/27 01:48
 */
public class TryValue<T> {
    private final boolean exist;
    private final T value;

    private TryValue(boolean exist, T value) {
        this.exist = exist;
        this.value = value;
    }

    public static <T> TryValue<T> of(boolean exist, T value) {
        return new TryValue<T>(exist, value);
    }

    public boolean isExist() {
        return exist;
    }

    public boolean isNull() {
        return value == null;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TryValue{" +
                "exist=" + exist +
                ", value=" + value +
                '}';
    }
}
