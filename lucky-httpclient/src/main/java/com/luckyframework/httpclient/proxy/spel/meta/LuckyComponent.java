package com.luckyframework.httpclient.proxy.spel.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个类/接口是lucky-httpclient组件，并指定组件类型。
 *
 * <p>本注解只给IDEA插件使用，是纯静态标记：框架运行期从不读取，不改变任何行为。
 * 插件据此识别哪些文件属于lucky-httpclient框架组件，并支撑标记（图标）、
 * 聚合（组件清单）、搜索（按类型/名称定位）与调试（跳到相关SpEL元数据）。
 *
 * <p>识别入口有三处，满足任意一处即视为组件：
 * <ul>
 *     <li>直接标在类/接口上，声明该类型本身是组件；</li>
 *     <li>直接标在类中的方法上，表示该方法是组件入口，所在类也是组件；</li>
 *     <li>标在注解声明上作为元注解，由使用该注解的类/接口/方法继承组件身份。
 *         例如{@code @HttpRequest}上标{@code type = API}后，所有带
 *         {@code @HttpRequest}的接口，以及带以{@code @HttpRequest}为元注解的
 *         {@code @Get}/{@code @Post}等注解的接口，都是{@code API}组件。</li>
 * </ul>
 *
 * <p>类型判定顺序：类/接口上直接标注的{@code @LuckyComponent}优先；其次取类上
 * 注解的元注解链最近一层；再次取方法上（含方法注解的元注解链）的声明；都没有时
 * 才按{@link LuckyComponentType#AUTO}的结构推断规则处理，推断不出则不是组件。
 *
 * <p>展示名从Javadoc第一段首句读取，读不到时使用简单类名，因此本注解不提供
 * {@code value}/{@code name}属性。
 *
 * <p>约束：
 * <ul>
 *     <li>不加{@code @Inherited}：子类不会因为父类标注而成为组件；</li>
 *     <li>不参与Spring容器：它不是{@code @Component}的替代品，不触发Bean注册。</li>
 * </ul>
 *
 * <p>本注解只作为静态元数据供IDE插件读取，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuckyComponent {

    /**
     * 组件类型，默认按类结构自动推断。
     */
    LuckyComponentType type() default LuckyComponentType.AUTO;

    /**
     * 分组名称，用于决定IDEA插件中的文件夹名字
     */
    String group() default "httpclient";

    /**
     * 分组优先级,数字越小优先级越大，插件在展示分组时越靠前
     */
    double priority() default Double.MAX_VALUE;
}
