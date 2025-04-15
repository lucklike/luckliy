package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.ClassStaticElement;
import com.luckyframework.httpclient.proxy.spel.ParamWrapperSetter;
import com.luckyframework.httpclient.proxy.spel.ParameterInstanceGetter;
import com.luckyframework.reflect.ASMUtil;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.MethodUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_METHOD_ARGS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalRootVarName.$_THIS_$;


/**
 * 插件生成器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/2/7 11:14
 */
public class PluginGenerate {

    private static final String $_EXECUTE_META_$ = "$exeMeta$";

    private final Object pluginObject;

    public PluginGenerate(@NonNull Object pluginObject) {
        Assert.notNull(pluginObject, "pluginObject must not be null");
        this.pluginObject = pluginObject;
    }

    /**
     * 生成插件集合
     *
     * @return 插件集合
     */
    @SuppressWarnings("all")
    public List<ProxyPlugin> generate() {
        List<ProxyPlugin> plugins = new ArrayList<>();
        Method[] methods = ASMUtil.getAllMethodOrder(pluginObject.getClass());
        for (Method method : methods) {

            ProxyPlugin plugin = null;

            // 前置增强
            if (AnnotationUtils.isAnnotated(method, Before.class)) {
                Before beforeAnn = AnnotationUtils.findMergedAnnotation(method, Before.class);
                plugin = new ReflectBeforeProxyPlugin(beforeAnn.init(), beforeAnn.value(), method);
            }
            // 后置增强
            else if (AnnotationUtils.isAnnotated(method, After.class)) {
                After afterAnn = AnnotationUtils.findMergedAnnotation(method, After.class);
                plugin = new ReflectAfterProxyPlugin(afterAnn.init(), afterAnn.value(), method);
            }
            // 正常返回后执行
            else if (AnnotationUtils.isAnnotated(method, AfterReturning.class)) {
                AfterReturning afterAnn = AnnotationUtils.findMergedAnnotation(method, AfterReturning.class);
                plugin = new ReflectAfterReturningProxyPlugin(afterAnn.init(), afterAnn.value(), method);
            }
            // 异常返回后执行
            else if (AnnotationUtils.isAnnotated(method, AfterThrowing.class)) {
                AfterThrowing afterAnn = AnnotationUtils.findMergedAnnotation(method, AfterThrowing.class);
                plugin = new ReflectAfterThrowingProxyPlugin(afterAnn.init(), afterAnn.value(), method);
            }
            // 环绕增强
            else if (AnnotationUtils.isAnnotated(method, Around.class)) {
                Around aroundAnn = AnnotationUtils.findMergedAnnotation(method, Around.class);
                plugin = new ReflectAroundProxyPlugin(aroundAnn.init(), aroundAnn.value(), method);
            }

            if (plugin != null) {
                plugins.add(plugin);
            }
        }
        return plugins;
    }


    private void invokeBeforeMethod(Method method, ProxyDecorator decorator) {
        ParamWrapperSetter wrapperSetter = getParameterInstanceSetter(decorator);
        ParameterInstanceGetter instanceGetter = getParameterInstanceGetter(decorator, null, false);
        decorator.getMeta().getMetaContext().invokeMethod(pluginObject, method, wrapperSetter, instanceGetter);
    }

    private void invokeAfterMethod(Method method, ProxyDecorator decorator) {
        ParamWrapperSetter wrapperSetter = getParameterInstanceSetter(decorator);
        ParameterInstanceGetter instanceGetter = getParameterInstanceGetter(decorator, null, false);
        decorator.getMeta().getMetaContext().invokeMethod(pluginObject, method, wrapperSetter, instanceGetter);
    }

    private Object invokeAfterThrowingMethod(Method method, ProxyDecorator decorator, Throwable e) {
        ParamWrapperSetter wrapperSetter = getParameterInstanceSetter(decorator);
        ParameterInstanceGetter instanceGetter = getParameterInstanceGetter(decorator, e, false);
        return decorator.getMeta().getMetaContext().invokeMethod(pluginObject, method, wrapperSetter, instanceGetter);
    }

    private Object invokeAroundMethod(Method method, ProxyDecorator decorator) {
        ParamWrapperSetter wrapperSetter = getParameterInstanceSetter(decorator);
        ParameterInstanceGetter instanceGetter = getParameterInstanceGetter(decorator, null, true);
        return decorator.getMeta().getMetaContext().invokeMethod(pluginObject, method, wrapperSetter, instanceGetter);
    }

    private ParamWrapperSetter getParameterInstanceSetter(ProxyDecorator decorator) {
        return pw -> {
            Map<String, Object> varMap = new HashMap<>();
            varMap.put($_METHOD_ARGS_$, decorator.getMeta().getArgs());
            varMap.put($_EXECUTE_META_$, decorator.getMeta());
            varMap.put($_THIS_$, pluginObject);
            pw.importPackage(pluginObject.getClass().getPackage().getName());
            pw.getVariables().addFirst(ClassStaticElement.create(getClass()).getAllStaticMethods());
            pw.getRootObject().addFirst(varMap);
        };
    }


    private ParameterInstanceGetter getParameterInstanceGetter(ProxyDecorator decorator, Throwable e, boolean supportProxyDecorator) {
        return parameter -> {
            Class<?> parameterType = parameter.getType();
            if (supportProxyDecorator && ProxyDecorator.class.isAssignableFrom(parameterType)) {
                return decorator;
            }
            if (ExecuteMeta.class.isAssignableFrom(parameterType)) {
                return decorator.getMeta();
            }
            if (Context.class.isAssignableFrom(parameterType)) {
                return decorator.getMeta().getMetaContext();
            }
            if (Class.class.isAssignableFrom(parameterType)) {
                return decorator.getMeta().getTargetClass();
            }
            if (Method.class.isAssignableFrom(parameterType)) {
                return decorator.getMeta().getMethod();
            }
            if (Throwable.class.isAssignableFrom(parameterType)) {
                return e;
            }
            return null;
        };
    }


    abstract class ReflectProxyPlugin implements ProxyPlugin {

        private final String init;
        private final String match;
        protected final Method method;

        ReflectProxyPlugin(String init, String match, Method method) {
            this.init = init;
            this.match = match;
            this.method = method;
        }

        private ParamWrapperSetter getExecuteMetaParameterInstanceSetter(ExecuteMeta meta) {
            return pw -> {
                Map<String, Object> varMap = new HashMap<>();
                varMap.put($_METHOD_ARGS_$, meta.getArgs());
                varMap.put($_EXECUTE_META_$, meta);
                varMap.put($_THIS_$, pluginObject);
                pw.importPackage(pluginObject.getClass().getPackage().getName());
                pw.getVariables().addFirst(ClassStaticElement.create(pluginObject.getClass()).getAllStaticMethods());
                pw.getRootObject().addFirst(varMap);
            };
        }

        @Override
        public boolean match(ExecuteMeta meta) {
            if (StringUtils.hasText(match)) {
                return meta.getMetaContext().parseExpression(match, boolean.class, getExecuteMetaParameterInstanceSetter(meta));
            }
            return false;
        }

        @Override
        public void init(ExecuteMeta meta) {
            if (StringUtils.hasText(init)) {
                meta.getMetaContext().parseExpression(init, getExecuteMetaParameterInstanceSetter(meta));
            }
        }

        @Override
        public String uniqueIdentification() {
            return String.format(
                    "%s@%s.%s#%s",
                    type(),
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    MethodUtils.getWithParamMethodName(method));
        }

        @Override
        public String toString() {
            return uniqueIdentification();
        }

        protected abstract String type();
    }

    /**
     * 基于反射的前置增强插件
     */
    class ReflectBeforeProxyPlugin extends ReflectProxyPlugin {


        ReflectBeforeProxyPlugin(String initFun, String matchFun, Method method) {
            super(initFun, matchFun, method);
        }

        @Override
        protected String type() {
            return "[Before]";
        }

        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            invokeBeforeMethod(method, decorator);
            return decorator.proceed();
        }
    }

    /**
     * 基于反射的后置增强插件
     */
    class ReflectAfterProxyPlugin extends ReflectProxyPlugin {

        ReflectAfterProxyPlugin(String initFun, String matchFun, Method method) {
            super(initFun, matchFun, method);
        }

        @Override
        protected String type() {
            return "[After]";
        }

        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            try {
                return decorator.proceed();
            } finally {
                invokeAfterMethod(method, decorator);
            }
        }

    }

    /**
     * 基于反射的正常返回后执行增强的插件
     */
    class ReflectAfterReturningProxyPlugin extends ReflectProxyPlugin {

        ReflectAfterReturningProxyPlugin(String initFun, String matchFun, Method method) {
            super(initFun, matchFun, method);
        }

        @Override
        protected String type() {
            return "[AfterReturning]";
        }

        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            Object result = decorator.proceed();
            invokeAfterMethod(method, decorator);
            return result;
        }
    }

    /**
     * 基于反射的异常返回后执行增强的插件
     */
    class ReflectAfterThrowingProxyPlugin extends ReflectProxyPlugin {

        ReflectAfterThrowingProxyPlugin(String initFun, String matchFun, Method method) {
            super(initFun, matchFun, method);
        }

        @Override
        protected String type() {
            return "[AfterThrowing]";
        }

        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            try {
                return decorator.proceed();
            } catch (Throwable e) {
                Object exReturn = invokeAfterThrowingMethod(method, decorator, e);
                if (void.class == method.getReturnType() || Void.class == method.getReturnType()) {
                    throw e;
                }
                return exReturn;
            }
        }
    }

    /**
     * 基于反射的环绕增强插件
     */
    class ReflectAroundProxyPlugin extends ReflectProxyPlugin {

        ReflectAroundProxyPlugin(String initFun, String matchFun, Method method) {
            super(initFun, matchFun, method);
            Class<?> returnType = method.getReturnType();
            if (returnType != Object.class) {
                throw new PluginException("The method return value of the plugin method of type @Around must be 'java.lang.Object': {}", method);
            }
            boolean hasPD = false;
            for (Parameter parameter : method.getParameters()) {
                Class<?> type = parameter.getType();
                if (ProxyDecorator.class.isAssignableFrom(type)) {
                    hasPD = true;
                    break;
                }
            }
            if (!hasPD) {
                throw new PluginException("The parameter list of the plugin method of type @Around must contain the parameter of type '{}': {}", ProxyDecorator.class.getName(), method);
            }
        }

        @Override
        protected String type() {
            return "[Around]";
        }

        @Override
        public Object decorate(ProxyDecorator decorator) throws Throwable {
            return invokeAroundMethod(method, decorator);
        }
    }
}
