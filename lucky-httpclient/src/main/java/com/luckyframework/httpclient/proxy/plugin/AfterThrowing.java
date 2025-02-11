package com.luckyframework.httpclient.proxy.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 定义后置增强逻辑的注解，只有在目标方法执行出现异常后才会执行
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
 *          // 后置增强
 *          // 1.方法返回值类型为void/Void时，执行玩增强逻辑之后依然会抛出原异常
 *          // 2.方法返回值类型为其他时，该返方法的回值会被当做代理方法的返回值处理
 *          @AfterThrowing(value = "#{#hasAnn($mec$, 'com.lucky.UseMyPlugin')}", init = "#{$this$.pluginInit($exeMeta$)}")
 *          public void afterThrowing(ExecuteMeta meta, Exception e) {
 *              Method method = meta.getMethod().getName();
 *              System.out.println("AfterThrowing Running [" + method + " Exception: " + e + "]");
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
 *      // < 【Exception Return】 Execute Http request
 *      // < AfterThrowing Running [pluginDemo Exception: java.lang.NullPointerException]
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
public @interface AfterThrowing {

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
