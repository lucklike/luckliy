package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.AutoCloseResponse;
import com.luckyframework.httpclient.proxy.annotations.ConvertProhibition;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import com.luckyframework.httpclient.proxy.spel.VarScope;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.reflect.Param;
import com.luckyframework.reflect.ParameterUtils;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.ASYNC_TAG;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.CLASS_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.METHOD_CONTEXT;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.REQUEST;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THIS;
import static com.luckyframework.httpclient.proxy.ParameterNameConstant.THROWABLE;

/**
 * 方法上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 13:01
 */
public class MethodContext extends Context {

    /**
     * 类上下文
     */
    private final ClassContext classContext;

    /**
     * 参数数组
     */
    private final Parameter[] parameters;

    /**
     * 参数值数值
     */
    private final Object[] arguments;

    /**
     * 参数名数组
     */
    private final String[] parameterNames;

    /**
     * 参数上下文数组
     */
    private final ParameterContext[] parameterContexts;

    /**
     * 方法上下文构造方法
     *
     * @param proxyObject   代理对象
     * @param currentClass  当前类Class
     * @param currentMethod 当前方法
     * @param arguments     参数列表
     * @throws IOException 构造过程中可能会出现IO异常
     */
    public MethodContext(Object proxyObject, Class<?> currentClass, Method currentMethod, Object[] arguments) throws IOException {
        this(new ClassContext(currentClass), proxyObject, currentMethod, arguments);
    }

    /**
     * 方法上下文构造方法
     *
     * @param classContext  类上下文对象
     * @param proxyObject   代理对象
     * @param currentMethod 当前方法
     * @param arguments     参数列表
     * @throws IOException 构造过程中可能会出现IO异常
     */
    public MethodContext(ClassContext classContext, Object proxyObject, Method currentMethod, Object[] arguments) throws IOException {
        super(currentMethod);
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.parameters = currentMethod.getParameters();
        this.classContext = classContext;
        this.parameterNames = new String[parameters.length];
        setProxyObject(proxyObject);
        setParentContext(classContext);
        List<String> asmParamNames = ASMUtil.getClassOrInterfaceMethodParamNames(currentMethod);
        boolean asmSuccess = ContainerUtils.isNotEmptyCollection(asmParamNames);
        for (int i = 0; i < parameters.length; i++) {
            parameterNames[i] = ParameterUtils.getParamName(parameters[i], asmSuccess ? asmParamNames.get(i) : null);
        }
        setContextVar();
        this.parameterContexts = createParameterContexts();
    }

    /**
     * 获取类上下文对象
     *
     * @return 类上下文对象
     */
    public ClassContext getClassContext() {
        return classContext;
    }

    /**
     * 获取当前的注解元素{@link Method}
     *
     * @return 当前的注解元素 Method
     */
    @Override
    public Method getCurrentAnnotatedElement() {
        return (Method) super.getCurrentAnnotatedElement();
    }

    /**
     * 获取所有的参数信息
     *
     * @return 参数信息数组
     */
    public Parameter[] getParameters() {
        return parameters;
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
     * 获取所有的参数名
     *
     * @return 参数列表对应的参数名
     */
    public String[] getParameterNames() {
        return parameterNames;
    }

    /**
     * 获取方法返回值类型{@link ResolvableType}
     *
     * @return 方法返回值类型ResolvableType
     */
    public ResolvableType getReturnResolvableType() {
        return ResolvableType.forMethodReturnType(getCurrentAnnotatedElement());
    }

    /**
     * 获取方法返回值类型{@link Class}
     *
     * @return 方法返回值类型Class
     */
    public Class<?> getReturnType() {
        return getCurrentAnnotatedElement().getReturnType();
    }

    /**
     * 判断当前方法是否是一个void方法
     *
     * @return 当前方法是否是一个void方法
     */
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
    public boolean isConvertProhibition() {
        return isAnnotated(ConvertProhibition.class);
    }

    /**
     * 当前方法是否为一个异步的void方法
     *
     * @return 当前方法是否为一个异步的void方法
     */
    public boolean isAsyncMethod() {
        if (!isVoidMethod()) {
            return false;
        }
        Boolean asyncTag = getVar(ASYNC_TAG, Boolean.class);
        if (asyncTag != null) {
            return asyncTag;
        }
        Async asyncAnn = getMergedAnnotationCheckParent(Async.class);
        return asyncAnn != null && asyncAnn.enable();
    }

    /**
     * 当前方法是否是一个{@link Future}方法
     *
     * @return 当前方法是否是一个Future方法
     */
    public boolean isFutureMethod() {
        return Future.class.isAssignableFrom(getReturnType());
    }

    /**
     * 获取当前方法的真实返回值类型，如果是{@link Future}方法则返回泛型类型
     *
     * @return 获取当前方法的真实返回值类型
     */
    public Type getRealMethodReturnType() {
        if (isFutureMethod()) {
            ResolvableType methodReturnType = getReturnResolvableType();
            return methodReturnType.hasGenerics() ? methodReturnType.getGeneric(0).getType() : Object.class;
        }
        return getReturnResolvableType().getType();
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
    public String getSimpleSignature() {
        return getClassContext().getCurrentAnnotatedElement().getSimpleName() + "#" + MethodUtils.getWithParamMethodName(getCurrentAnnotatedElement());
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
        for (int i = 0; i < parameterCount; i++) {
            parameterContexts[i] = new ParameterContext(this, this.parameterNames[i], this.arguments[i], i);
        }
        return parameterContexts;
    }

    /**
     * 根据方法参数类型将参数转化为该类型对应的值
     *
     * @param method 方法实例
     * @return 默认参数名
     */
    @NonNull
    public Object[] getMethodParamObject(Method method) {
        List<Object> varNameList = new ArrayList<>();

        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();

            // 执行配置在@Param注解中的SpEL表达式
            Param paramAnn = AnnotationUtils.findMergedAnnotation(parameter, Param.class);
            if (paramAnn != null && StringUtils.hasText(paramAnn.value())) {
                try {
                    varNameList.add(parseExpression(paramAnn.value(), ResolvableType.forMethodParameter(method, i)));
                } catch (Exception e) {
                    throw new MethodParameterAcquisitionException(e, "An exception occurred while getting a method argument from a SpEL expression: '{}'", paramAnn.value());
                }

                continue;
            }

            if (parameterType == MethodContext.class) {
                varNameList.add(getRootVar(METHOD_CONTEXT));
            } else if (parameterType == ClassContext.class) {
                varNameList.add(getRootVar(CLASS_CONTEXT));
            } else if (parameterType == Method.class) {
                varNameList.add(getRootVar(METHOD));
            } else if (parameterType == Class.class) {
                varNameList.add(getRootVar(CLASS));
            } else if (parameterType == getClassContext().getCurrentAnnotatedElement()) {
                varNameList.add(getRootVar(THIS));
            } else if (parameterType == Request.class) {
                varNameList.add(getRootVar(REQUEST));
            } else if (Throwable.class.isAssignableFrom(parameterType)) {
                varNameList.add(getRootVar(THROWABLE));
            } else {
                varNameList.add(null);
            }
        }

        return varNameList.toArray(new Object[0]);
    }

    @Override
    public void setContextVar() {
        MapRootParamWrapper contextVar = getContextVar();
        contextVar.addRootVariable(THIS, LazyValue.of(this::getProxyObject));
        contextVar.addRootVariable(METHOD_CONTEXT, LazyValue.of(this));
        contextVar.addRootVariable(METHOD, LazyValue.of(this::getCurrentAnnotatedElement));

        ClassContext classContext = getClassContext();
        Class<?> currentClass = classContext.getCurrentAnnotatedElement();
        Method currentMethod = getCurrentAnnotatedElement();

        // [Method] 加载由@SpELImpoet注解导入的SpEL变量、包 -> root()、var()、rootLit()、varLit()、pack()
        this.loadSpELImportAnnotationVar(currentMethod);
        // [Method] 加载由@SpELImpoet注解导入的Class -> classes() 当前Context加载作用域为DEFAULT和METHOD的变量，父Context加载作用域为CLASS的变量
        this.loadSpELImportAnnotationImportClasses(this, this, currentMethod, VarScope.DEFAULT, VarScope.METHOD);
        classContext.loadSpELImportAnnotationImportClasses(classContext, this, currentMethod, VarScope.CLASS);

        // [Class] 加载由@SpELImpoet注解导入的Class -> classes()，Class中导入的作用域为METHOD的变量此时加载到当前Context中
        classContext.loadSpELImportAnnotationImportClassesFindParent(this, this, currentClass, VarScope.METHOD);

        // 加载当前类中作用域为METHOD的变量
        loadClassSpELVar(this, currentClass, VarScope.METHOD);
        super.setContextVar();
    }

    @Override
    public void setResponseVar(Response response, Context context) {
        super.setResponseVar(response, context);
        loadSpELImportAnnImportClassesVarByScope(VarScope.RESPONSE);
    }

    @Override
    public void setRequestVar(Request request) {
        super.setRequestVar(request);
        loadSpELImportAnnImportClassesVarByScope(VarScope.REQUEST);
    }


    public void setThrowableVar(Throwable throwable) {
        getContextVar().addRootVariable(THROWABLE, throwable);
        loadSpELImportAnnImportClassesVarByScope(VarScope.THROWABLE);
    }


    private void loadSpELImportAnnImportClassesVarByScope(VarScope varScope) {
        ClassContext classContext = getClassContext();
        Class<?> currentClass = classContext.getCurrentAnnotatedElement();
        Method currentMethod = getCurrentAnnotatedElement();
        this.loadSpELImportAnnotationImportClasses(this, this, currentMethod, varScope);
        classContext.loadSpELImportAnnotationImportClassesFindParent(this, this, currentClass, varScope);
        loadClassSpELVar(this, currentClass, varScope);
    }
}
