package com.luckyframework.condition;

import java.lang.annotation.*;

/**
 * Linux操作系统条件器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/26 20:02
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnOS("linux")
public @interface ConditionalOnLinux {


}
