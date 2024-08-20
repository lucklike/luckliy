package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;

/**
 * Mock注解
 * <pre>
 *     1.优先级
 *     mockResp的优先级 > status + header + body
 *
 *     2.约定大于配置
 *     当mockResp不做任何配置时，Lucky会检测当前代理接口中是否存在方法名+Mock的静态方法，如果有则会自动使用该方法来生成MockResponse
 *
 *     {@code
 *     @HttpClientComponent
 *     public interface MockApi{
 *
 *
 *         @Mock
 *         @Get("/hello")
 *         String hello();
 *
 *         // hello方法的默认Mock方法helloMock()
 *         static MockResponse helloMock() {
 *             return MockResponse.create()
 *                     .status(200)
 *                     .header("Content-Type: text/plain")
 *                     .body("Mock Hello World!");
 *         }
 *
 *          @Mock(
 *             status = 302,
 *             header = "Location: http://www.baidu.com",
 *             body = "Hello"
 *          )
 *         @Get("/index")
 *         String index()
 *     }
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@MockMeta(mock = @ObjectGenerate(DefaultMockResponseFactory.class))
public @interface Mock {

    /**
     * 启用Mock的条件表达式
     */
    @AliasFor(annotation = MockMeta.class, attribute = "condition")
    String condition() default "";

    /**
     * 生成{@link MockResponse}的SpEL表达式
     */
    @AliasFor("mockResp")
    String value() default "";

    /**
     * 生成{@link MockResponse}的SpEL表达式
     */
    @AliasFor("value")
    String mockResp() default "";

    /**
     * HTTP状态值
     */
    int status() default 200;

    /**
     * 响应头，支持SpEL表达式，格式：Key: Value
     */
    String[] header() default {};

    /**
     * 响应体，支持SpEL表达式<br/>
     *
     * <pre>
     *  支持返回的类型为：
     *  1.{@link String}，Content-Type需要在{@link #header()}中进行配置
     *  2.{@link byte[]}，Content-Type需要在{@link #header()}中进行配置
     *  3.{@link InputStream}，Content-Type需要在{@link #header()}中进行配置
     *  4.{@link File}，Content-Type会自动根据文件获取，并且会设置Content-Disposition
     *  5.{@link Resource}，Content-Type会自动根据文件获取，并且会设置Content-Disposition
     *  6.{@link InputStreamSource}，Content-Type需要在{@link #header()}中进行配置
     *  7.{@link ByteBuffer}，Content-Type需要在{@link #header()}中进行配置
     * </pre>
     *
     */
    String body() default "";

    /**
     * 是否缓存第一次生成的Mock响应对象
     */
    boolean cache() default true;

}
