package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.NotAnalyzeBody;
import com.luckyframework.httpclient.proxy.spel.SpELUtils;
import com.luckyframework.httpclient.proxy.annotations.Async;
import com.luckyframework.httpclient.proxy.annotations.ConvertProhibition;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.core.ResolvableType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * 方法上下文
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/21 13:01
 */
public class MethodContext extends Context {

    private final ClassContext classContext;
    private final Parameter[] parameters;
    private final Object[] arguments;
    private final String[] parameterNames;

    public MethodContext(Object proxyObject, Class<?> currentClass, Method currentMethod, Object[] arguments) throws IOException {
        super(currentMethod);
        this.arguments = arguments == null ? new Object[0] : arguments;
        this.parameters = currentMethod.getParameters();
        this.classContext = new ClassContext(currentClass);
        this.parameterNames = new String[parameters.length];
        setProxyObject(proxyObject);
        List<String> asmParamNames = ASMUtil.getClassOrInterfaceMethodParamNames(currentMethod);
        for (int i = 0; i < asmParamNames.size(); i++) {
            String asmName = asmParamNames.get(i);
            parameterNames[i] = StringUtils.hasText(asmName) ? asmName : parameters[i].getName();
        }
    }

    public MethodContext(ClassContext classContext, Method currentMethod, Object[] arguments) throws IOException {
        this(classContext.getProxyObject(), classContext.getCurrentAnnotatedElement(), currentMethod, arguments);
        setParentContext(classContext);
    }

    public ClassContext getClassContext() {
        return classContext;
    }

    @Override
    public Method getCurrentAnnotatedElement() {
        return (Method) super.getCurrentAnnotatedElement();
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public String[] getParameterNames() {
        return parameterNames;
    }

    public ResolvableType getReturnResolvableType() {
        return ResolvableType.forMethodReturnType(getCurrentAnnotatedElement());
    }

    public Class<?> getReturnType() {
        return getCurrentAnnotatedElement().getReturnType();
    }

    public boolean isVoidMethod() {
        return getReturnType() == void.class;
    }

    public boolean isVoidResponseMethod() {
        return getReturnType() == VoidResponse.class;
    }

    public boolean isNotAnalyzeBodyMethod() {
        return isVoidMethod() || isVoidResponseMethod() || isAnnotatedCheckParent(NotAnalyzeBody.class);
    }

    public boolean isConvertProhibition() {
        return AnnotationUtils.isAnnotated(getCurrentAnnotatedElement(), ConvertProhibition.class);
    }

    public boolean isAsyncMethod() {
        return AnnotationUtils.isAnnotated(getCurrentAnnotatedElement(), Async.class)
                || AnnotationUtils.isAnnotated(getClassContext().getCurrentAnnotatedElement(), Async.class);
    }

    public boolean isFutureMethod() {
        return Future.class.isAssignableFrom(getReturnType());
    }

    public Type getRealMethodReturnType() {
        if (isFutureMethod()) {
            ResolvableType methodReturnType = getReturnResolvableType();
            return methodReturnType.hasGenerics() ? methodReturnType.getGeneric(0).getType() : Object.class;
        }
        return getReturnResolvableType().getType();
    }

    public List<ParameterContext> getParameterContexts() {
        int parameterCount = getCurrentAnnotatedElement().getParameterCount();
        List<ParameterContext> parameterContexts = new ArrayList<>(parameterCount);
        for (int i = 0; i < parameterCount; i++) {
            parameterContexts.add(new ParameterContext(this, this.parameterNames[i], this.arguments[i], i));
        }
        return parameterContexts;
    }

    public String getSimpleSignature() {
        return getClassContext().getCurrentAnnotatedElement().getSimpleName() + "#" + MethodUtils.getWithParamMethodName(getCurrentAnnotatedElement());
    }

    public String getSignature() {
        return getClassContext().getCurrentAnnotatedElement().getSimpleName() + "#" + getCurrentAnnotatedElement().getName() + paramterToString();
    }

    public String paramterToString() {
        return StringUtils.join(getArguments(), "(", ", ", ")");
    }

    @Override
    public <T> T parseExpression(String expression, ResolvableType returnType, Consumer<SpELUtils.ExtraSpELArgs> argSetter) {
        SpELUtils.ExtraSpELArgs spELArgs = getSpELArgs();
        argSetter.accept(spELArgs);
        return SpELUtils.parseExpression(
                SpELUtils.getContextParamWrapper(this, spELArgs)
                        .setExpression(expression)
                        .setExpectedResultType(returnType)
        );
    }

    @Override
    public SpELUtils.ExtraSpELArgs getSpELArgs() {
        return super.getSpELArgs()
                .extractMethodContext(this);
    }
}
