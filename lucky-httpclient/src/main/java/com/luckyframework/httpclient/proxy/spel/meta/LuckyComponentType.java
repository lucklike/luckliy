package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * Lucky组件类型。
 *
 * <p>由{@link LuckyComponent#type()}声明，只给IDEA插件读取：
 * 直接决定文件级图标与颜色，不影响任何运行期行为。{@link #AUTO}表示按类结构推断，
 * 推断不出时由插件侧使用兜底值{@code OTHER}，用户不能书写{@code OTHER}。
 *
 * <p>框架内的身份注解：{@code @HttpRequest}/{@code @ServerAddressMeta}/{@code @ApiConfig} → API，
 * {@code @SpelConfig} → CONFIG，{@code @Namespace} → FUNCTION；SUPPORT 没有对应注解，只靠结构推断。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
public enum LuckyComponentType {

    /**
     * 自动推断。接口上有{@code @HttpRequest}/{@code @Get}/{@code @Post}等 → API；
     * 类上有{@code @SpelConfig} → CONFIG；{@code @Namespace}类或含静态{@code @FunctionAlias}方法 → FUNCTION；
     * 实现{@code ObjectGenerate}/{@code ObjectFactory}/{@code HttpClientProxyObjectFactory}、
     * Hook处理器、Mock实现等 → SUPPORT；都不满足 → OTHER。
     */
    AUTO,

    /**
     * HTTP API接口。
     */
    API,

    /**
     * 数据库接口
     */
    DATABASE,

    /**
     * 配置模型，典型特征是类上有{@code @SpelConfig}。
     */
    CONFIG,

    /**
     * SpEL函数库，典型特征是{@code @Namespace}类或静态{@code @FunctionAlias}方法所在类。
     */
    FUNCTION,

    /**
     * 支撑类，例如对象生成器、对象工厂、Hook处理器、Mock实现等。
     */
    SUPPORT


}
