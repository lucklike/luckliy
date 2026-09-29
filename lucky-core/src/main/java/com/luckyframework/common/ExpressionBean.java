package com.luckyframework.common;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.List;

/**
 * 支持表达式操作的 Bean 包装器接口
 *
 * <p>该接口将原始对象（{@link #getBean()}）包装为统一的可计算数据视图，提供以下能力：</p>
 * <pre>
 *     1.表达式读取与写入：{@link #get(String, Type)}、{@link #set(String, Object)}
 *     2.结构转换：{@link #to(Type)}、{@link #beanConvert(Type)}、{@link #beanBind(Type)}
 *     3.类型化的便捷取值：基本类型、数组、{@link List} 等场景的快速访问方法
 * </pre>
 *
 * <p><b>实现说明：</b>表达式的具体语法（方言）由实现类决定，例如：</p>
 * <pre>
 *     1.{@link FlatBean}                              —— 属性路径表达式，如：{@code user.name}、{@code list[0]['key']}
 *     2.{@link com.luckyframework.spel.SimpleSpelBean} —— SpEL 表达式，支持完整的 SpEL 语法（不携带上下文变量）
 *     3.SpelBean（位于 lucky-httpclient 模块）          —— SpEL 表达式，并且可以访问 HTTP 客户端方法上下文中的变量与函数
 * </pre>
 * <p>在不同实现之间切换时，请注意表达式语法与语义的差异。</p>
 *
 * <p>典型用法：</p>
 * <pre>
 *     FlatBean&lt;?&gt; bean = FlatBean.of(configMap);
 *     String name = bean.getString("user.name");
 * </pre>
 *
 * <p>注意：取值方法的返回值均可能为 {@code null}，将基本类型包装类的结果用于自动拆箱时请留意空值风险。</p>
 *
 * @param <T> Bean 对象类型
 * @author fukang
 * @version 1.0.0
 * @date 2025/12/1 01:25
 */
public interface ExpressionBean<T> {

    /**
     * 获取原始 Bean 对象
     *
     * @return 原始 Bean 对象
     */
    T getBean();

    /**
     * 通过表达式给对象的某个属性赋值
     *
     * @param expression 赋值表达式（具体语法由实现类决定）
     * @param value      值
     */
    void set(String expression, Object value);

    /**
     * 通过表达式来获取对象中的某个属性的值
     *
     * @param expression 取值表达式（具体语法由实现类决定）
     * @param type       结果类型
     * @param <V>        结果类型泛型
     * @return 取值表达式对应的值
     */
    <V> V get(String expression, Type type);

    /**
     * 将自身转化为另一种泛型结构的{@link ExpressionBean}
     * <p>相当于先将内部 Bean 对象转换为目标类型，再将转换结果重新包装为对应类型的{@link ExpressionBean}</p>
     *
     * @param type 目标类型
     * @param <R>  目标类型泛型
     * @return 目标类型的{@link ExpressionBean}对象
     */
    <R> ExpressionBean<R> to(Type type);

    /**
     * 是否存在 Bean 对象（即{@link #getBean()}是否不为 {@code null}）
     *
     * @return 是否存在 Bean 对象
     */
    default boolean hasBean() {
        return getBean() != null;
    }

    /**
     * 判断内部 Bean 对象是否为传入类型的实例
     *
     * @param clazz 类型
     * @return 内部 Bean 对象是否为传入类型的实例
     */
    default boolean beanTypeMatch(Class<?> clazz) {
        return clazz.isInstance(getBean());
    }

    //----------------------------------------------------------
    //                      to
    //----------------------------------------------------------

    /**
     * {@link #to(Type)} 的 Class 版本
     *
     * @param type 目标类型的 Class
     * @param <R>  目标类型泛型
     * @return 目标类型的{@link ExpressionBean}对象
     * @see #to(Type)
     */
    default <R> ExpressionBean<R> to(Class<R> type) {
        return to((Type) type);
    }

    /**
     * {@link #to(Type)} 的类型Token版本，用于保留目标类型的泛型信息
     *
     * @param typeToken 目标类型的类型Token
     * @param <R>       目标类型泛型
     * @return 目标类型的{@link ExpressionBean}对象
     * @see #to(Type)
     */
    default <R> ExpressionBean<R> to(SerializationTypeToken<R> typeToken) {
        return to(typeToken.getType());
    }

    /**
     * {@link #to(Type)} 的{@link ResolvableType}版本
     *
     * @param type 目标类型的{@link ResolvableType}
     * @param <R>  目标类型泛型
     * @return 目标类型的{@link ExpressionBean}对象
     * @see #to(Type)
     */
    default <R> ExpressionBean<R> to(ResolvableType type) {
        return to(type.getType());
    }

    //----------------------------------------------------------
    //                      beanConvert
    //----------------------------------------------------------

    /**
     * 将内部的 Bean 对象转换为指定类型的对象
     * <p>采用严格转换策略，类型不兼容或转换失败时会抛出异常</p>
     *
     * @param type 目标类型
     * @param <R>  目标类型泛型
     * @return 转换后的对象
     * @see ConversionUtils#conversion(Object, Type)
     */
    default <R> R beanConvert(Type type) {
        return ConversionUtils.conversion(getBean(), type);
    }

    /**
     * {@link #beanConvert(Type)} 的 Class 版本
     *
     * @param type 目标类型的 Class
     * @param <R>  目标类型泛型
     * @return 转换后的对象
     * @see #beanConvert(Type)
     */
    default <R> R beanConvert(Class<R> type) {
        return beanConvert((Type) type);
    }

    /**
     * {@link #beanConvert(Type)} 的类型Token版本，用于保留目标类型的泛型信息
     *
     * @param typeToken 目标类型的类型Token
     * @param <R>       目标类型泛型
     * @return 转换后的对象
     * @see #beanConvert(Type)
     */
    default <R> R beanConvert(SerializationTypeToken<R> typeToken) {
        return beanConvert(typeToken.getType());
    }

    /**
     * {@link #beanConvert(Type)} 的{@link ResolvableType}版本
     *
     * @param type 目标类型的{@link ResolvableType}
     * @param <R>  目标类型泛型
     * @return 转换后的对象
     * @see #beanConvert(Type)
     */
    default <R> R beanConvert(ResolvableType type) {
        return beanConvert(type.getType());
    }

    //----------------------------------------------------------
    //                      beanBind
    //----------------------------------------------------------

    /**
     * 将内部的 Bean 对象以「松散绑定」的方式转换为指定类型的对象
     * <p>松散绑定（Relaxed Binding）规则：</p>
     * <pre>
     *     1.属性名匹配忽略大小写
     *     2.支持中划线（-）、下划线（_）与驼峰命名之间的互相匹配，如：user-name、user_name、userName
     *     3.忽略目标类型中不存在的字段
     *     4.支持对嵌套的 POJO、集合、数组与 Map 结构进行递归绑定
     * </pre>
     *
     * @param type 目标类型
     * @param <R>  目标类型泛型
     * @return 绑定结果对象
     * @see ConversionUtils#looseBind(Type, Object)
     */
    default <R> R beanBind(Type type) {
        return ConversionUtils.looseBind(type, getBean());
    }

    /**
     * {@link #beanBind(Type)} 的 Class 版本
     *
     * @param type 目标类型的 Class
     * @param <R>  目标类型泛型
     * @return 绑定结果对象
     * @see #beanBind(Type)
     */
    default <R> R beanBind(Class<R> type) {
        return beanBind((Type) type);
    }

    /**
     * {@link #beanBind(Type)} 的类型Token版本，用于保留目标类型的泛型信息
     *
     * @param typeToken 目标类型的类型Token
     * @param <R>       目标类型泛型
     * @return 绑定结果对象
     * @see #beanBind(Type)
     */
    default <R> R beanBind(SerializationTypeToken<R> typeToken) {
        return beanBind(typeToken.getType());
    }

    /**
     * {@link #beanBind(Type)} 的{@link ResolvableType}版本
     *
     * @param type 目标类型的{@link ResolvableType}
     * @param <R>  目标类型泛型
     * @return 绑定结果对象
     * @see #beanBind(Type)
     */
    default <R> R beanBind(ResolvableType type) {
        return beanBind(type.getType());
    }

    //----------------------------------------------------------
    //                          get
    //----------------------------------------------------------

    /**
     * 通过表达式获取对象中的某个属性的值
     *
     * @param expression 取值表达式
     * @return 取值表达式对应的值
     * @see #get(String, Type)
     */
    default Object get(String expression) {
        return get(expression, Object.class);
    }

    /**
     * {@link #get(String, Type)} 的{@link ResolvableType}版本
     *
     * @param expression 取值表达式
     * @param type       结果类型的{@link ResolvableType}
     * @param <V>        结果类型泛型
     * @return 取值表达式对应的值
     * @see #get(String, Type)
     */
    default <V> V get(String expression, ResolvableType type) {
        return get(expression, type.getType());
    }

    /**
     * {@link #get(String, Type)} 的类型Token版本，用于保留结果类型的泛型信息
     *
     * @param expression 取值表达式
     * @param typeToken  结果类型的类型Token
     * @param <V>        结果类型泛型
     * @return 取值表达式对应的值
     * @see #get(String, Type)
     */
    default <V> V get(String expression, SerializationTypeToken<V> typeToken) {
        return get(expression, typeToken.getType());
    }

    /**
     * {@link #get(String, Type)} 的 Class 版本
     *
     * @param expression 取值表达式
     * @param clazz      结果类型的 Class
     * @param <V>        结果类型泛型
     * @return 取值表达式对应的值
     * @see #get(String, Type)
     */
    default <V> V get(String expression, Class<V> clazz) {
        return get(expression, (Type) clazz);
    }

    /**
     * 通过表达式获取对象中的某个属性的值，并将结果转换为指定元素类型的{@link List}
     *
     * @param expression   取值表达式
     * @param elementClass {@link List}的元素类型
     * @param <E>          元素类型泛型
     * @return 表达式对应的{@link List}结果
     */
    default <E> List<E> getList(String expression, Class<E> elementClass) {
        return get(expression, ResolvableType.forClassWithGenerics(List.class, elementClass));
    }

    /**
     * 通过表达式获取对象中的某个属性的值，并将结果转换为元素类型为 {@code Object} 的{@link List}
     *
     * @param expression 取值表达式
     * @return 表达式对应的{@link List}结果
     * @see #getList(String, Class)
     */
    default List<?> getList(String expression) {
        return getList(expression, Object.class);
    }

    //----------------------------------------------------------
    //                          bind
    //----------------------------------------------------------

    /**
     * 通过表达式获取对象中的某个属性的值，并将取值结果以「松散绑定」的方式转换为指定类型的对象
     *
     * @param expression 取值表达式
     * @param type       目标类型
     * @param <R>        目标类型泛型
     * @return 绑定结果对象
     * @see #beanBind(Type)
     */
    default <R> R bind(String expression, Type type) {
        return ConversionUtils.looseBind(type, get(expression, Object.class));
    }

    /**
     * {@link #bind(String, Type)} 的 Class 版本
     *
     * @param expression 取值表达式
     * @param type       目标类型的 Class
     * @param <R>        目标类型泛型
     * @return 绑定结果对象
     * @see #bind(String, Type)
     */
    default <R> R bind(String expression, Class<R> type) {
        return bind(expression,(Type) type);
    }

    /**
     * {@link #bind(String, Type)} 的类型Token版本，用于保留目标类型的泛型信息
     *
     * @param expression 取值表达式
     * @param typeToken  目标类型的类型Token
     * @param <R>        目标类型泛型
     * @return 绑定结果对象
     * @see #bind(String, Type)
     */
    default <R> R bind(String expression, SerializationTypeToken<R> typeToken) {
        return bind(expression, typeToken.getType());
    }

    /**
     * {@link #bind(String, Type)} 的{@link ResolvableType}版本
     *
     * @param expression 取值表达式
     * @param type       目标类型的{@link ResolvableType}
     * @param <R>        目标类型泛型
     * @return 绑定结果对象
     * @see #bind(String, Type)
     */
    default <R> R bind(String expression, ResolvableType type) {
        return bind(expression, type.getType());
    }

    //----------------------------------------------------------
    //                    Basic Types
    //----------------------------------------------------------


    /** 获取表达式结果并转换为 {@code String} 类型 */
    default String getString(String expression) {
        return get(expression, String.class);
    }

    /** 获取表达式结果并转换为 {@code Integer} 类型 */
    default Integer getInt(String expression) {
        return get(expression, Integer.class);
    }

    /** 获取表达式结果并转换为 {@code Long} 类型 */
    default Long getLong(String expression) {
        return get(expression, Long.class);
    }

    /** 获取表达式结果并转换为 {@code Double} 类型 */
    default Double getDouble(String expression) {
        return get(expression, Double.class);
    }

    /** 获取表达式结果并转换为 {@code Boolean} 类型 */
    default Boolean getBoolean(String expression) {
        return get(expression, Boolean.class);
    }

    /** 获取表达式结果并转换为 {@code Float} 类型 */
    default Float getFloat(String expression) {
        return get(expression, Float.class);
    }

    /** 获取表达式结果并转换为 {@code Short} 类型 */
    default Short getShort(String expression) {
        return get(expression, Short.class);
    }

    /** 获取表达式结果并转换为 {@code Byte} 类型 */
    default Byte getByte(String expression) {
        return get(expression, Byte.class);
    }

    /** 获取表达式结果并转换为 {@code Character} 类型 */
    default Character getChar(String expression) {
        return get(expression, Character.class);
    }

    //----------------------------------------------------------
    //                    Basic Types Array
    //----------------------------------------------------------

    /** 获取表达式结果并转换为 {@code String[]} 类型 */
    default String[] getStringArray(String expression) {
        return get(expression, String[].class);
    }

    /** 获取表达式结果并转换为 {@code int[]} 类型 */
    default int[] getIntArray(String expression) {
        return get(expression, int[].class);
    }

    /** 获取表达式结果并转换为 {@code long[]} 类型 */
    default long[] getLongArray(String expression) {
        return get(expression, long[].class);
    }

    /** 获取表达式结果并转换为 {@code double[]} 类型 */
    default double[] getDoubleArray(String expression) {
        return get(expression, double[].class);
    }

    /** 获取表达式结果并转换为 {@code boolean[]} 类型 */
    default boolean[] getBooleanArray(String expression) {
        return get(expression, boolean[].class);
    }

    /** 获取表达式结果并转换为 {@code float[]} 类型 */
    default float[] getFloatArray(String expression) {
        return get(expression, float[].class);
    }

    /** 获取表达式结果并转换为 {@code short[]} 类型 */
    default short[] getShortArray(String expression) {
        return get(expression, short[].class);
    }

    /** 获取表达式结果并转换为 {@code byte[]} 类型 */
    default byte[] getByteArray(String expression) {
        return get(expression, byte[].class);
    }

    /** 获取表达式结果并转换为 {@code char[]} 类型 */
    default char[] getCharArray(String expression) {
        return get(expression, char[].class);
    }


    //----------------------------------------------------------
    //                    Basic Types List
    //----------------------------------------------------------

    /** 获取表达式结果并转换为 {@code List<String>} 类型 */
    default List<String> getStringList(String expression) {
        return getList(expression, String.class);
    }

    /** 获取表达式结果并转换为 {@code List<Integer>} 类型 */
    default List<Integer> getIntList(String expression) {
        return getList(expression, Integer.class);
    }

    /** 获取表达式结果并转换为 {@code List<Long>} 类型 */
    default List<Long> getLongList(String expression) {
        return getList(expression, Long.class);
    }

    /** 获取表达式结果并转换为 {@code List<Double>} 类型 */
    default List<Double> getDoubleList(String expression) {
        return getList(expression, Double.class);
    }

    /** 获取表达式结果并转换为 {@code List<Boolean>} 类型 */
    default List<Boolean> getBooleanList(String expression) {
        return getList(expression, Boolean.class);
    }

    /** 获取表达式结果并转换为 {@code List<Float>} 类型 */
    default List<Float> getFloatList(String expression) {
        return getList(expression, Float.class);
    }

    /** 获取表达式结果并转换为 {@code List<Short>} 类型 */
    default List<Short> getShortList(String expression) {
        return getList(expression, Short.class);
    }

    /** 获取表达式结果并转换为 {@code List<Byte>} 类型 */
    default List<Byte> getByteList(String expression) {
        return getList(expression, Byte.class);
    }

    /** 获取表达式结果并转换为 {@code List<Character>} 类型 */
    default List<Character> getCharList(String expression) {
        return getList(expression, Character.class);
    }
}
