package com.luckyframework.httpclient.generalapi.ai;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.Resources;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

import java.util.Map;

/**
 * AI能力的连接配置入口。
 * <pre>
 *     厂商地址、鉴权密钥、MCP的连接命令与请求头等连接信息一律来自配置文件或编程式配置，
 *     不允许出现在代码常量里。配置装载后以SpEL Root变量 {@value #ROOT_VARIABLE_NAME}
 *     的名义注册进代理工厂，注解上的 {@code #{ai.openai.baseUrl}} 这类表达式即可取到配置值，
 *     于是 {@code @ServerAddress} 与 {@code @StaticHeader} 只需要声明配置项的位置。
 *
 *     配置文件支持properties/yml/json/xml，键名使用点号路径（与框架既有配置读取风格一致）：
 *     <code>
 *     ai:
 *       openai:
 *         baseUrl: https://api.openai.com/v1/
 *         apiKey: ${from-your-secret-store}
 *     </code>
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public final class AiConfiguration {

    /**
     * 注册到SpEL运行时环境时使用的Root变量名
     */
    public static final String ROOT_VARIABLE_NAME = "ai";

    private final ConfigurationMap config;

    private AiConfiguration(ConfigurationMap config) {
        this.config = config;
    }

    /**
     * 创建一个空配置，后续用 {@link #put(String, Object)} 逐项填充
     *
     * @return 配置对象
     */
    public static AiConfiguration create() {
        return new AiConfiguration(new ConfigurationMap());
    }

    /**
     * 从配置文件读取，支持properties/yml/json/xml四种资源格式
     * <pre>
     *     文件里写不写顶层的 {@value #ROOT_VARIABLE_NAME} 前缀都可以：
     *     存在该键时取其子树作为配置，不存在时整个文件内容就是 {@value #ROOT_VARIABLE_NAME} 的内容。
     * </pre>
     *
     * @param resourceLocation 资源位置，例如 {@code classpath:ai/openai/api-config.yml}
     * @return 配置对象
     */
    @SuppressWarnings("unchecked")
    public static AiConfiguration load(String resourceLocation) {
        ConfigurationMap config = Resources.resourceAsConfigMap(resourceLocation);
        Object root = config.getProperty(ROOT_VARIABLE_NAME);
        if (root instanceof Map) {
            return new AiConfiguration(new ConfigurationMap((Map<String, Object>) root));
        }
        return new AiConfiguration(config);
    }

    /**
     * 写入一项配置，键名支持点号路径
     *
     * @param path  配置路径，如 {@code openai.baseUrl}
     * @param value 配置值
     * @return 自身
     */
    public AiConfiguration put(String path, Object value) {
        this.config.addProperty(path, value);
        return this;
    }

    /**
     * 读取一项文本配置
     *
     * @param path 配置路径
     * @return 配置值，不存在时返回null
     */
    public String getString(String path) {
        Object value = this.config.getProperty(path);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 把本配置注册进代理工厂的SpEL运行时环境，之后注解表达式即可引用
     *
     * @param factory 代理工厂
     * @return 自身
     */
    public AiConfiguration install(HttpClientProxyObjectFactory factory) {
        factory.addSpringElRootVariable(ROOT_VARIABLE_NAME, this.config);
        return this;
    }

    /**
     * 获取底层配置Map，只读用途
     *
     * @return 配置Map
     */
    public ConfigurationMap asMap() {
        return this.config;
    }
}
