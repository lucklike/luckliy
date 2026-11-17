package com.luckyframework.httpclient.proxy.spel.meta;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记同一注解内提供SpEL生效阶段的元素。
 *
 * <p>被标注元素的类型必须是{@link Lifecycle}，且同一注解内最多标注一个。
 *
 * <p>它是注解级的默认阶段来源：同注解内所有{@link SpelAttribute}元素，只要
 * {@link SpelAttribute#stage()}没有被显式书写，都使用该元素的值；显式书写的
 * {@code stage}优先。组合注解通过{@code @AliasFor}指向声明端时自动跟随，不需要重复标注。
 *
 * <p>它不携带任何属性值，也不点名任何属性，只标记"运行期实参所在的元素"；
 * {@link SpelAttribute#kind()}、{@link SpelAttribute#result()}等声明期常量仍须逐属性写死，
 * 不存在注解级预设或覆盖。
 *
 * <p>本注解只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelStage {
}
