package com.luckyframework.httpclient.proxy.spel;


import com.luckyframework.common.ExpressionBean;
import com.luckyframework.common.FlatBean;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.ContextAware;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.serializable.SerializationTypeToken;
import com.luckyframework.spel.ParamWrapper;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Properties;

/**
 * 支持使用 SpEL 表达式操作的 Bean 对象
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/11/30 17:37
 */
public class SpelBean<T> implements ContextAware, ExpressionBean<T> {

    /**
     * 方法上下文
     */
    private Context context;

    /**
     * Bean 对象
     */
    private final T bean;

    /**
     * 私有构造函数
     *
     * @param context 方法上下文
     * @param bean    Bean对象
     */
    private SpelBean(Context context, T bean) {
        this.context = context;
        this.bean = bean;
    }

    /**
     * 获取一个不完整的{@link SpelBean}实例，该示例不可直接使用，后续调用{@link #setContext(Context)}方法之后才可以正常使用
     *
     * @param bean Bean对象
     * @param <T>  Bean类型
     * @return {@link SpelBean}实例
     */
    public static <T> SpelBean<T> ofIncomplete(T bean) {
        return new SpelBean<>(null, bean);
    }

    /**
     * 获取一个{@link SpelBean}实例
     *
     * @param context 方法上下文
     * @param bean    Bean对象
     * @param <T>     Bean类型
     * @return {@link SpelBean}实例
     */
    public static <T> SpelBean<T> of(Context context, T bean) {
        return new SpelBean<>(context, bean);
    }

    private static SpelBean<?> forProperties(MethodContext context, Properties properties) {
        return of(context, FlatBean.forProperties(properties).getBean());
    }

    @Override
    public <R> SpelBean<R> to(Type type) {
        return SpelBean.of(context, beanConvert(type));
    }

    @Override
    public <R> SpelBean<R> to(Class<R> type) {
        return to((Type) type);
    }

    @Override
    public <R> SpelBean<R> to(SerializationTypeToken<R> typeToken) {
        return to(typeToken.getType());
    }

    @Override
    public <R> SpelBean<R> to(ResolvableType type) {
        return to(type.getType());
    }

    /**
     * 获取原始Bean对象
     *
     * @return 原始Bean对象
     */
    @Override
    public T getBean() {
        return bean;
    }

    @Override
    public void set(String expression, Object value) {
        context.getSpELConverter().getSpELRuntime().setValue(bean, expression, value);
    }

    @Override
    public <V> V get(String expression, Type type) {
        ParamWrapper beanParamWrapper = ParamWrapper.craft(context.getFinallyVar());
        beanParamWrapper.setExpression(expression);
        beanParamWrapper.setExpectedResultType(type);
        beanParamWrapper.setRootObject(bean);
        return context.getSpELConverter().getSpELRuntime().getValueForType(beanParamWrapper);
    }

    public <V> SpelBean<V> getSpelBean(String expression, Type type) {
        return SpelBean.of(context, get(expression, type));
    }

    public <V> SpelBean<V> getSpelBean(String expression, Class<V> type) {
        return getSpelBean(expression, (Type) type);
    }

    public <V> SpelBean<V> getSpelBean(String expression, SerializationTypeToken<V> type) {
        return getSpelBean(expression, type.getType());
    }

    public <V> SpelBean<V> getSpelBean(String expression, ResolvableType type) {
        return getSpelBean(expression, type.getType());
    }

    public SpelBean<?> getSpelBean(String expression) {
        return getSpelBean(expression, Object.class);
    }

    public boolean eq(String expression, Object value) {
        return Objects.equals(get(expression), value);
    }

    public boolean ne(String expression, Object value) {
        return !eq(expression, value);
    }

    public <V> boolean eq(String expression, V value, Class<V> type) {
        return Objects.equals(get(expression, type), value);
    }

    public <V> boolean ne(String expression, V value, Class<V> type) {
        return !eq(expression, value, type);
    }


    @Override
    public String toString() {
        return "@SpelBean: " + bean;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }
}
