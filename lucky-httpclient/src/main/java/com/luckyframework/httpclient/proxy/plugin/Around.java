package com.luckyframework.httpclient.proxy.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 定义环绕置增强逻辑的注解
 *
 * <pre>
 * 以下是一个案例：
 * </pre>
 *
 * <pre>
 *     {@code
 *
 *      // 目标接口
 *      @DomainName("http://localhost:8864/")
 *      public interface HttpApi {
 *
 *          @UseMyPlugin
 *          @Get("plugin")
 *          String pluginDemo();
 *
 *      }
 *
 *      // 插件类
 *      public class MyPlugin {
 *
 *          // 环绕增强，以下是一个统计方法耗时的逻辑
 *          // 1.方法返回值必须为{@link Object}
 *          // 2.必须存在{@link ProxyDecorator}类型的参数
 *          @Around(value = "#{#hasAnn($mec$, 'com.lucky.UseMyPlugin')}", init = "#{$this$.pluginInit($exeMeta$)}")
 *          public Object around(ExecuteMeta meta) throws Throwable {
 *                // 获取执行元数据
 *                ExecuteMeta meta = decorator.getMeta();
 *                String method = meta.getMethod().getName();
 *
 *                // 记录开始时间
 *                long start = System.currentTimeMillis();
 *                System.out.println("Start [" + method + "]");
 *
 *                // 执行代理方法
 *                Object proceed = decorator.proceed();
 *
 *                // 记录结束时间
 *                System.out.println("End [" + method + "]");
 *                long end = System.currentTimeMillis();
 *                System.out.println("Around Running [" + method + " Execution time: " + (end - start) + "ms]");
 *                return proceed;
 *          }
 *
 *          // 初始化方法
 *          public void pluginInit(ExecuteMeta meta) {
 *                Method method = meta.getMethod().getName();
 *                System.out.println("Init  [" + method + "]");
 *          }
 *      }
 *
 *      //------------------------------------------------------------------
 *      //                          Out put
 *      //------------------------------------------------------------------
 *      // < Init [pluginDemo]
 *      // < Start [pluginDemo]
 *      // < Execute Http request
 *      // < End [pluginDemo]
 *      // < Around Running [pluginTest Execution time: 20ms]
 *      //------------------------------------------------------------------
 *
 *     }
 * </pre>
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/2/11 10:24
 * @see PluginGenerate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface Around {

    /**
     * 增强逻辑生效的SpEL条件表达式
     * <pre>
     *     例如：
     *     1.增强被某个注解标注的方法：<b>#{#hasAnn($mec$, 'xxxx.xxx.xx.YouAnnotation')}</b>
     *     2.增强方法名称为xxx的方法：<b>#{'YouMethodName' eq $method$.getName()}</b>
     *     3.增强某类中所有的方法：   <b>#{T(xxx.xxx.xxx.YouClass) == $class$}</b>
     * </pre>
     */
    String value();

    /**
     * 增强插件的SpEL初始化方法表达式
     */
    String init() default "";
}
