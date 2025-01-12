package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.AutoCloseResponse;
import com.luckyframework.httpclient.proxy.annotations.ConvertProhibition;
import com.luckyframework.httpclient.proxy.annotations.Wrapper;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.reflect.ParameterUtils;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_META_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_PARAM_NAMES_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_PARAM_TYPES_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_REAL_RETURN_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_RETURN_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalVarName.__$ASYNC_TAG$__;


/**
 * 方法元信息上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/19 23:41
 */
public final class MethodMetaContext extends Context implements MethodMetaAcquireAbility {

    /**
     * 参数名数组
     */
    private final String[] parameterNames;

    /**
     * 参数数组
     */
    private final Parameter[] parameters;

    /**
     * 参数类型数组
     */
    private final ResolvableType[] parameterTypes;

    /**
     * 方法返回值类型
     */
    private final ResolvableType methodReturnType;



    /**
     * 方法元数据上下文构造器
     *
     * @param method 方法
     * @throws IOException 构造过程中可能会出现IO异常
     */
    public MethodMetaContext(Method method) throws IOException {
        super(method);

        // 方法返回值类型
        this.methodReturnType = ResolvableType.forMethodReturnType(method);

        // 设置参数信息
        int parameterCount = method.getParameterCount();
        this.parameters = method.getParameters();
        this.parameterNames = new String[parameterCount];
        this.parameterTypes = new ResolvableType[parameterCount];
        List<String> asmParamNames = ASMUtil.getClassOrInterfaceMethodParamNames(method);
        boolean asmSuccess = ContainerUtils.isNotEmptyCollection(asmParamNames);
        for (int i = 0; i < parameters.length; i++) {
            parameterNames[i] = ParameterUtils.getParamName(parameters[i], asmSuccess ? asmParamNames.get(i) : null);
            parameterTypes[i] = ResolvableType.forMethodParameter(method, i);
        }
    }

    /**
     * 设置默认的上下文变量
     */
    @Override
    public void setContextVar() {
        SpELVariate contextVar = getContextVar();
        contextVar.addRootVariable($_METHOD_META_CONTEXT_$, this);
        contextVar.addRootVariable($_METHOD_$, LazyValue.of(this::getCurrentAnnotatedElement));
        contextVar.addRootVariable($_METHOD_RETURN_TYPE_$, LazyValue.of(this::getReturnResolvableType));
        contextVar.addRootVariable($_METHOD_REAL_RETURN_TYPE_$, LazyValue.of(this::getRealMethodReturnResolvableType));
        contextVar.addRootVariable($_METHOD_PARAM_TYPES_$, LazyValue.of(this::getParameterResolvableTypes));
        contextVar.addRootVariable($_METHOD_PARAM_NAMES_$, LazyValue.of(this::getParameterNames));

        handleSpELImport(getCurrentAnnotatedElement(), importFunHookHandler());

        useHook(Lifecycle.METHOD_META);
    }

    @Override
    public Method getCurrentAnnotatedElement() {
        return (Method) super.getCurrentAnnotatedElement();
    }

    /**
     * 获取所有的参数信息
     *
     * @return 参数信息数组
     */
    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    /**
     * 获取所有的参数名
     *
     * @return 参数列表对应的参数名
     */
    @Override
    public String[] getParameterNames() {
        return parameterNames;
    }

    /**
     * 获取方法返回值类型{@link ResolvableType}
     *
     * @return 方法返回值类型ResolvableType
     */
    @Override
    public ResolvableType getReturnResolvableType() {
        return methodReturnType;
    }

    /**
     * 获取方法返回值类型{@link Class}
     *
     * @return 方法返回值类型Class
     */
    @Override
    public Class<?> getReturnType() {
        return getCurrentAnnotatedElement().getReturnType();
    }

    /**
     * 判断当前方法是否是一个void方法
     *
     * @return 当前方法是否是一个void方法
     */
    @Override
    public boolean isVoidMethod() {
        return getReturnType() == void.class;
    }

    /**
     * 当前方法是否需要自动关闭资源
     * <pre>
     *     1.如果方法、类上上有被{@link AutoCloseResponse @AutoCloseResponse}注解标注，则是否自动关闭资源取决于{@link AutoCloseResponse#value()}
     *     2.检查当前方法的返回值是否为不必自动关闭资源的类型
     * </pre>
     *
     * @return 当前方法是否需要自动关闭资源
     */
    @Override
    public boolean needAutoCloseResource() {
        AutoCloseResponse autoCloseAnn = getMergedAnnotationCheckParent(AutoCloseResponse.class);
        if (autoCloseAnn != null) {
            return autoCloseAnn.value();
        }
        return !HttpClientProxyObjectFactory.getNotAutoCloseResourceTypes().contains(getRealMethodReturnType());
    }

    /**
     * 当前方法使用禁止使用转换器
     *
     * @return 当前方法使用禁止使用转换器
     */
    @Override
    public boolean isConvertProhibition() {
        return isAnnotated(ConvertProhibition.class);
    }

    /**
     * 当前方法是否为一个异步的void方法
     *
     * @return 当前方法是否为一个异步的void方法
     */
    @Override
    public boolean isAsyncMethod() {
        if (!isVoidMethod()) {
            return false;
        }
        Boolean asyncTag = getVar(__$ASYNC_TAG$__, Boolean.class);
        if (asyncTag != null) {
            return asyncTag;
        }
        Async asyncAnn = getMergedAnnotationCheckParent(Async.class);
        return asyncAnn != null && asyncAnn.enable();
    }

    /**
     * 是否为一个包装器方法
     *
     * @return 是否为一个包装器方法
     */
    @Override
    public boolean isReqCreatCompleteExecutionWrapperMethod() {
        Wrapper wrapperAnn = getMergedAnnotation(Wrapper.class);
        return wrapperAnn != null && wrapperAnn.waitReqCreatComplete();
    }

    /**
     * 是否为一个立即执行的包装器方法
     *
     * @return 是否为一个立即执行的包装器方法
     */
    @Override
    public boolean isImmediateExecutionWrapperMethod() {
        Wrapper wrapperAnn = getMergedAnnotation(Wrapper.class);
        return wrapperAnn != null && !wrapperAnn.waitReqCreatComplete();
    }

    /**
     * 执行包装器方法
     *
     * @return 执行结果
     */
    @Override
    public Object invokeWrapperMethod() {
        throw new UnsupportedOperationException("The current context ‘MethodMetaContext’ does not support executing wrapper methods.");
    }

    /**
     * 当前方法是否是一个{@link Future}方法
     *
     * @return 当前方法是否是一个Future方法
     */
    @Override
    public boolean isFutureMethod() {
        return Future.class.isAssignableFrom(getReturnType());
    }

    @Override
    public boolean isOptionalMethod() {
        return Optional.class.isAssignableFrom(getReturnType());
    }

    /**
     * 获取当前方法的真实返回值类型，如果是{@link Future}方法则返回泛型类型
     *
     * @return 获取当前方法的真实返回值类型
     */
    @Override
    public Type getRealMethodReturnType() {
        return getRealMethodReturnResolvableType().getType();
    }

    /**
     * 获取当前方法的真实返回值类型，如果是{@link Future}方法则返回泛型类型
     *
     * @return 获取当前方法的真实返回值类型
     */
    @Override
    public ResolvableType getRealMethodReturnResolvableType() {
        ResolvableType methodReturnType = getReturnResolvableType();
        if (isFutureMethod() || isOptionalMethod()) {
            return methodReturnType.hasGenerics() ? methodReturnType.getGeneric(0) : ResolvableType.forClass(Object.class);
        }
        return methodReturnType;
    }

    /**
     * 获取当前方法的简单签名信息<br/>
     * <pre>
     *  {@code
     *   类名#方法名(参数类型1, 参数类型2, ...)
     *
     *   例如：
     *   StringUtils#hasText(String)
     *  }
     * </pre>
     *
     * @return 当前方法的简单签名信息
     */
    @Override
    public String getSimpleSignature() {
        return getParentContext().getCurrentAnnotatedElement().getSimpleName() + "#" + MethodUtils.getWithParamMethodName(getCurrentAnnotatedElement());
    }

    /**
     * 获取参数列表类型
     *
     * @return 参数列表类型
     */
    @Override
    public ResolvableType[] getParameterResolvableTypes() {
        return this.parameterTypes;
    }

    /**
     * 获取类上下文信息
     *
     * @return 类上下文信息
     */
    public ClassContext getParentContext() {
        return (ClassContext) super.getParentContext();
    }
}

