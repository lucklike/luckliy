package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.expression.common.TemplateParserContext;

import java.util.Map;

/**
 * SpEL转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 16:30
 */
public class SpELConvert {

    /**
     * SpEL模版表达式内容
     */
    private final TemplateParserContext templateParserContext = new TemplateParserContext();

    /**
     * SpEL表达式运行时环境
     */
    private final SpELRuntime spELRuntime;

    public SpELConvert(SpELRuntime spELRuntime) {
        this.spELRuntime = spELRuntime;
    }

    public SpELConvert() {
        this(new SpELRuntime());
    }

    /**
     * 获取SpEL运行时环境
     *
     * @return SpEL运行时环境
     */
    public SpELRuntime getSpELRuntime() {
        return spELRuntime;
    }

    /**
     * 嵌套解析SpEL表达式
     * @param paramWrapper 参数包装器
     * @param <T>          结果泛型
     * @return SpEL表达式结果
     */
    public <T> T nestParseExpression(ParamWrapper paramWrapper) {
        paramWrapperPostProcess(paramWrapper);
        T value = spELRuntime.getValueForType(paramWrapper);
        while (needParse(value)) {
            value = nestParseExpression(paramWrapper.setExpression((String) value));
        }
        return value;
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     *
     * @param paramWrapper 参数包装器
     * @param <T>          结果泛型
     * @return SpEL表达式结果
     */
    public <T> T parseExpression(ParamWrapper paramWrapper) {
        paramWrapperPostProcess(paramWrapper);
        return spELRuntime.getValueForType(paramWrapper);
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     *
     * @param spELExpression SpEL表达式
     * @return 解析结果
     */
    public Object parseExpression(String spELExpression) {
        return parseExpression(new ParamWrapper(spELExpression));
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     *
     * @param spELExpression SpEL表达式
     * @param variables      参数部分
     * @return 解析结果
     */
    public Object parseExpression(String spELExpression, Map<String, Object> variables) {
        return parseExpression(new ParamWrapper(spELExpression).setVariables(variables));
    }

    /**
     * 获取SpEL表达式前缀
     *
     * @return SpEL表达式前缀
     */
    public String getExpressionPrefix() {
        return this.templateParserContext.getExpressionPrefix();
    }

    /**
     * 获取SpEL表达式后缀
     *
     * @return SpEL表达式后缀
     */
    public String getExpressionSuffix() {
        return this.templateParserContext.getExpressionSuffix();
    }

    /**
     * 参数包装器后置处理
     *
     * @param paramWrapper 参数包装器
     */
    protected void paramWrapperPostProcess(ParamWrapper paramWrapper) {
        paramWrapper.setParserContext(templateParserContext);
    }

    /**
     * 当前值是否还需要解析
     *
     * @param value 当前值
     * @return 当前值是否还需要解析
     */
    protected boolean needParse(Object value) {
        if (value instanceof String) {
            return isSpELExpression((String) value);
        }
        return false;
    }

    /**
     * 是否为SpEL表达式
     *
     * @param text 待判断的文本
     * @return 是否为SpEL表达式
     */
    protected boolean isSpELExpression(String text) {
        return isExpression(text, templateParserContext.getExpressionPrefix(), templateParserContext.getExpressionSuffix());
    }

    protected boolean isExpression(String text, String exPrefix, String exSuffix) {
        int start = text.indexOf(exPrefix);
        int end = text.lastIndexOf(exSuffix);
        return start != -1 && end != -1 && start < end;
    }

}
