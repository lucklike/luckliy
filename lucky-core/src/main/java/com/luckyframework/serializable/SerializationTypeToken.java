package com.luckyframework.serializable;

import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/2 7:35 下午
 */
public interface SerializationTypeToken<T> {

    default Type getType(){
        Class<?> thisClass = this.getClass();
        ResolvableType resolvableType = ResolvableType.forClass(SerializationTypeToken.class, thisClass);
        return resolvableType.getGeneric(0).getType();
    }
}
