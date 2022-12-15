package com.luckyframework.environment;

import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.PropertyResolver;
import org.springframework.lang.Nullable;

/**
 * 对象属性解析器,是对{@link PropertyResolver}接口的扩展，
 * 该解析器支持将环境表变量中对属性解析成一个具体的对象
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/17 09:46
 */
 public interface ObjectPropertyResolver extends PropertyResolver {

    //----------------------------------------------------------------
    //              get property for type
    //----------------------------------------------------------------

   /**
    * 获取环境中的某个属性的值，并将其转化为指定的类型
    * @param property   属性对象
    * @param type       目标类型
    * @return           目标类型的对象
    * @param <T>        目标类型的泛型
    */
    @Nullable
    <T> T getPropertyForType(Object property, ResolvableType type);

    @Nullable
    default Object getPropertyForObject(Object property){
        return getPropertyForType(property, Object.class);
    }

    @Nullable
    default <T> T getPropertyForType(Object property, SerializationTypeToken<T> typeToken){
        return getPropertyForType(property, ResolvableType.forType(typeToken.getType()));
    }

    @Nullable
    default <T> T getPropertyForType(Object property, Class<T> type){
        return getPropertyForType(property, ResolvableType.forRawClass(type));
    }

    //----------------------------------------------------------------
    //              get require property for type
    //----------------------------------------------------------------

    @Nullable
    <T> T getRequiredPropertyForType(Object property, ResolvableType type);

    @Nullable
    default Object getRequiredPropertyForObject(Object property){
        return getRequiredPropertyForType(property, Object.class);
    }

    @Nullable
    default <T> T getRequiredPropertyForType(Object property, Class<T> type){
        return getRequiredPropertyForType(property, ResolvableType.forRawClass(type));
    }

    @Nullable
    default <T> T getRequiredPropertyForType(Object property, SerializationTypeToken<T> typeToken){
       return getRequiredPropertyForType(property, ResolvableType.forType(typeToken.getType()));
    }

    //----------------------------------------------------------------
    //              resolve placeholders for type
    //----------------------------------------------------------------

    @Nullable
    Object resolvePlaceholdersForObject(Object placeholderObj);

    @Nullable
    <T> T resolvePlaceholdersForType(Object placeholderObj, ResolvableType type);

    @Nullable
    default <T> T resolvePlaceholdersForType(Object placeholderObj, Class<T> type){
        return resolvePlaceholdersForType(placeholderObj, ResolvableType.forRawClass(type));
    }

    @Nullable
    default <T> T resolvePlaceholdersForType(Object placeholderObj, SerializationTypeToken<T> typeToken){
        return resolvePlaceholdersForType(placeholderObj, ResolvableType.forType(typeToken.getType()));
    }

    //----------------------------------------------------------------
    //              resolve required placeholders for type
    //----------------------------------------------------------------

    @Nullable
    Object resolveRequiredPlaceholdersForObject(Object placeholderObj) throws IllegalArgumentException;

    @Nullable
    <T> T resolveRequiredPlaceholdersForType(Object placeholderObj, ResolvableType type) throws IllegalArgumentException;

    @Nullable
    default <T> T resolveRequiredPlaceholdersForType(Object placeholderObj, Class<T> type) throws IllegalArgumentException{
        return resolveRequiredPlaceholdersForType(placeholderObj, ResolvableType.forRawClass(type));
    }

    @Nullable
    default <T> T resolveRequiredPlaceholdersForType(Object placeholderObj, SerializationTypeToken<T> typeToken) throws IllegalArgumentException{
        return resolveRequiredPlaceholdersForType(placeholderObj, ResolvableType.forType(typeToken.getType()));
    }
}
