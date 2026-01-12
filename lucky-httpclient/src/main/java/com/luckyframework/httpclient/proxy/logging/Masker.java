package com.luckyframework.httpclient.proxy.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.luckyframework.httpclient.proxy.logging.MaskType.NON;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Masker {

    /**
     * 脱敏处理器,优先级2
     */
    Class<? extends CustomMasker> maskerHandler() default CustomMasker.class;

    /**
     * 脱敏类型,优先级3
     */
    MaskType type() default NON;

    /**
     * 脱敏关键字，大小写不敏感
     */
    String[] keys();


}
