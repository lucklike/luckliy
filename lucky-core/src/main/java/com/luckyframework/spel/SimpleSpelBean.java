package com.luckyframework.spel;

import com.luckyframework.common.ExpressionBean;
import com.luckyframework.common.FlatBean;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Properties;

/**
 * 支持使用 SpEL 表达式操作的 Bean 对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/1 02:32
 */
public class SimpleSpelBean<T> implements ExpressionBean<T> {

    private final SpELRuntime spELRuntime;
    private final T bean;

    private SimpleSpelBean(SpELRuntime spELRuntime, T bean) {
        this.spELRuntime = spELRuntime;
        this.bean = bean;
    }

    public static <T> SimpleSpelBean<T> of(SpELRuntime spELRuntime, T bean) {
        return new SimpleSpelBean<>(spELRuntime, bean);
    }

    public static <T> SimpleSpelBean<T> of(T bean) {
        return of(new SpELRuntime(), bean);
    }

    public static SimpleSpelBean<?> forProperties(SpELRuntime spELRuntime, Properties properties) {
        return  SimpleSpelBean.of(spELRuntime, FlatBean.forProperties(properties).getBean());
    }

    public static SimpleSpelBean<?> forProperties(Properties properties) {
        return forProperties(new SpELRuntime(), properties);
    }

    @Override
    public T getBean() {
        return bean;
    }

    @Override
    public void set(String expression, Object value) {
        spELRuntime.setValue(bean, expression, value);
    }

    @Override
    public <V> V get(String expression, Type type) {
        return spELRuntime.getValueForType(new ParamWrapper(expression).setRootObject(bean).setExpectedResultType(type));
    }

    @Override
    public <R> SimpleSpelBean<R> to(Type type) {
        return SimpleSpelBean.of(spELRuntime, beanConvert(type));
    }

    @Override
    public <R> SimpleSpelBean<R> to(ResolvableType type) {
        return to(type.getType());
    }

    @Override
    public <R> SimpleSpelBean<R> to(SerializationTypeToken<R> typeToken) {
        return to(typeToken.getType());
    }

    @Override
    public <R> SimpleSpelBean<R> to(Class<R> type) {
        return to((Type) type);
    }

    public <V> SimpleSpelBean<V> getSimpleSpelBean(String expression, Type type) {
        return SimpleSpelBean.of(spELRuntime, get(expression, type));
    }

    public <V> SimpleSpelBean<V> getSimpleSpelBean(String expression, Class<V> type) {
        return getSimpleSpelBean(expression, (Type) type);
    }

    public <V> SimpleSpelBean<V> getSimpleSpelBean(String expression, SerializationTypeToken<V> type) {
        return getSimpleSpelBean(expression, type.getType());
    }

    public <V> SimpleSpelBean<V> getSimpleSpelBean(String expression, ResolvableType type) {
        return getSimpleSpelBean(expression, type.getType());
    }

    public SimpleSpelBean<?> getSimpleSpelBean(String expression) {
        return getSimpleSpelBean(expression, Object.class);
    }
}
