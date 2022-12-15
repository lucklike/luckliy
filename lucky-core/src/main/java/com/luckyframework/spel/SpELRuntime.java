package com.luckyframework.spel;

import com.luckyframework.common.CommonUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.ResolvableType;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;

import java.util.ArrayList;
import java.util.List;

/**
 * SpEL运行时环境
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/31 10:56
 */
public class SpELRuntime {

    /** 默认的公共执行上下文工厂*/
    private final EvaluationContextFactory defaultEvaluationContextFactory;
    /** 公共参数*/
    private final ParamWrapper commonParams;

    public SpELRuntime(EvaluationContextFactory defaultEvaluationContextFactory, ParamWrapper commonParams) {
        this.defaultEvaluationContextFactory = defaultEvaluationContextFactory;
        this.commonParams = commonParams;
    }

    public SpELRuntime(EvaluationContextFactory defaultEvaluationContextFactory) {
        this(defaultEvaluationContextFactory, new ParamWrapper());
    }

    public SpELRuntime(ParamWrapper commonParams){
        this(EvaluationContextFactory.DEFAULT_FACTORY, commonParams);
    }

    public SpELRuntime(){
        this(EvaluationContextFactory.DEFAULT_FACTORY, new ParamWrapper());
    }


    /**
     * 获取SpEL运行时环境的公共参数
     * @return SpEL运行时环境的公共参数
     */
    public ParamWrapper getCommonParams() {
        return commonParams;
    }

    //-------------------------------------------------------------------------
    //                         get value for type
    //-------------------------------------------------------------------------

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param env   上下文工厂
     * @param pw    执行参数
     * @return      定类型的结果
     * @param <T>   类型泛型
     */
    public <T> T getValueForType(EvaluationContextFactory env, ParamWrapper pw){
        EvaluationContextFactory realEnv = env == null ? defaultEvaluationContextFactory : env;
        ParamWrapper paramWrapper = getParamWrapper(pw);
        EvaluationContext spELContext = getSpELContext(realEnv, paramWrapper);
        Object result = paramWrapper.getExpressionInstance().getValue(spELContext);
        return paramWrapper.conversionToExpectedResult(result);
    }

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param pw    执行参数
     * @return      定类型的结果
     * @param <T>   类型泛型
     */
    public <T> T getValueForType(ParamWrapper pw){
        return getValueForType(null, pw);
    }

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param expression    SpEL表达式
     * @param type          结果类型
     * @return              定类型的结果
     * @param <T>           类型泛型
     */
    public <T> T getValueForType(String expression, Class<T> type){
        return getValueForType(new ParamWrapper().setExpression(expression).setExpectedResultType(type));
    }

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param expression    SpEL表达式
     * @param type          结果类型
     * @return              定类型的结果
     * @param <T>           类型泛型
     */
    public <T> T getValueForType(String expression, ResolvableType type){
        return getValueForType(new ParamWrapper().setExpression(expression).setExpectedResultType(type));
    }

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param expression    SpEL表达式
     * @param typeToken     结果类型
     * @return              定类型的结果
     * @param <T>           类型泛型
     */
    public <T> T getValueForType(String expression, SerializationTypeToken<T> typeToken){
        return getValueForType(new ParamWrapper().setExpression(expression).setExpectedResultType(typeToken));
    }

    /**
     * 执行一个SpEL表达式，并返回指定类型的结果
     * @param expression    SpEL表达式
     * @return              定类型的结果
     * @param <T>           类型泛型
     */
    public <T> T getValueForType(String expression){
        return getValueForType(new ParamWrapper().setExpression(expression));
    }


    //-------------------------------------------------------------------------
    //                              set value
    //-------------------------------------------------------------------------

    /**
     * 使用SpEL表达式对目标对象进行赋值操作
     * @param env       上下文工厂
     * @param pw        执行参数(该执行参数的rootObject必须设置)
     * @param setValue  要设置的值
     */
    public void setValue(EvaluationContextFactory env, ParamWrapper pw, Object setValue){
        EvaluationContextFactory realEnv = env == null ? defaultEvaluationContextFactory : env;
        ParamWrapper paramWrapper = getParamWrapper(pw);
        EvaluationContext spELContext = getSpELContext(realEnv, paramWrapper);
        paramWrapper.getExpressionInstance().setValue(spELContext, setValue);
    }

    /**
     * 使用SpEL表达式对目标对象进行赋值操作
     * @param pw        执行参数(该执行参数的rootObject必须设置)
     * @param setValue  要设置的值
     */
    public void setValue(ParamWrapper pw, Object setValue){
        setValue(null, pw, setValue);
    }

    /**
     * 使用SpEL表达式对目标对象进行赋值操作
     * @param instance      目标对象
     * @param expression    赋值表达式
     * @param value         要设置的值
     */
    public void setValue(Object instance, String expression, Object value){
        setValue(new ParamWrapper().setExpression(expression).setRootObject(instance), value);
    }

    //-------------------------------------------------------------------------
    //                          invoke  method
    //-------------------------------------------------------------------------

    /**
     * 执行一个SpEL方法表达式
     * @param env   上下文工厂
     * @param pw    执行参数
     */
    public void invokeMethod(EvaluationContextFactory env, ParamWrapper pw){
        EvaluationContextFactory realEnv = env == null ? defaultEvaluationContextFactory : env;
        ParamWrapper paramWrapper = getParamWrapper(pw);
        EvaluationContext spELContext = getSpELContext(realEnv, paramWrapper);
        pw.getExpressionInstance().getValue(spELContext);
    }

    /**
     * 执行一个SpEL方法表达式
     * @param pw    执行参数
     */
    public void invokeMethod(ParamWrapper pw){
        invokeMethod(null, pw);
    }


    /**
     * 创建SpEL执行上下文对象
     * @param env   上下文工厂
     * @param pw    执行参数
     * @return      执行上下文对象
     */
    private EvaluationContext getSpELContext(EvaluationContextFactory env, ParamWrapper pw){
        EvaluationContext context = env.getEvaluationContext();
        if(context instanceof StandardEvaluationContext){
            StandardEvaluationContext standardContext = (StandardEvaluationContext) context;
            standardContext.setTypeLocator(getTypeLocator(pw));
            standardContext.setVariables(pw.getVariables());
            standardContext.setRootObject(pw.getRootObject());
        }
        return context;
    }

    /**
     * 创建类型定位器
     * @param pw    执行参数
     * @return      类型定位器
     */
    private StandardTypeLocator getTypeLocator(ParamWrapper pw){
        StandardTypeLocator typeLocator = new StandardTypeLocator(ClassUtils.getDefaultClassLoader());
        pw.getKnownPackagePrefixes().forEach(typeLocator::registerImport);
        return typeLocator;
    }

    /**
     * 将公共参数与用户参数合成一个最终的执行参数
     * @param userParamWrapper  用户设置的执行参数
     * @return  最终执行参数
     */
    private ParamWrapper getParamWrapper(ParamWrapper userParamWrapper){
        ParamWrapper realPw = new ParamWrapper(commonParams);
        List<String> commonPackages = new ArrayList<>(realPw.getKnownPackagePrefixes());

        // 导包操作, 保证用户手动设置的包的优先级要高于公共包
        realPw.clearImportPackage();
        userParamWrapper.getKnownPackagePrefixes().forEach(realPw::importPackage);
        commonPackages.forEach(realPw::importPackage);

        CommonUtils.trueIsRunning(userParamWrapper.getExpression() != null, () -> realPw.setExpression(userParamWrapper.getExpression()));
        CommonUtils.trueIsRunning(userParamWrapper.getRootObject() != null, () -> realPw.setRootObject(userParamWrapper.getRootObject()));
        CommonUtils.trueIsRunning(userParamWrapper.getExpectedResultType() != null, () -> realPw.setExpectedResultType(userParamWrapper.getExpectedResultType()));
        realPw.addVariables(userParamWrapper.getVariables());
        return realPw;
    }
}
