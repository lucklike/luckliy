package com.luckyframework.httpclient.proxy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口包装器注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Wrapper {

    /**
     * 执行包装逻辑的SpEL表达式
     * <pre>
     *  例如：
     * {@code
     *
     *  interface UserApi {
     *
     *      // 只获取用户名称
     *      @Wrapper("#{$this$.getUser(id).name}")
     *      String getUserName(String id);
     *
     *      // 只获取用户年龄
     *      @Wrapper("#{$this$.getUser(id).age}")
     *      Integer getUserAge(String id);
     *
     *      // 获取用户信息
     *      @Get("http://localhost:8080/user/get?id=#{id}")
     *      User getUser(String id);
     *
     *  }
     * }
     * </pre>
     */
    String value();

    /**
     * 是否等创建完Request对象之后在执行Wrapper逻辑
     */
    boolean waitReqCreatComplete() default false;

}
