package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;

import java.lang.annotation.*;

/**
 * 美化日志：多行缩进排版，等价于{@code @Logger(handlerClass = BeautifulLoggerPrintHandler.class)}
 * <pre>
 *     请求日志以{@code >>}开头并以{@code >>}结尾，响应日志以{@code <<}开头并以{@code <<}结尾，
 *     中间按区域逐行缩进输出：
 *     1.请求：标题（异步请求带⚡️标记）、接口描述、线程名、请求ID、HTTP执行器、方法位置、
 *       请求方法与URL、请求头、请求体；
 *     2.响应：标题（含重试/重定向/Mock标记）、接口描述、线程名、请求ID、HTTP执行器、方法位置、
 *       请求方法与URL、HTTP版本与状态码（2xx/3xx/4xx/5xx分别着色，慢响应追加⚠️并将耗时标红）、
 *       响应头（需要{@link PrintLog#printRespHeader()}开启）、响应体；
 *     3.JSON与XML的请求体/响应体会先格式化换行再整体缩进，multipart表单按{@code --LuckyBoundary}
 *       分段展开（文件段展示文件名、MimeType与文件描述），urlencoded表单按{@code &}换行；
 *     4.超出{@link PrintLog#allowReqBodyMaxLength()}与{@link PrintLog#allowRespBodyMaxLength()}
 *       的内容会被截断，并在末尾标注{@code ⇡...(limit:n)...⇡}。
 *
 *     多行加彩色的排版适合开发调试时在控制台阅读；
 *     日志量大、需要按行检索或采集时，请使用排版更紧凑的{@link SimpleLogger}。
 *
 *     打印内容、脱敏、打印条件等仍然由{@link PrintLog}控制，
 *     标注了{@link PrintLogProhibition}的接口或方法不会输出日志；
 *     日志通过名为{@code BeautifulLoggerPrintHandler}的Logger输出，可以为它单独配置级别与输出目标。
 *     另外，配置类接口（{@code @EnableConfigurationParser}）的日志固定使用本处理器的排版，
 *     由配置源中的logger配置驱动，与本注解无关。
 *
 *     可以标注在方法、接口/类上，也可以作为元注解组合进自定义注解：
 *     {@code
 *     @BeautifulLogger
 *     @Post("login")
 *     LoginResult login(@BodyParam LoginParam param);
 *     }
 * </pre>
 *
 * @see Logger
 * @see BeautifulLoggerPrintHandler
 * @see SimpleLogger
 * @see PrintLog
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
@Logger(handlerClass = BeautifulLoggerPrintHandler.class)
public @interface BeautifulLogger {
}
