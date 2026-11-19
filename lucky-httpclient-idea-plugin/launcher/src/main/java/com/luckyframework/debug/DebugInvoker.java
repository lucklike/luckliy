package com.luckyframework.debug;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * invoke 请求的执行器：定位接口方法、取代理对象、绑定参数、编码结果。
 *
 * 代理对象来自容器中的 __luckyHttpClientProxyFactory__，
 * 与 starter 内部注入使用的入口完全一致。
 */
final class DebugInvoker {

    private static final Map<Class<?>, Class<?>> WRAPPERS = new HashMap<Class<?>, Class<?>>();

    static {
        WRAPPERS.put(boolean.class, Boolean.class);
        WRAPPERS.put(byte.class, Byte.class);
        WRAPPERS.put(short.class, Short.class);
        WRAPPERS.put(int.class, Integer.class);
        WRAPPERS.put(long.class, Long.class);
        WRAPPERS.put(float.class, Float.class);
        WRAPPERS.put(double.class, Double.class);
        WRAPPERS.put(char.class, Character.class);
    }

    private final DebugContainer container;
    private final DebugArgumentBinder binder = new DebugArgumentBinder();
    private final DebugResultEncoder encoder = new DebugResultEncoder();

    DebugInvoker(DebugContainer container) {
        this.container = container;
    }

    JsonNode invoke(JsonNode request) throws DebugException {
        String className = DebugJson.text(request, "className", "");
        String methodName = DebugJson.text(request, "methodName", "");
        if (className.isEmpty() || methodName.isEmpty()) {
            throw new DebugException(DebugProtocol.ERROR_INTERNAL,
                    "className/methodName 不能为空", null);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> apiClass = loadClass(className, loader);
        Method method = findMethod(apiClass, methodName, request.path("parameterTypes"));

        Object factory = container.factory();
        Object proxy = proxyOf(factory, apiClass);
        Object[] arguments = binder.bind(request.path("arguments"), method.getGenericParameterTypes());

        try {
            Object result = method.invoke(proxy, arguments);
            return encoder.encode(method, result);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new DebugException(DebugProtocol.ERROR_INVOCATION_TARGET,
                    DebugJson.messageOf(cause), DebugJson.stackOf(cause));
        } catch (IllegalArgumentException e) {
            throw new DebugException(DebugProtocol.ERROR_ARGUMENT_CONVERT,
                    DebugJson.messageOf(e), DebugJson.stackOf(e));
        } catch (IllegalAccessException e) {
            throw new DebugException(DebugProtocol.ERROR_INTERNAL,
                    DebugJson.messageOf(e), DebugJson.stackOf(e));
        }
    }

    private Class<?> loadClass(String name, ClassLoader loader) throws DebugException {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            throw new DebugException(DebugProtocol.ERROR_CLASS_NOT_FOUND,
                    "接口类不存在: " + name, DebugJson.stackOf(e));
        } catch (LinkageError e) {
            throw new DebugException(DebugProtocol.ERROR_CLASS_NOT_FOUND,
                    "接口类加载失败: " + name + "，" + DebugJson.messageOf(e), DebugJson.stackOf(e));
        }
    }

    /**
     * 按名称、参数个数和参数类型定位重载。
     *
     * parameterTypes 缺失时退回「第一个同名同参数的 public 方法」，
     * 用于方法没有重载的常见场景。
     */
    private Method findMethod(Class<?> apiClass, String name, JsonNode types) throws DebugException {
        int expectedCount = types != null && types.isArray() ? types.size() : -1;
        Method fallback = null;
        for (Method method : apiClass.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            if (expectedCount >= 0 && method.getParameterCount() != expectedCount) {
                continue;
            }
            if (fallback == null) {
                fallback = method;
            }
            if (matches(method.getParameterTypes(), types)) {
                return method;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new DebugException(DebugProtocol.ERROR_METHOD_NOT_FOUND,
                "方法不存在: " + apiClass.getName() + "#" + name, null);
    }

    private boolean matches(Class<?>[] actual, JsonNode types) {
        if (types == null || !types.isArray()) {
            return true;
        }
        if (actual.length != types.size()) {
            return false;
        }
        for (int i = 0; i < actual.length; i++) {
            if (!sameType(actual[i], types.get(i).asText())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameType(Class<?> actual, String expected) {
        if (expected == null || expected.trim().isEmpty()) {
            return true;
        }
        if (actual.getTypeName().equals(expected) || actual.getName().equals(expected)) {
            return true;
        }
        if (actual.isPrimitive()) {
            Class<?> wrapper = WRAPPERS.get(actual);
            return wrapper != null
                    && (wrapper.getTypeName().equals(expected) || wrapper.getName().equals(expected));
        }
        return false;
    }

    private Object proxyOf(Object factory, Class<?> apiClass) throws DebugException {
        try {
            Method method = factory.getClass().getMethod("getProxyObject", Class.class);
            return method.invoke(factory, apiClass);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new DebugException(DebugProtocol.ERROR_INVOCATION_TARGET,
                    "创建代理失败: " + DebugJson.messageOf(cause), DebugJson.stackOf(cause));
        } catch (Throwable e) {
            throw new DebugException(DebugProtocol.ERROR_INVOCATION_TARGET,
                    "创建代理失败: " + DebugJson.messageOf(e), DebugJson.stackOf(e));
        }
    }
}
