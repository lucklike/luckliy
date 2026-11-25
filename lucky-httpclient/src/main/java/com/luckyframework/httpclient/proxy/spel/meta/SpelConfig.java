package com.luckyframework.httpclient.proxy.spel.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个配置模型。
 *
 * <p>只描述"配置前缀、别名前缀、字段名到key的命名策略、生效层级"；字段是否支持SpEL仍由
 * {@link SpelAttribute}逐个声明。本注解不提供字段的默认阶段，
 * 也不存在"整个配置模型默认识别"的隐式规则。
 *
 * <p>本注解只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
@LuckyComponent(type = LuckyComponentType.CONFIG, group = "config", priority = 4)
public @interface SpelConfig {

    /**
     * 配置前缀，必填。
     * <p>空串表示该模型没有独立配置前缀，只能作为嵌套字段的类型使用，
     * 实际路径由父字段的{@link SpelAttribute#key()}决定。
     */
    String prefix();

    /**
     * 其他可用的配置前缀。
     */
    String[] aliases() default {};

    /**
     * 字段名到配置key的命名策略，默认保持原样。
     */
    SpelNaming naming() default SpelNaming.AS_IS;

    /**
     * 该配置模型参与的配置层级，默认类级与方法级共用。
     */
    ConfigScope scope() default ConfigScope.BOTH;

    /**
     * 说明文本，供IDE展示。
     */
    String description() default "";
}
