package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

/**
 * 条件转换配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/30 16:20
 */
@SpelConfig(prefix = "", scope = ConfigScope.BOTH, description = "条件结果转换配置，作为 condition-convert 的列表元素使用")
public class Condition {

    /**
     * 断言条件
     */
    @SpelAttribute(key = "assertion", kind = SpelKind.CONDITION, resultType = Boolean.class)
    private String assertion;

    /**
     * 响应结果
     */
    @SpelAttribute(key = "result", kind = SpelKind.EXPRESSION)
    private String result;

    /**
     * 异常结果
     */
    @SpelAttribute(key = "exception", kind = SpelKind.EXPRESSION)
    private String exception;

    /**
     * 获取断言条件
     *
     * @return 断言条件
     */
    public String getAssertion() {
        return assertion;
    }

    /**
     * 设置断言条件
     *
     * @param assertion 断言条件
     */
    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * 获取响应结果
     *
     * @return 响应结果
     */
    public String getResult() {
        return result;
    }

    /**
     * 设置响应结果
     *
     * @param result 响应结果
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * 获取异常结果
     *
     * @return 异常结果
     */
    public String getException() {
        return exception;
    }

    /**
     * 设置异常结果
     *
     * @param exception 异常结果
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * 是否是有效的配置
     *
     * @return 是否是有效配置
     */
    public boolean effective() {
        return StringUtils.hasText(assertion) &&
                (StringUtils.hasText(result) || StringUtils.hasText(exception));

    }
}
