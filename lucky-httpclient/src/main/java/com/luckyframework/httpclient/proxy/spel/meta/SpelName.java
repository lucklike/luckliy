package com.luckyframework.httpclient.proxy.spel.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记参数注解中提供SpEL变量名的元素。
 *
 * <p>只在{@link SpelSymbol}标注在注解类型上时有意义；被标注元素的类型必须是
 * {@code String}，且同一注解内最多标注一个。
 *
 * <p>名称取值顺序：{@link SpelSymbol#name()}显式值 → 本注解标注元素的值 →
 * 回退到{@code value}元素（保持旧行为）→ 都没有则无名称，不回退参数名。
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
public @interface SpelName {
}
