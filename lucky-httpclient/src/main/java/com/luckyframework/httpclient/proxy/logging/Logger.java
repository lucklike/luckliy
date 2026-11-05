package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.PrintLog;
import com.luckyframework.httpclient.proxy.annotations.PrintLogProhibition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定声明式HTTP接口使用的日志处理器
 * <pre>
 *     本注解只负责「由谁来打印日志」，不负责「打印什么」：
 *     打印内容（请求体/响应体的长度上限、允许的MimeType、是否打印响应头、打印条件、脱敏规则等）
 *     由{@link PrintLog}控制，两个注解可以同时标注、互不冲突；
 *     标注了{@link PrintLogProhibition}的接口或方法则一律不打印日志。
 *
 *     日志处理器的生效顺序（越靠前越优先）：
 *     1.本注解：从被调用的方法出发沿声明处与接口/父类向上查找，
 *       因此标在接口或类上即可对该类型下的所有方法生效，方法上再标注则覆盖类上的配置；
 *     2.工厂的全局处理器{@link com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory#setLoggerHandler}；
 *     3.两者都没有配置时使用{@link NotRecordLog}，即不打印任何日志。
 *
 *     本注解必须指定{@link #handlerClass()}与{@link #handler()}中的至少一个：
 *     两个都保持默认值时框架无法确定要创建哪种处理器，会在生成处理器时抛出{@link IllegalArgumentException}；
 *     同时指定时以{@link #handler()}为准。
 *
 *     现成的处理器与对应的预设注解：
 *     1.{@link BeautifulLogger}等价于{@code @Logger(handlerClass = BeautifulLoggerPrintHandler.class)}，
 *       多行缩进排版，适合开发调试时在控制台阅读；
 *     2.{@link SimpleLogger}等价于{@code @Logger(handlerClass = SimpleLoggerPrintHandler.class)}，
 *       单行排版且换行被压平，适合日志采集与检索；
 *     3.需要自定义输出内容时实现{@link LoggerHandler}，再用本注解指定它。
 *
 *     日志最终通过「处理器类名」对应的Logger输出（{@code LoggerFactory.getLogger(处理器类名)}），
 *     因此可以为不同处理器单独配置日志级别与输出目标：
 *     {@code
 *     @Logger(handlerClass = SimpleLoggerPrintHandler.class)
 *     @Post("login")
 *     LoginResult login(@BodyParam LoginParam param);
 *     }
 *
 *     由于{@code @Target}中包含{@link ElementType#ANNOTATION_TYPE}，本注解也可以作为元注解使用，
 *     把{@link Logger}与{@link PrintLog}的配置一起组合成业务自己的日志注解。
 * </pre>
 *
 * @see PrintLog
 * @see LoggerHandler
 * @see BeautifulLogger
 * @see SimpleLogger
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface Logger {

    /**
     * 日志处理器的Class
     * <pre>
     *     框架通过反射创建该类型的实例（单例作用域，同一个Class只会创建一次），
     *     因此该类型必须能通过无参构造创建；需要自定义创建过程时改用{@link #handler()}。
     *     保持默认值{@link LoggerHandler}表示「本属性未指定」，
     *     此时{@link #handler()}也不能保持默认值，否则视为非法配置。
     * </pre>
     *
     * @see BeautifulLoggerPrintHandler
     * @see SimpleLoggerPrintHandler
     * @see NotRecordLog
     */
    Class<? extends LoggerHandler> handlerClass() default LoggerHandler.class;

    /**
     * 用于生成日志处理器对象的对象生成器
     * <pre>
     *     比{@link #handlerClass()}更灵活：{@link ObjectGenerate#clazz()}既可以写处理器Class，
     *     也可以写自定义的对象工厂（{@code ObjectFactory}的实现），
     *     {@link ObjectGenerate#msg()}用于给对象工厂传递额外的创建信息，
     *     {@link ObjectGenerate#scope()}用于指定对象的作用域。
     *     因此需要自定义处理器的创建过程时使用本属性，与{@link #handlerClass()}同时配置时本属性优先。
     * </pre>
     */
    ObjectGenerate handler() default @ObjectGenerate(LoggerHandler.class);

}
