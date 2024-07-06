package com.luckyframework.expression;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.exception.BeanExpressionException;
import com.luckyframework.exception.BeansException;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/18 00:18
 */
public class StandardBeanExpressionResolver {
    /** Default expression prefix: "#{". */
    public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

    /** Default expression suffix: "}". */
    public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


    private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

    private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

    private ExpressionParser expressionParser;

    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256);

    private static StandardEvaluationContext evaluationContext;

    private final ParserContext beanExpressionParserContext = new ParserContext() {
        @Override
        public boolean isTemplate() {
            return true;
        }
        @Override
        public String getExpressionPrefix() {
            return expressionPrefix;
        }
        @Override
        public String getExpressionSuffix() {
            return expressionSuffix;
        }
    };

    /**
     * Create a new {@code StandardBeanExpressionResolver} with default settings.
     */
    public StandardBeanExpressionResolver() {
        this.expressionParser = new SpelExpressionParser();
    }

    /**
     * Create a new {@code StandardBeanExpressionResolver} with the given bean class loader,
     * using it as the basis for expression compilation.
     * @param beanClassLoader the factory's bean class loader
     */
    public StandardBeanExpressionResolver(@Nullable ClassLoader beanClassLoader) {
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
    }


    public void initializeStandardEvaluationContext(BeanFactory beanFactory, Environment environment){
        if(evaluationContext == null){
            evaluationContext = (StandardEvaluationContext) new BeanFactoryEvaluationContextFactory(beanFactory).getDefaultEvaluationContext();
            evaluationContext.setRootObject(beanFactory);
            evaluationContext.setVariable("env", environment);
            customizeEvaluationContext(evaluationContext);
        }
    }

    public boolean isExpression(String expression){
        expression = expression.trim();
        return expression.startsWith(expressionPrefix) && expression.endsWith(expressionSuffix);
    }

    /**
     * Set the prefix that an expression string starts with.
     * The default is "#{".
     * @see #DEFAULT_EXPRESSION_PREFIX
     */
    public void setExpressionPrefix(String expressionPrefix) {
        Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
        this.expressionPrefix = expressionPrefix;
    }

    /**
     * Set the suffix that an expression string ends with.
     * The default is "}".
     * @see #DEFAULT_EXPRESSION_SUFFIX
     */
    public void setExpressionSuffix(String expressionSuffix) {
        Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
        this.expressionSuffix = expressionSuffix;
    }

    /**
     * Specify the EL parser to use for expression parsing.
     * <p>Default is a {@link org.springframework.expression.spel.standard.SpelExpressionParser},
     * compatible with standard Unified EL style expression syntax.
     */
    public void setExpressionParser(ExpressionParser expressionParser) {
        Assert.notNull(expressionParser, "ExpressionParser must not be null");
        this.expressionParser = expressionParser;
    }

    @Nullable
    public Object evaluate(@Nullable String value) throws BeansException {
        if (!StringUtils.hasLength(value)) {
            return value;
        }
        try {
            Expression expr = this.expressionCache.get(value);
            if (expr == null) {
                expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
                this.expressionCache.put(value, expr);
            }
            return expr.getValue(evaluationContext);
        }
        catch (Throwable ex) {
            throw new BeanExpressionException("Expression parsing failed", ex);
        }
    }

    @Nullable
    public <T> T evaluate(@Nullable String value, Class<T> typeOf) throws BeansException {
        return ConversionUtils.conversion(evaluate(value),typeOf);
    }

    @Nullable
    public Object evaluate(@Nullable String value, ResolvableType resolvableType) throws BeansException {
        return ConversionUtils.conversion(evaluate(value),resolvableType);
    }

    @Nullable
    public Object evaluate(@Nullable String value, Type type) throws BeansException {
        return ConversionUtils.conversion(evaluate(value),type);
    }

    @Nullable
    public <T> T evaluate(@Nullable String value, SerializationTypeToken<T> typeToken) throws BeansException {
        return ConversionUtils.conversion(evaluate(value),typeToken);
    }

    /**
     * Template method for customizing the expression evaluation context.
     * <p>The default implementation is empty.
     */
    protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
    }
}
