package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
import org.springframework.core.io.Resource;

/**
 * 条件请求体
 * @author fukang
 * @version 1.0.0
 * @date 2026/5/10 01:21
 */
@SpelConfig(prefix = "", scope = ConfigScope.BOTH, description = "条件请求体，作为 condition-body 的列表元素使用")
public class ConditionBody {
    /**
     * 条件表达式
     */
    @SpelAttribute(key = "condition", kind = SpelKind.CONDITION, resultType = Boolean.class)
    private String condition;

    /**
     * 请求体表达式，只支持{@link Resource}、{@link String}类型结果
     */
    @SpelAttribute(key = "body", kind = SpelKind.EXPRESSION, resultType = String.class, description = "请求体表达式，结果可为 Resource 或 String")
    private String body;

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
     * 获取请求体表达式，只支持{@link Resource}、{@link String}类型结果
     *
     * @return 请求体表达式，只支持{@link Resource}、{@link String}类型结果
     */
    public String getBody() {
        return body;
    }

    /**
     * 设置请求体表达式，只支持{@link Resource}、{@link String}类型结果
     *
     * @param body 请求体表达式，只支持{@link Resource}、{@link String}类型结果
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * 是否是有效的配置
     *
     * @return 是否是有效配置
     */
    public boolean effective() {
        return StringUtils.hasText(condition) && StringUtils.hasText(body);
    }
}
