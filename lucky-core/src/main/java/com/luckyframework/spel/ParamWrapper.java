package com.luckyframework.spel;

import com.luckyframework.cache.Cache;
import com.luckyframework.cache.impl.LRUCache;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpEL执行参数包装器
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 09:49
 */
public class ParamWrapper {

    /**
     * 表达式缓存，采用LRU缓存进行存储
     */
    private static final Cache<String, Expression> exCacheMap = new LRUCache<>(225);

    /**
     * 已知的包前缀
     */
    private final List<String> knownPackagePrefixes = new ArrayList<>(32);
    /**
     * 表达式
     */
    private String expression;
    /**
     * 内容解析器
     */
    private ParserContext parserContext;
    /**
     * 上下文工厂
     */
    private EvaluationContextFactory contextFactory;
    /**
     * 根对象
     */
    private Object rootObject;
    /**
     * 变量表
     */
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    /**
     * 期望的结果类型
     */
    private ResolvableType expectedResultType;


    public ParamWrapper() {

    }

    public ParamWrapper(String expression) {
        this.expression = expression;
    }

    public ParamWrapper(ParamWrapper paramWrapper) {
        this.knownPackagePrefixes.addAll(paramWrapper.getKnownPackagePrefixes());
        this.expression = paramWrapper.getExpression();
        this.parserContext = paramWrapper.getParserContext();
        this.rootObject = paramWrapper.getRootObject();
        this.variables.putAll(paramWrapper.getVariables());
        this.expectedResultType = paramWrapper.getExpectedResultType();
        this.contextFactory = paramWrapper.getContextFactory();
    }

    /**
     * 创建一个SpEL表达式对象，会优先到缓存中获取，缓存中没有才会去创建
     *
     * @param expression SpEL表达式
     * @param parserContext 解析上下文
     * @return SpEL表达式对象
     */
    public static Expression createExpression(String expression, ParserContext parserContext) {
        Expression expr = exCacheMap.get(expression);
        if (expr == null) {
            expr = new SpelExpressionParser().parseExpression(expression, parserContext);
            exCacheMap.put(expression, expr);
        }
        return expr;
    }

    /**
     * 设置SpEL表达式
     *
     * @param expression SpEL表达式
     */
    public ParamWrapper setExpression(String expression) {
        this.expression = expression;
        return this;
    }

    /**
     * 设置{@link ParserContext}对象
     *
     * @param parserContext ParserContext对象
     */
    public ParamWrapper setParserContext(ParserContext parserContext) {
        this.parserContext = parserContext;
        return this;
    }

    /**
     * 设置{@link EvaluationContextFactory}对象
     *
     * @param contextFactory ParserContext对象
     */
    public ParamWrapper setContextFactory(EvaluationContextFactory contextFactory) {
        this.contextFactory = contextFactory;
        return this;
    }

    /**
     * 设置预期的返回值结果类型
     *
     * @param type 预期的返回值结果类型
     */
    public ParamWrapper setExpectedResultType(ResolvableType type) {
        this.expectedResultType = type;
        return this;
    }

    /**
     * 设置预期的返回值结果类型
     *
     * @param type 预期的返回值结果类型
     */
    public ParamWrapper setExpectedResultType(Type type) {
        return setExpectedResultType(ResolvableType.forType(type));
    }

    /**
     * 设置预期的返回值结果类型
     *
     * @param type 预期的返回值结果类型
     */
    public ParamWrapper setExpectedResultType(Class<?> type) {
        return setExpectedResultType(ResolvableType.forRawClass(type));
    }

    /**
     * 设置预期的返回值结果类型
     *
     * @param typeToken 预期的返回值结果类型
     */
    public ParamWrapper setExpectedResultType(SerializationTypeToken<?> typeToken) {
        return setExpectedResultType(ResolvableType.forType(typeToken.getType()));
    }

    /**
     * 导入依赖包
     *
     * @param packagePrefixes 依赖包
     */
    public ParamWrapper importPackage(String... packagePrefixes) {
        for (String packagePrefix : packagePrefixes) {
            packagePrefix = packagePrefix.trim();
            if (!knownPackagePrefixes.contains(packagePrefix)) {
                knownPackagePrefixes.add(packagePrefix);
            }
        }
        return this;
    }

    /**
     * 从注解元素中导入依赖包
     *
     * @param annotatedElements 解元素数组
     */
    public ParamWrapper importPackage(AnnotatedElement... annotatedElements) {
        return importPackage(Arrays.asList(annotatedElements));
    }

    /**
     * 从注解元素中导入依赖包
     *
     * @param annotatedElements 解元素集合
     */
    public ParamWrapper importPackage(Collection<AnnotatedElement> annotatedElements) {
        for (AnnotatedElement annotatedElement : annotatedElements) {
            SpELImport spELImport = AnnotationUtils.getMergedAnnotation(annotatedElement, SpELImport.class);
            if (spELImport != null && !ContainerUtils.isEmptyArray(spELImport.packages())) {
                importPackage(spELImport.packages());
            }
        }
        return this;
    }

    /**
     * 清除掉所有导入的包
     */
    public void clearImportPackage() {
        knownPackagePrefixes.clear();
    }

    /**
     * 获取期望的返回值结果类型
     *
     * @return 期望的返回值结果类型
     */
    public ResolvableType getExpectedResultType() {
        return expectedResultType;
    }

    /**
     * 获取所有注册的依赖包前缀
     *
     * @return 所有依赖包前缀
     */
    public List<String> getKnownPackagePrefixes() {
        return knownPackagePrefixes;
    }

    /**
     * 获取SpEL表达式
     *
     * @return SpEL表达式
     */
    public String getExpression() {
        return expression;
    }

    /**
     * 获取{@link ParserContext}对象
     *
     * @return ParserContext对象
     */
    public ParserContext getParserContext() {
        return parserContext;
    }

    /**
     * 获取上下文工厂{@link EvaluationContextFactory}
     *
     * @return 上下文工厂
     */
    public EvaluationContextFactory getContextFactory() {
        return contextFactory;
    }

    /**
     * 获取SpEL表达式实例
     *
     * @return SpEL表达式实例
     */
    public Expression getExpressionInstance() {
        Assert.notNull(expression, "expression is null.");
        return createExpression(expression, parserContext);
    }

    /**
     * 获取根(root)对象
     *
     * @return 根(root)对象
     */
    public Object getRootObject() {
        return rootObject;
    }

    /**
     * 获取变量列表
     *
     * @return 变量列表
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * 设置根(root)对象
     *
     * @param rootObject 根(root)对象
     */
    public ParamWrapper setRootObject(@Nullable Object rootObject) {
        this.rootObject = rootObject;
        return this;
    }

    /**
     * 添加一个变量
     *
     * @param variableName  变量名
     * @param variableValue 变量值
     */
    public ParamWrapper addVariable(@NonNull String variableName, @Nullable Object variableValue) {
        this.variables.put(variableName, variableValue);
        return this;
    }

    /**
     * 添加一组变量
     *
     * @param variables 变量列表
     */
    public ParamWrapper addVariables(@NonNull Map<String, Object> variables) {
        this.variables.putAll(variables);
        return this;
    }

    /**
     * 设置一组变量
     *
     * @param variables 变量列表
     */
    public ParamWrapper setVariables(@NonNull Map<String, Object> variables) {
        this.variables.clear();
        this.variables.putAll(variables);
        return this;
    }

    /**
     * 将方法参数列表设置为变量
     *
     * @param method           方法实例
     * @param methodParameters 参数列表
     */
    public ParamWrapper addVariables(@NonNull Method method, @NonNull Object[] methodParameters) {
        if (method.getParameterCount() != methodParameters.length) {
            throw new IllegalArgumentException("Method parameters must be same length.");
        }
        Map<String, Object> paramNameValueMap = MethodUtils.getMethodParamsNV(method, methodParameters);
        int i = 0;
        for (Map.Entry<String, Object> entry : paramNameValueMap.entrySet()) {
            Object arg = entry.getValue();
            addVariable(entry.getKey(), arg);
            addVariable("args" + i, arg);
            addVariable("p" + i, arg);
            addVariable("a" + i, arg);
            i++;
        }
        return this;
    }

    /**
     * 将方法参数列表设置为Root对象
     *
     * @param method           方法实例
     * @param methodParameters 参数列表
     */
    public ParamWrapper setRootObject(@NonNull Method method, @NonNull Object[] methodParameters) {
        return setRootObject(method, methodParameters, Collections.EMPTY_MAP);
    }

    /**
     * 将方法参数列表设置为Root对象,并添加一些额外的参数
     *
     * @param method           方法实例
     * @param methodParameters 参数列表
     * @param extraVariables   额外的参数
     */
    public ParamWrapper setRootObject(@NonNull Method method, @NonNull Object[] methodParameters, Map<String, Object> extraVariables) {
        if (method.getParameterCount() != methodParameters.length) {
            throw new IllegalArgumentException("Method parameters must be same length.");
        }
        Map<String, Object> paramNameValueMap = MethodUtils.getMethodParamsNV(method, methodParameters);
        Map<String, Object> realNameValueMap = new HashMap<>(extraVariables);
        realNameValueMap.putAll(paramNameValueMap);
        int i = 0;
        for (Map.Entry<String, Object> entry : paramNameValueMap.entrySet()) {
            Object arg = entry.getValue();
            realNameValueMap.put("args" + i, arg);
            realNameValueMap.put("p" + i, arg);
            realNameValueMap.put("a" + i, arg);
            i++;
        }
        setRootObject(realNameValueMap);
        return this;
    }

    /**
     * 将执行结果转化为预期的类型
     *
     * @param result SpEL表达式的执行结果
     * @param <T>    结果类型泛型
     * @return 预期的类型的结果
     */
    @SuppressWarnings("unchecked")
    public <T> T conversionToExpectedResult(Object result) {
        return expectedResultType == null ? (T) result : (T) ConversionUtils.conversion(result, expectedResultType);
    }

}
