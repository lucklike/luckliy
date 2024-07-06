package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.TAG;
import com.luckyframework.httpclient.proxy.statics.XmlBodyHandle;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 静态Xml参数配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Combination({StaticBody.class})
@StaticBody(mimeType = "application/xml", bodyHandle = @ObjectGenerate(XmlBodyHandle.class))
public @interface StaticXmlBody {


    /**
     * <pre>
     * Body配置，支持SpEL表达式，表达式部分需要写在#{}中
     *
     * SpEL表达式内置参数有：
     *  root:{
     *      <b>SpEL Env : </b>
     *      {@value TAG#SPRING_ROOT_VAL}
     *      {@value TAG#SPRING_VAL}
     *
     *      <b>Context : </b>
     *      {@value TAG#METHOD_CONTEXT}
     *      {@value TAG#CLASS_CONTEXT}
     *      {@value TAG#ANNOTATION_CONTEXT}
     *      {@value TAG#CLASS}
     *      {@value TAG#METHOD}
     *      {@value TAG#THIS}
     *      {@value TAG#ANNOTATION_INSTANCE}
     *      {@value TAG#PARAM_TYPE}
     *      {@value TAG#PN}
     *      {@value TAG#PN_TYPE}
     *      {@value TAG#PARAM_NAME}
     *  }
     * </pre>
     */
    @AliasFor(annotation = StaticBody.class, attribute = "body")
    String value();

    /**
     * 进行URL编码时采用的编码方式
     */
    @AliasFor(annotation = StaticBody.class, attribute = "charset")
    String charset() default "UTF-8";

}
