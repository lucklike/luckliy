package com.luckyframework.httpclient.generalapi.plugin.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 禁止使用缓存插件，标注该注解后，即使当前元素或者父级元素使用{@link CachePluginMeta}配置了缓存插件也不会生效
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CachePluginProhibition {
}
