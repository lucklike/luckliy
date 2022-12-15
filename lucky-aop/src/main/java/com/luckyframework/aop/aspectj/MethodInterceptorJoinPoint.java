package com.luckyframework.aop.aspectj;


import com.luckyframework.aop.exception.MethodParamObtainException;
import com.luckyframework.aop.proxy.AopAdviceChainInvocation;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.reflect.ASMUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 连接点
 * @author fk
 * @version 1.0
 * @date 2021/3/19 0019 11:41
 */
public class MethodInterceptorJoinPoint implements ProceedingJoinPoint, JoinPoint.StaticPart{

    /** Lazily initialized signature object. */
    @Nullable
    private Signature signature;

    /** Lazily initialized source location object. */
    @Nullable
    private SourceLocation sourceLocation;

    @NonNull
    private final AopAdviceChainInvocation aopAdviceChainInvocation;


    /**
     * Create a new MethodInterceptorJoinPoint, wrapping the given
     * Lucky Proxy object 、 Target object and invoke Method and invoke param
     */
    public MethodInterceptorJoinPoint(AopAdviceChainInvocation aopAdviceChainInvocation) {
        Assert.notNull(aopAdviceChainInvocation, "AopAdviceChainInvocation object must not be null");
        this.aopAdviceChainInvocation=aopAdviceChainInvocation;
    }


    @Override
    public void set$AroundClosure(AroundClosure aroundClosure) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public Object proceed() throws Throwable {
        return aopAdviceChainInvocation.invoke();
    }

    @Override
    @Nullable
    public Object proceed(Object[] arguments) throws Throwable {
        this.aopAdviceChainInvocation.setArgs(arguments);
        return this.aopAdviceChainInvocation.invoke();
    }

    @NonNull
    public String getBeanName(){
        return this.aopAdviceChainInvocation.getBeanName();
    }

    /**
     * Returns the Lucky AOP proxy. Cannot be {@code null}.
     */
    @Override
    public Object getThis() {
        return this.aopAdviceChainInvocation.getProxy();
    }

    /**
     * Returns the Lucky AOP target. May be {@code null} if there is no target.
     */
    @Override
    @Nullable
    public Object getTarget() {
        return this.aopAdviceChainInvocation.getTarget();
    }

    public void changeTarget(Object newTarget){
        this.aopAdviceChainInvocation.setTarget(newTarget);
    }

    @Override
    public Object[] getArgs() {
        return this.aopAdviceChainInvocation.getArgs();
    }

    @Override
    public Signature getSignature() {
        if (this.signature == null) {
            this.signature = new MethodSignatureImpl();
        }
        return this.signature;
    }

    @Override
    public SourceLocation getSourceLocation() {
        if (this.sourceLocation == null) {
            this.sourceLocation = new SourceLocationImpl();
        }
        return this.sourceLocation;
    }

    @Override
    public String getKind() {
        return ProceedingJoinPoint.METHOD_EXECUTION;
    }

    @Override
    public int getId() {
        // TODO: It's just an adapter but returning 0 might still have side effects...
        return 0;
    }

    @Override
    public StaticPart getStaticPart() {
        return this;
    }

    @Override
    public String toShortString() {
        return "execution(" + getSignature().toShortString() + ")";
    }

    @Override
    public String toLongString() {
        return "execution(" + getSignature().toLongString() + ")";
    }

    @Override
    public String toString() {
        return "execution(" + getSignature().toString() + ")";
    }


    /**
     * Lazily initialized MethodSignature.
     */
    private class MethodSignatureImpl implements MethodSignature {

        @Nullable
        private volatile String[] parameterNames;

        @Override
        public String getName() {
            return getMethod().getName();
        }

        @Override
        public int getModifiers() {
            return getMethod().getModifiers();
        }

        @Override
        public Class<?> getDeclaringType() {
            return getMethod().getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return getMethod().getDeclaringClass().getName();
        }

        @Override
        public Class<?> getReturnType() {
            return getMethod().getReturnType();
        }

        @Override
        public Method getMethod() {
            return aopAdviceChainInvocation.getMethod();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return getMethod().getParameterTypes();
        }

        @Override
        @Nullable
        public String[] getParameterNames() {
            String[] parameterNames = this.parameterNames;
            if (parameterNames == null) {
                try {
                    parameterNames = ContainerUtils.listToArray(ASMUtil.getClassOrInterfaceMethodParamNames(getMethod()),String.class);
                } catch (IOException e) {
                    throw new MethodParamObtainException("An exception occurred when getting the parameter list name of the '"+getMethod()+"' method");
                }
                this.parameterNames = parameterNames;
            }
            return parameterNames;
        }

        @Override
        public Class<?>[] getExceptionTypes() {
            return getMethod().getExceptionTypes();
        }

        @Override
        public String toShortString() {
            return toString(false, false, false, false);
        }

        @Override
        public String toLongString() {
            return toString(true, true, true, true);
        }

        @Override
        public String toString() {
            return toString(false, true, false, true);
        }

        private String toString(boolean includeModifier, boolean includeReturnTypeAndArgs,
                                boolean useLongReturnAndArgumentTypeName, boolean useLongTypeName) {

            StringBuilder sb = new StringBuilder();
            if (includeModifier) {
                sb.append(Modifier.toString(getModifiers()));
                sb.append(" ");
            }
            if (includeReturnTypeAndArgs) {
                appendType(sb, getReturnType(), useLongReturnAndArgumentTypeName);
                sb.append(" ");
            }
            appendType(sb, getDeclaringType(), useLongTypeName);
            sb.append(".");
            sb.append(getMethod().getName());
            sb.append("(");
            Class<?>[] parametersTypes = getParameterTypes();
            appendTypes(sb, parametersTypes, includeReturnTypeAndArgs, useLongReturnAndArgumentTypeName);
            sb.append(")");
            return sb.toString();
        }

        private void appendTypes(StringBuilder sb, Class<?>[] types, boolean includeArgs,
                                 boolean useLongReturnAndArgumentTypeName) {

            if (includeArgs) {
                for (int size = types.length, i = 0; i < size; i++) {
                    appendType(sb, types[i], useLongReturnAndArgumentTypeName);
                    if (i < size - 1) {
                        sb.append(",");
                    }
                }
            }
            else {
                if (types.length != 0) {
                    sb.append("..");
                }
            }
        }

        private void appendType(StringBuilder sb, Class<?> type, boolean useLongTypeName) {
            if (type.isArray()) {
                appendType(sb, type.getComponentType(), useLongTypeName);
                sb.append("[]");
            }
            else {
                sb.append(useLongTypeName ? type.getName() : type.getSimpleName());
            }
        }
    }


    /**
     * Lazily initialized SourceLocation.
     */
    private class SourceLocationImpl implements SourceLocation {

        @Override
        public Class<?> getWithinType() {
            if (aopAdviceChainInvocation.getProxy() == null) {
                throw new UnsupportedOperationException("No source location joinpoint available: target is null");
            }
            return aopAdviceChainInvocation.getProxy().getClass();
        }

        @Override
        public String getFileName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLine() {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public int getColumn() {
            throw new UnsupportedOperationException();
        }
    }

}
