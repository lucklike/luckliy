package com.luckyframework.httpclient.proxy.spel.meta;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个字段、参数或参数注解为SpEL符号。
 *
 * <p>可声明的符号：
 * <ul>
 *     <li>字段常量 → 变量；</li>
 *     <li>参数 → 参数变量；</li>
 *     <li>注解类型 → 参数注解声明的变量，变量名由{@link SpelName}标注的元素提供。</li>
 * </ul>
 *
 * <p>函数不通过本注解声明：函数是否可用及其生命周期由{@code @SPELImport}与
 * {@code HttpClientProxyObjectFactory#addSpringElFunctionClass(Class)}决定，
 * 插件按这两条路径识别，本注解不提供函数声明入口。
 *
 * <p>名称、类型、可见阶段在未显式书写时按位置和Java签名自动推导；
 * 说明、参数说明、返回值、示例、版本、弃用信息全部从Javadoc读取，不在注解中重复。
 *
 * <p>{@code @Rar}/{@code @Var}/{@code @Bean}/{@code @Env}等框架内置参数注入注解
 * 已在{@code value}元素上标注{@link SpelAttribute}，由插件按元数据识别；
 * {@code com.luckyframework.reflect.Param}位于lucky-core，依赖方向不允许其引用本模型的
 * 元注解，由插件按全限定名识别为{@link SpelKind#PARAMETER_INJECTION_EXPRESSION}。
 *
 * <p>本注解只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelSymbol {

    /**
     * 符号名。
     * <p>空串表示自动推导：字段常量读取常量值、参数取参数名、
     * 注解类型取{@link SpelName}标注元素的值（无则回退{@code value}元素）。
     */
    String name() default "";

    /**
     * 符号类型，默认按符号所在位置推导。
     */
    SpelSymbolKind kind() default SpelSymbolKind.AUTO;

    /**
     * 变量类型。
     * <p>{@link Object}是默认的哨兵值，表示从字段类型或参数类型读取，
     * 而不是「类型就是Object」。符号的真实类型就是{@link Object}时，改用
     * {@link #typeExpression()}写{@code "java.lang.Object"}，否则会被读成字段类型。
     * <p>泛型类型（如{@code Map<String, Object>}）只能用{@link #typeExpression()}表达。
     */
    Class<?> type() default Object.class;

    /**
     * 泛型类型覆盖。
     */
    String typeExpression() default "";

    /**
     * 从哪个阶段开始可见，默认{@link Lifecycle#METHOD}。
     * <p>取值优先级：本属性显式值 → Javadoc{@code @spelStage} → {@link Lifecycle#METHOD}。
     */
    Lifecycle from() default Lifecycle.METHOD;

    /**
     * 到哪个阶段为止，默认{@link Lifecycle#NON}，表示不设置阶段上界。
     * <p>{@code NON}不对应任何真实阶段，作为上界等价于无上界；它在这里与
     * {@code @Hook#lifecycle}里的“不执行”语义不同，插件按属性区分。
     */
    Lifecycle until() default Lifecycle.NON;
}
