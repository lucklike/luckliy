package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.exeception.MethodParameterAcquisitionException;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.var.VarScope;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.Param;
import com.luckyframework.spel.LazyValue;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CLASS_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_METHOD_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_METHOD_CONTEXT_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_REQUEST_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_THIS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_THROWABLE_$;


/**
 * 方法上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 13:01
 */
public class MethodContext extends Context implements MethodMetaAcquireAbility {

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

    @Override
    public Method getCurrentAnnotatedElement() {
        return metaContext.getCurrentAnnotatedElement();
    }

    /**
     * 获取类上下文
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
    public boolean isFutureMethod() {
        return metaContext.isFutureMethod();
    }

    @Override
    public Type getRealMethodReturnType() {
        return metaContext.getRealMethodReturnType();
    }

    @Override
    public String getSimpleSignature() {
        return metaContext.getSimpleSignature();
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

            // 取默认名称的类型
            if (parameterType == MethodContext.class) {
                varNameList.add(getRootVar($_METHOD_CONTEXT_$));
            } else if (parameterType == ClassContext.class) {
                varNameList.add(getRootVar($_CLASS_CONTEXT_$));
            } else if (parameterType == Method.class) {
                varNameList.add(getRootVar($_METHOD_$));
            } else if (parameterType == Class.class) {
                varNameList.add(getRootVar($_CLASS_$));
            } else if (parameterType == getClassContext().getCurrentAnnotatedElement()) {
                varNameList.add(getRootVar($_THIS_$));
            } else if (parameterType == Request.class) {
                varNameList.add(getRootVar($_REQUEST_$));
            } else if (Throwable.class.isAssignableFrom(parameterType)) {
                varNameList.add(getRootVar($_THROWABLE_$));
            } else {
                varNameList.add(null);
            }
        }
        return varNameList.toArray(new Object[0]);
    }

    @Override
    public void setContextVar() {
        SpELVariate contextVar = getContextVar();
        contextVar.addRootVariable($_THIS_$, LazyValue.of(this::getProxyObject));
        contextVar.addRootVariable($_METHOD_CONTEXT_$, LazyValue.of(this));
        contextVar.addRootVariable($_METHOD_$, LazyValue.of(this::getCurrentAnnotatedElement));

        ClassContext classContext = getClassContext();
        Class<?> currentClass = classContext.getCurrentAnnotatedElement();
        Method currentMethod = getCurrentAnnotatedElement();

        // [Method] 加载由@SpELImpoet注解导入的SpEL变量、包 -> root()、var()、rootLit()、varLit()、pack()
        this.loadSpELImportAnnVarFun(currentMethod);
        // [Method] 加载由@SpELImpoet注解导入的Class -> value() 当前Context加载作用域为DEFAULT和METHOD的变量，父Context加载作用域为CLASS的变量
        this.loadSpELImportAnnImportClassesVar(this, this, currentMethod, VarScope.DEFAULT, VarScope.METHOD_CONTEXT);
        classContext.loadSpELImportAnnImportClassesVar(classContext, this, currentMethod, VarScope.CLASS);

        // [Class] 加载由@SpELImpoet注解导入的Class -> value()，Class中导入的作用域为METHOD的变量此时加载到当前Context中
        classContext.loadSpELImportAnnImportClassesVarFindParent(this, this, currentClass, VarScope.METHOD_CONTEXT);

        // 加载当前类中作用域为METHOD的变量
        loadClassSpELVar(this, currentClass, VarScope.METHOD_CONTEXT);
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
        getContextVar().addRootVariable($_THROWABLE_$, throwable);
        loadSpELImportAnnImportClassesVarByScope(VarScope.THROWABLE);
    }


    private void loadSpELImportAnnImportClassesVarByScope(VarScope varScope) {
        ClassContext classContext = getClassContext();
        Class<?> currentClass = classContext.getCurrentAnnotatedElement();
        Method currentMethod = getCurrentAnnotatedElement();
        this.loadSpELImportAnnImportClassesVar(this, this, currentMethod, varScope);
        classContext.loadSpELImportAnnImportClassesVarFindParent(this, this, currentClass, varScope);
        loadClassSpELVar(this, currentClass, varScope);
    }
}
