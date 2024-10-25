package com.luckyframework.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  1.Class类型
 *  2.基本类型以及基本类型的包装类型
 *  3.基本类型以及其包装类型的数组
 *  4.非JDK类型
 *  5.集合类型(List、Set)
 *    5.1.泛型为基本类型
 *    5.2.泛型为非JDK类型
 *    5.3.泛型为Class
 *  6.Map类型(String => Object)
 * @author fk
 * @version 1.0
 * @date 2020/12/18 0018 9:47
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value();
}
