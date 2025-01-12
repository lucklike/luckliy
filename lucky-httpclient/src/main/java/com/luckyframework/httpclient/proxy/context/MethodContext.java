package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.exeception.WrapperMethodInvokeException;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.spel.LazyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_ARGS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THROWABLE_$;


/**
 * 方法上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 13:01
 */
public final class MethodContext extends Context implements MethodMetaAcquireAbility {

    private static final Logger log = LoggerFactory.getLogger(MethodContext.class);

    /**
     * 方法元信息上下文
     */
    private final MethodMetaContext metaContext;

    /**
     * 参数值数值
     */
    private final Object[] arguments;

    /**
     * 参数上下文数组
     */
    private final ParameterContext[] parameterContexts;

    /**
     * 方法上下文构造方法
     *
     * @param methodMetaContext 方法元信息上下文对象
     * @param arguments         参数列表
     * @throws IOException 构造过程中可能会出现IO异常
     */
    public MethodContext(MethodMetaContext methodMetaContext, Object[] arguments) throws IOException {
        super(methodMetaContext.getCurrentAnnotatedElement());
        this.metaContext = methodMetaContext;
        this.arguments = arguments == null ? new Object[0] : arguments;
        setParentContext(methodMetaContext.getParentContext());
        this.parameterContexts = createParameterContexts();
        setContextVar();
    }

    public MethodMetaContext getMetaContext() {
        return metaContext;
    }

    @Override
    public Method getCurrentAnnotatedElement() {
        return metaContext.getCurrentAnnotatedElement();
    }

    /**
     * 获取类上下文
     *
     * @return 类上下文
     */
    public ClassContext getClassContext() {
        return (ClassContext) getParentContext();
    }

    /**
     * 获取所有原始的参数值
     *
     * @return 方法参数列表
     */
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * 获取所有经过处理之后的参数值
     *
     * @return 经过处理之后的参数列表
     */
    public Object[] getAfterProcessArguments() {
        return Stream.of(getParameterContexts()).map(ParameterContext::getValue).toArray(Object[]::new);
    }

    /**
     * 获取所有的方法参数上下文对象
     *
     * @return 所有的方法参数上下文对象
     */
    public ParameterContext[] getParameterContexts() {
        return this.parameterContexts;
    }


    /**
     * 获取当前方法的唯一签名信息<br/>
     * <pre>
     *  {@code
     *   类名#方法名(参数值1, 参数值1, ...)
     *
     *   例如：
     *   StringUtils#hasText('abcd')
     *  }
     * </pre>
     *
     * @return 当前方法的简单签名信息
     */
    public String getSignature() {
        return getClassContext().getCurrentAnnotatedElement().getSimpleName() + "#" + getCurrentAnnotatedElement().getName() + paramterToString();
    }

    /**
     * 将参数列表转化为字符串显示
     *
     * @return 参数列表转化为字符串显示
     */
    public String paramterToString() {
        return StringUtils.join(getArguments(), "(", ", ", ")");
    }

    /**
     * 使用当前方法上下文中的信息创建出对应的参数列表上下文对象
     *
     * @return 参数上下文对象数组
     */
    private ParameterContext[] createParameterContexts() {
        int parameterCount = getCurrentAnnotatedElement().getParameterCount();
        ParameterContext[] parameterContexts = new ParameterContext[parameterCount];
        String[] parameterNames = metaContext.getParameterNames();
        for (int i = 0; i < parameterCount; i++) {
            parameterContexts[i] = new ParameterContext(this, parameterNames[i], this.arguments[i], i);
        }
        return parameterContexts;
    }

    @Override
    public Parameter[] getParameters() {
        return metaContext.getParameters();
    }

    @Override
    public ResolvableType[] getParameterResolvableTypes() {
        return metaContext.getParameterResolvableTypes();
    }

    @Override
    public String[] getParameterNames() {
        return metaContext.getParameterNames();
    }

    @Override
    public ResolvableType getReturnResolvableType() {
        return metaContext.getReturnResolvableType();
    }

    @Override
    public Class<?> getReturnType() {
        return metaContext.getReturnType();
    }

    @Override
    public boolean isVoidMethod() {
        return metaContext.isVoidMethod();
    }

    @Override
    public boolean needAutoCloseResource() {
        return metaContext.needAutoCloseResource();
    }

    @Override
    public boolean isConvertProhibition() {
        return metaContext.isConvertProhibition();
    }

    @Override
    public boolean isAsyncMethod() {
        return metaContext.isAsyncMethod();
    }

    @Override
    public boolean isReqCreatCompleteExecutionWrapperMethod() {
        return metaContext.isReqCreatCompleteExecutionWrapperMethod();
    }

    @Override
    public boolean isImmediateExecutionWrapperMethod() {
        return metaContext.isImmediateExecutionWrapperMethod();
    }

    @Override
    public Object invokeWrapperMethod() {
        try {
            return parseExpression(getMergedAnnotation(Wrapper.class).value(), getRealMethodReturnType());
        } catch (Exception e) {
            throw new WrapperMethodInvokeException(e, "Wrapper method invocation failed: '{}'", getCurrentAnnotatedElement()).printException(log);
        }
    }

    @Override
    public boolean isFutureMethod() {
        return metaContext.isFutureMethod();
    }

    @Override
    public boolean isOptionalMethod() {
        return metaContext.isOptionalMethod();
    }

    @Override
    public Type getRealMethodReturnType() {
        return metaContext.getRealMethodReturnType();
    }

    @Override
    public ResolvableType getRealMethodReturnResolvableType() {
        return metaContext.getRealMethodReturnResolvableType();
    }

    @Override
    public String getSimpleSignature() {
        return metaContext.getSimpleSignature();
    }

    @Override
    public void setContextVar() {
        SpELVariate contextVar = getContextVar();
        contextVar.addRootVariable($_METHOD_CONTEXT_$, this);
        contextVar.addRootVariable($_METHOD_ARGS_$, LazyValue.of(this::getArguments));
        Method currentMethod = getCurrentAnnotatedElement();

        // 加载由@SpELImport导入的函数、变量和Hook
        handleSpELImport(currentMethod, importVarHandler());

        useHook(Lifecycle.METHOD);
    }

    public void setThrowableVar(Throwable throwable) {
        getContextVar().addRootVariable($_THROWABLE_$, throwable);
        useHook(Lifecycle.THROWABLE);
    }

}
