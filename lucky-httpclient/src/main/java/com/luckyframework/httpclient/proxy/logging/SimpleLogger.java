package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;

import java.lang.annotation.*;

/**
 * 简单日志：请求与响应各压成一行输出，等价于{@code @Logger(handlerClass = SimpleLoggerPrintHandler.class)}
 * <pre>
 *     一次请求与一次响应各输出一行：
 *     1.请求行以📤开头，依次输出请求ID、异步标记、HTTP执行器、接口与方法、请求方法、Content-Type、
 *       URL、查询参数、请求体、请求头；
 *     2.响应行以📩开头，依次输出请求ID、异步标记、HTTP执行器、接口与方法（描述与名称不同时再带上描述）、
 *       耗时、状态码（2xx/3xx/4xx/5xx分别着色）、带重试/重定向/Mock标记的URL、响应体、响应头，
 *       其中响应头需要{@link PrintLog#printRespHeader()}开启，慢响应还会追加⚠️标记；
 *     3.各个片段中的换行都会被压平为单行，超出{@link PrintLog#allowReqBodyMaxLength()}与
 *       {@link PrintLog#allowRespBodyMaxLength()}的内容会被截断，并在末尾标注{@code ...(limit: n)...}。
 *
 *     因为每条日志只占一行，适合写入文件、接入集中式日志系统、按请求ID检索；
 *     希望控制台里看到多行缩进、JSON/XML格式化后的排版时，请使用{@link BeautifulLogger}。
 *
 *     打印内容、脱敏、打印条件等仍然由{@link PrintLog}控制，
 *     标注了{@link PrintLogProhibition}的接口或方法不会输出日志；
 *     日志通过名为{@code SimpleLoggerPrintHandler}的Logger输出，可以为它单独配置级别与输出目标。
 *
 *     可以标注在方法、接口/类上，也可以作为元注解组合进自定义注解：
 *     {@code
 *     @SimpleLogger
 *     @Post("login")
 *     LoginResult login(@BodyParam LoginParam param);
 *     }
 * </pre>
 *
 * @see Logger
 * @see SimpleLoggerPrintHandler
 * @see BeautifulLogger
 * @see PrintLog
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
@Logger(handlerClass = SimpleLoggerPrintHandler.class)
public @interface SimpleLogger {
}
