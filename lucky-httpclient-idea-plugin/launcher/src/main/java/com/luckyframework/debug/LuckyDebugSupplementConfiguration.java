package com.luckyframework.debug;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 调试容器追加的配置类。
 *
 * 启动时与用户主类一起交给 SpringApplicationBuilder，用于补注册
 * starter 的装配类——发布版 starter 没有 AutoConfiguration.imports，
 * 未标注 @EnableLuckyHttpClient 的项目只能靠这里拿到工厂 bean。
 *
 * 用户已标注该注解时，Spring 会按导入类去重，不会重复注册。
 */
@Configuration(proxyBeanMethods = false)
@Import(LuckyDebugImportSelector.class)
public class LuckyDebugSupplementConfiguration {
}
