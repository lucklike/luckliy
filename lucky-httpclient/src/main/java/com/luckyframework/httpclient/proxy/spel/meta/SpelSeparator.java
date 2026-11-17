package com.luckyframework.httpclient.proxy.spel.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记同一注解内哪个元素是{@link SpelKind#KEY_VALUE}的分隔符。
 *
 * <p>被标注元素的类型必须是{@code String}，且同一注解内最多标注一个。
 * 被标注的元素不需要再标{@link SpelAttribute}；两者同时标注时仍以"分隔符"语义为准，
 * 不参与SpEL求值。
 *
 * <p>分隔符取值顺序：{@link SpelAttribute#separator()}字面量非空 → 本注解标注元素的值 →
 * 都不存在时不拆分（安全降级）。
 *
 * <p>本注解不携带属性值，也不点名任何属性，属性改名时注解跟着元素一起移动。
 * 它只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SpelSeparator {
}
