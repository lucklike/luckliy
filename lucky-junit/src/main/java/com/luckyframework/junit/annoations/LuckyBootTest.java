package com.luckyframework.junit.annoations;

import com.luckyframework.junit.core.LuckyExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/18 2:54 下午
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestConfiguration
@ExtendWith({LuckyExtension.class})
public @interface LuckyBootTest {

    @AliasFor(annotation = TestConfiguration.class,attribute = "rootClasses")
    Class<?>[] value() default {};

    @AliasFor(annotation = TestConfiguration.class,attribute = "rootClasses")
    Class<?>[] rootClasses() default {};

    @AliasFor(annotation = TestConfiguration.class,attribute = "basePackages")
    String[] basePackages() default {};
}
