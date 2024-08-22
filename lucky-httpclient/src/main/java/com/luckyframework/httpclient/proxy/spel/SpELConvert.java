package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELRuntime;
import org.springframework.core.ResolvableType;
import org.springframework.expression.common.TemplateParserContext;

import java.util.Map;

/**
 * SpEL转换器,提供SpEL表达式解析功能
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 16:30
 */
public class SpELConvert {

    /**
     * 默认嵌套表达式前缀
     */
    public static final String DEFAULT_NEST_EXPRESSION_PREFIX = "``";

    /**
     * 默认嵌套表达式后缀
     */
    public static final String DEFAULT_NEST_EXPRESSION_SUFFIX = "``";

    /**
     * SpEL模版表达式内容
     */
    private final TemplateParserContext templateParserContext = new TemplateParserContext();

    /**
     * SpEL表达式运行时环境
     */
    private final SpELRuntime spELRuntime;

    /**
     * 嵌套表达式前缀
     */
    private final String nestExpressionPrefix;

    /**
     * 嵌套表达式后缀
     */
    private final String nestExpressionSuffix;

    /**
     * SpEL转换器构造函数
     *
     * @param spELRuntime          SpEL运行时环境
     * @param nestExpressionPrefix 嵌套表达式前缀
     * @param nestExpressionSuffix 嵌套表达式后缀
     */
    public SpELConvert(SpELRuntime spELRuntime, String nestExpressionPrefix, String nestExpressionSuffix) {
        this.spELRuntime = spELRuntime;
        this.nestExpressionPrefix = nestExpressionPrefix;
        this.nestExpressionSuffix = nestExpressionSuffix;
    }

    /**
     * SpEL转换器构造函数
     *
     * @param nestExpressionPrefix 嵌套表达式前缀
     * @param nestExpressionSuffix 嵌套表达式后缀
     */
    public SpELConvert(String nestExpressionPrefix, String nestExpressionSuffix) {
        this(new SpELRuntime(), nestExpressionPrefix, nestExpressionSuffix);
    }

    /**
     * SpEL转换器构造函数
     *
     * @param spELRuntime SpEL运行时环境
     */
    public SpELConvert(SpELRuntime spELRuntime) {
        this(spELRuntime, DEFAULT_NEST_EXPRESSION_PREFIX, DEFAULT_NEST_EXPRESSION_SUFFIX);
    }

    /**
     * SpEL转换器构造函数
     */
    public SpELConvert() {
        this(DEFAULT_NEST_EXPRESSION_SUFFIX, DEFAULT_NEST_EXPRESSION_SUFFIX);
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
     * 嵌套解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     *
     * @param paramWrapper 参数包装器
     * @param <T>          结果泛型
     * @return SpEL表达式结果
     */
    @SuppressWarnings("unchecked")
    public <T> T nestParseExpression(ParamWrapper paramWrapper) {
        paramWrapperPostProcess(paramWrapper);

        // 保存目标类型，并将ParamWrapper中的类型设置为Object
        ResolvableType resultType = paramWrapper.getExpectedResultType();
        paramWrapper.setExpectedResultType(Object.class);

        Object value = spELRuntime.getValueForType(paramWrapper);
        while (needParse(value)) {
            value = nestParseExpression(paramWrapper.setExpression((String) value));
        }
        return (T) ConversionUtils.conversion(value, resultType);
    }

    /**
     * 不进行嵌套解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     *
     * @param paramWrapper 参数包装器
     * @param <T>          结果泛型
     * @return SpEL表达式结果
     */
    public <T> T notNestParseExpression(ParamWrapper paramWrapper) {
        paramWrapperPostProcess(paramWrapper);
        return spELRuntime.getValueForType(paramWrapper);
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value #DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value #DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}  ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``  ->  表示不需要使用嵌套解析}
     * </pre>
     *
     * @param paramWrapper 参数包装器
     * @param <T>          结果泛型
     * @return SpEL表达式结果
     */
    public <T> T parseExpression(ParamWrapper paramWrapper) {
        String expression = paramWrapper.getExpression();
        if (expression != null && expression.startsWith(nestExpressionPrefix) && expression.endsWith(nestExpressionSuffix)) {
            expression = expression.substring(nestExpressionPrefix.length(), expression.length() - nestExpressionSuffix.length());
            paramWrapper.setExpression(expression);
            return nestParseExpression(paramWrapper);
        } else {
            return notNestParseExpression(paramWrapper);
        }
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value #DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value #DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}  ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``  ->  表示不需要使用嵌套解析}
     * </pre>
     *
     * @param spELExpression SpEL表达式
     * @return SpEL表达式结果
     */
    public Object parseExpression(String spELExpression) {
        return parseExpression(new ParamWrapper(spELExpression));
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     * <pre>
     * 根据表达式是否以嵌套表达式前缀{@value #DEFAULT_NEST_EXPRESSION_PREFIX}开头
     * 以及是否以嵌套表达式后缀{@value #DEFAULT_NEST_EXPRESSION_SUFFIX}结尾
     * 来决定是否启用嵌套解析
     * eg:
     * {@code #{expression}  ->  表示不需要使用嵌套解析}
     * {@code  ``#{expression}``  ->  表示不需要使用嵌套解析}
     * </pre>
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

    /**
     * 判断某个字符串中是否包含某个表达式
     *
     * @param text     待判断的字符串
     * @param exPrefix 表达式前缀
     * @param exSuffix 表达式后缀
     * @return 是否包含表达式
     */
    protected boolean isExpression(String text, String exPrefix, String exSuffix) {
        int start = text.indexOf(exPrefix);
        int end = text.lastIndexOf(exSuffix);
        return start != -1 && end != -1 && start < end;
    }

}
