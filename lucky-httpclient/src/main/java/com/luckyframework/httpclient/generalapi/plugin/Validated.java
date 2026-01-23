package com.luckyframework.httpclient.generalapi.plugin;


import com.luckyframework.httpclient.proxy.plugin.Plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数校验注解，使用此插件需要额外导入如下两个依赖包
 * <pre>
 *     {@code
 *     // 1.实现java-validation规范的依赖包 例如：
 *      <dependency>
 *          <groupId>org.hibernate.validator</groupId>
 *          <artifactId>hibernate-validator</artifactId>
 *          <version>6.2.5.Final</version>
 *      </dependency>
 *
 *
 *     // 2.实现El表达式的依赖包 例如：
 *      <dependency>
 *          <groupId>org.apache.tomcat.embed</groupId>
 *          <artifactId>tomcat-embed-el</artifactId>
 *          <version>9.0.83</version>
 *      </dependency>
 *     }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Plugin(pluginClass = ValidationPlugin.class)
public @interface Validated {

    Class<?>[] value() default {};
}
