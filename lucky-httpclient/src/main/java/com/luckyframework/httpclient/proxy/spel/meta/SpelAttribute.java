package com.luckyframework.httpclient.proxy.spel.meta;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明注解元素或配置字段的SpEL语义。
 *
 * <p>只有标注了本注解的元素才参与SpEL解析；没有标注即不参与。不存在白名单、分组、
 * {@code all}/{@code disabled}开关，也不存在"整个注解默认识别"的隐式规则。
 * 属性改名时注解跟着元素一起移动或一起报错，不需要维护任何属性名字符串清单。
 *
 * <p>与两个元素标记注解的分工：
 * <ul>
 *     <li>{@link SpelStage}只标记"阶段来源元素"，不改变本注解的{@link #kind()}、
 *     {@link #result()}等声明期常量；显式书写的{@link #stage()}优先于它；</li>
 *     <li>{@link SpelSeparator}只标记"分隔符元素"，与{@link #separator()}二选一，
 *     显式{@code separator}优先。</li>
 * </ul>
 *
 * <p>缺省即降级：未写{@code stage}按{@link Lifecycle#METHOD}处理（不做阶段过滤），
 * 未写{@code result}和{@code typeExpression}表示不限制结果类型，都不会产生误报。
 *
 * <p>配置字段通过{@link #key()}显式声明YAML键，未写时按所在类的{@link SpelConfig#naming()}
 * 由字段名推导；{@link #expandable()}声明集合值是否展开为多个同名参数；
 * {@link #resultType()}声明单个结果类型。
 *
 * <p>本注解只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface SpelAttribute {

    /**
     * 值形态，默认普通字符串模板。
     */
    SpelKind kind() default SpelKind.TEMPLATE;

    /**
     * 生命周期阶段，默认{@link Lifecycle#METHOD}，表示不参与阶段过滤（最宽松，避免漏报）。
     * <p>显式书写时优先于同注解内{@link SpelStage}标注元素的值。
     */
    Lifecycle stage() default Lifecycle.METHOD;

    /**
     * 结果类型候选，用于{@link Class}字面量能完整表达的类型（原始类型、数组、非参数化类型）。
     * <p>与{@link #typeExpression()}取并集，任一候选匹配即通过；两者都为空表示不限制类型。
     */
    Class<?>[] result() default {};

    /**
     * 完整类型表达式候选，用于{@link Class}字面量无法完整表达的泛型类型，
     * 例如{@code "java.util.Collection<java.io.File>"}。
     * <p>与{@link #result()}取并集，任一候选匹配即通过，不按索引对齐；
     * 每项必须是可解析的完整类型名，数组等{@link Class}能表达的类型仍写在{@link #result()}中。
     */
    String[] typeExpression() default {};

    /**
     * 粗粒度形态约束。
     * <p>空数组表示{@link SpelConstraint#ANY}；长度1时广播；否则与{@link #result()}等长。
     */
    SpelConstraint[] constraint() default {};

    /**
     * 字面量分隔符，用于{@link SpelKind#KEY_VALUE}。
     * <p>非空时优先于同注解内{@link SpelSeparator}标注元素的值。
     */
    String separator() default "";

    /**
     * key部分是否参与SpEL求值。
     */
    boolean keySpel() default false;

    /**
     * value部分是否参与SpEL求值；字面量属性用{@code false}。
     */
    boolean valueSpel() default true;

    /**
     * 可选的静态生效条件，例如{@code enable != false}。
     * <p>只支持属性引用、字面量、比较、逻辑和括号。
     */
    String condition() default "";

    /**
     * 配置key。
     * <p>缺省时由字段名与所在类的{@link SpelConfig#naming()}推导。
     */
    String key() default "";

    /**
     * 集合值是否展开为多个同名参数。
     */
    boolean expandable() default false;

    /**
     * 单个结果类型，{@link Object}表示未指定。
     * <p>与{@link #result()}并存时优先使用本属性。
     */
    Class<?> resultType() default Object.class;

    /**
     * 说明文本，供IDE展示。
     */
    String description() default "";
}
