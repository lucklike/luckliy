package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.impl.SpELDomainNameGetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 支持SpEL表达式的域名配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DomainName(getter = SpELDomainNameGetter.class)
public @interface SpELDomainName {

    /**
     * 请求的域名配置
     */
    @AliasFor(annotation = DomainName.class, attribute = "value")
    String value() default "";


}
