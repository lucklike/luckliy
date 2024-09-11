package com.luckyframework.common;

/**
 * Boolean值枚举，常用于注解属性
 */
public enum Bool {
    TRUE(true),
    FALSE(false),
    DEFAULT(null);

    private final Boolean value;

    Bool(Boolean value) {
        this.value = value;
    }

    public Boolean getValue() {
        return value;
    }

    public boolean getValue(boolean defaultValue) {
        return value == null ? defaultValue : value;
    }
}
