package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.common.FlatBean;
import com.luckyframework.spel.LazyValue;
import com.luckyframework.spel.SimpleSpelBean;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 包装类型枚举
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/06/27 13:18
 */
public class WrapType {

    /**
     * 收集外层Class与枚举实例的映射关系
     */
    private static final Map<Class<?>, Function<Supplier<?>, Object>> WRAP_TYPE_MAP;

    static {
        WRAP_TYPE_MAP = new ConcurrentHashMap<>();
        WRAP_TYPE_MAP.put(LazyValue.class, LazyValue::of);
        WRAP_TYPE_MAP.put(Supplier.class, s -> s);
        WRAP_TYPE_MAP.put(Optional.class, o -> Optional.ofNullable(o.get()));
        WRAP_TYPE_MAP.put(FlatBean.class, o -> FlatBean.of(o.get()));
        WRAP_TYPE_MAP.put(SpelBean.class, o -> SpelBean.ofIncomplete(o.get()));
        WRAP_TYPE_MAP.put(SimpleSpelBean.class, o -> SimpleSpelBean.of(o.get()));
    }

    /**
     * 外层Class
     */
    private final Class<?> resolve;

    /**
     * 将{@link Supplier}包装成外层对象实例的函数
     */
    private final Function<Supplier<?>, Object> wrapFunction;

    /**
     * 注册一个包装类型
     *
     * @param clazz        包装类型Class
     * @param wrapFunction 包装转换逻辑
     */
    public static void registerWrapType(Class<?> clazz, Function<Supplier<?>, Object> wrapFunction) {
        WRAP_TYPE_MAP.put(clazz, wrapFunction);
    }


    /**
     * 私有构造函数
     *
     * @param resolve         外层Class
     * @param convertFunction 包装函数
     */
    WrapType(Class<?> resolve, Function<Supplier<?>, Object> convertFunction) {
        this.resolve = resolve;
        this.wrapFunction = convertFunction;
    }

    /**
     * 静态构造方法，用于将某个Class转成对应的枚举实例
     *
     * @param resolve 外层Class
     * @return 对应的枚举实例
     */
    @NonNull
    public static WrapType of(Class<?> resolve) {
        if (resolve == null) {
            return new WrapType(Object.class, Supplier::get);
        }
        return new WrapType(resolve, WRAP_TYPE_MAP.getOrDefault(resolve, Supplier::get));
    }

    /**
     * 静态构造方法，用于将具体的{@link ResolvableType}转成对应的枚举实例
     *
     * @param type 类型
     * @return 对应的枚举实例
     */
    public static WrapType of(ResolvableType type) {
        return of(type.resolve());
    }


    /**
     * 获取外层Class
     *
     * @return 外层Class
     */
    public Class<?> getRawClass() {
        return resolve;
    }

    /**
     * 获取包装函数
     *
     * @return 包装函数
     */
    public Function<Supplier<?>, Object> getWrapFunction() {
        return wrapFunction;
    }

    /**
     * 获取将{@link Supplier}包装成外层对象实例的函数
     *
     * @param objectSupplier 对象获取方法
     * @return 对象包装类实例
     */
    public Object wrap(Supplier<?> objectSupplier) {
        return wrapFunction.apply(objectSupplier);
    }

    /**
     * 获取目标类型
     *
     * @param type 原类型
     * @return 目标类型
     */
    public ResolvableType getTargetType(ResolvableType type) {
        return WRAP_TYPE_MAP.containsKey(type.resolve()) ? type.getGeneric(0) : type;
    }
}
