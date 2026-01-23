package com.luckyframework.serializable;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * 序列化类型令牌
 * <pre>
 * {@code
 *      new SerializationTypeToken<Map<String, List<Integer>>>() {};
 *      new SerializationTypeToken<Map<String, List<Integer>>>() {}.getType();
 * }
 * </pre>
 *
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:35 下午
 */
public abstract class SerializationTypeToken<T> {

    /**
     * 内部的泛型类型
     */
    private final Type _type_;

    public SerializationTypeToken() {
        this._type_ = ResolvableType.forClass(SerializationTypeToken.class, getClass()).getGeneric(0).getType();
    }

    public Type getType() {
        return this._type_;
    }
}
