package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * 配置模型参与的配置层级。
 *
 * <p>由{@link SpelConfig#scope()}声明，只供IDE插件读取：类级配置只能使用
 * {@link #CLASS}/{@link #BOTH}的模型，方法级配置只能使用{@link #METHOD}/{@link #BOTH}的模型。
 *
 * <p>本枚举只作为静态元数据，不改变框架运行期行为。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/18
 */
public enum ConfigScope {

    /**
     * 仅类级配置。
     */
    CLASS,

    /**
     * 仅方法级配置。
     */
    METHOD,

    /**
     * 类级与方法级配置共用。
     */
    BOTH
}
