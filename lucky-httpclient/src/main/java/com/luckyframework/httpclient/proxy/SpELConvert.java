package com.luckyframework.httpclient.proxy;

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

    private final SpELRuntime spELRuntime;

    public SpELConvert(SpELRuntime spELRuntime) {
        this.spELRuntime = spELRuntime;
    }

    public SpELConvert(){
        this(new SpELRuntime());
    }

    public SpELRuntime getSpELRuntime() {
        return spELRuntime;
    }

    public SpELConvert importPackage(String... packageNames) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        for (String  packageName : packageNames) {
            commonParams.importPackage(packageName);
        }
        return this;
    }

    public SpELConvert importPackage(Class<?> aClass) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        commonParams.importPackage(aClass.getPackage().getName());
        return this;
    }

    public SpELConvert addCommonParam(String paramName, Object paramValue) {
        ParamWrapper commonParams = spELRuntime.getCommonParams();
        commonParams.addVariable(paramName, paramValue);
        return this;
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     * @param spELExpression SpEL表达式
     * @return 解析结果
     */
    public Object parseExpression(String spELExpression) {
        return spELRuntime.getValueForType(
                new ParamWrapper(spELExpression)
                        .setRootObject(HttpClientProxyObjectFactory.getExpressionParams())
                        .setParserContext(new TemplateParserContext()));
    }

    /**
     * 解析SpEL表达式，被#{}包裹的将被视为SpEL表达式去解析
     * @param spELExpression SpEL表达式
     * @param variables 参数部分
     * @return 解析结果
     */
    public Object parseExpression(String spELExpression, Map<String, Object> variables) {
        return spELRuntime.getValueForType(
                new ParamWrapper(spELExpression)
                        .setParserContext(new TemplateParserContext())
                        .setRootObject(HttpClientProxyObjectFactory.getExpressionParams())
                        .setVariables(variables));

    }
}
