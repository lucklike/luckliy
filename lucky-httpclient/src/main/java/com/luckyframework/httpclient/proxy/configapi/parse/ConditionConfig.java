package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

import java.util.Map;

/**
 * 支持条件的配置项
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/5/10 00:50
 */
@SpelConfig(prefix = "", scope = ConfigScope.BOTH, description = "条件配置项，作为 condition-*-params 的列表元素使用")
public class ConditionConfig {

    /**
     * 条件表达式
     */
    @SpelAttribute(key = "condition", kind = SpelKind.CONDITION, resultType = Boolean.class)
    private String condition;

    /**
     * 配置详情
     */
    @SpelAttribute(key = "configs", kind = SpelKind.MAP_EXPRESSION, keySpel = true, expandable = true, description = "参数名与参数值都参与SpEL解析，集合值展开为多个同名参数")
    private Map<String, Object> configs;

    /**
     * 获取条件表达式
     *
     * @return 条件表达式
     */
    public String getCondition() {
        return condition;
    }

    /**
     * 设置条件表达式
     *
     * @param condition 条件表达式
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * 获取配置详情
     *
     * @return 配置详情
     */
    public Map<String, Object> getConfigs() {
        return configs;
    }

    /**
     * 设置配置详情
     *
     * @param configs 配置详情
     */
    public void setConfigs(Map<String, Object> configs) {
        this.configs = configs;
    }

    /**
     * 是否是有效的配置
     *
     * @return 是否是有效配置
     */
    public boolean effective() {
        return StringUtils.hasText(condition) && ContainerUtils.isNotEmptyMap(configs);
    }
}
