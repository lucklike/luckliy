package com.luckyframework.debug;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 用户 Spring 容器的持有者。
 *
 * Spring Boot / Spring Framework 全部通过反射访问，避免 launcher
 * 的编译期版本与用户项目绑定；同一份 launcher 可运行在 Boot 2.7 与 3.x 上。
 */
final class DebugContainer {

    private volatile String state = DebugProtocol.STATE_STARTING;
    private volatile Object context;
    private volatile String mainClass = "";
    private volatile String failureMessage;
    private volatile String failureStack;

    /**
     * 启动用户容器。supplement 用于补注册 starter 的自动配置，
     * 使未标注 @EnableLuckyHttpClient 的项目也能拿到工厂 bean。
     */
    void start(String mainClassName, String[] args) {
        this.mainClass = mainClassName;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> mainType = Class.forName(mainClassName, false, loader);
            Object builder = newBuilder(mainType, LuckyDebugSupplementConfiguration.class);
            Object started = run(builder, args);
            this.context = started;
            this.state = DebugProtocol.STATE_READY;
        } catch (Throwable error) {
            Throwable cause = unwrap(error);
            if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
                String missing = cause.getMessage() == null ? mainClassName : cause.getMessage();
                this.failureMessage = "找不到类 " + missing
                        + "，请先 Build Project 并确认该类在模块输出或依赖中";
            } else {
                this.failureMessage = DebugJson.messageOf(cause);
            }
            this.failureStack = DebugJson.stackOf(cause);
            this.state = DebugProtocol.STATE_FAILED;
            cause.printStackTrace();
        }
    }

    /** 取工厂 bean；首次取值会触发 LuckyHttpAutoConfiguration 的完整初始化。 */
    Object factory() throws DebugException {
        Object ctx = context;
        if (ctx == null) {
            throw new DebugException(DebugProtocol.ERROR_CONTAINER_STARTING,
                    "Spring 容器尚未就绪", null);
        }
        try {
            Class<?> contextType = Class.forName(DebugProtocol.CONTEXT_TYPE);
            Method getBean = contextType.getMethod("getBean", String.class);
            Object bean = getBean.invoke(ctx, DebugProtocol.FACTORY_BEAN_NAME);
            if (bean == null) {
                throw new DebugException(DebugProtocol.ERROR_BEAN_MISSING,
                        "容器中不存在 " + DebugProtocol.FACTORY_BEAN_NAME, null);
            }
            Class<?> expected = Class.forName(DebugProtocol.FACTORY_TYPE);
            if (!expected.isInstance(bean)) {
                throw new DebugException(DebugProtocol.ERROR_BEAN_TYPE_MISMATCH,
                        "bean 类型不匹配，实际为 " + bean.getClass().getName(), null);
            }
            return bean;
        } catch (DebugException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new DebugException(DebugProtocol.ERROR_BEAN_MISSING,
                    "类路径中缺少 " + DebugProtocol.FACTORY_TYPE, DebugJson.stackOf(e));
        } catch (NoSuchMethodException e) {
            throw new DebugException(DebugProtocol.ERROR_BEAN_MISSING,
                    "容器未提供 getBean(String)", DebugJson.stackOf(e));
        } catch (Throwable e) {
            Throwable cause = unwrap(e);
            throw new DebugException(DebugProtocol.ERROR_BEAN_MISSING,
                    "获取 " + DebugProtocol.FACTORY_BEAN_NAME + " 失败: " + DebugJson.messageOf(cause),
                    DebugJson.stackOf(cause));
        }
    }

    boolean beanPresent() {
        Object ctx = context;
        if (ctx == null) {
            return false;
        }
        try {
            Class<?> contextType = Class.forName(DebugProtocol.CONTEXT_TYPE);
            Method contains = contextType.getMethod("containsBean", String.class);
            return Boolean.TRUE.equals(contains.invoke(ctx, DebugProtocol.FACTORY_BEAN_NAME));
        } catch (Throwable e) {
            return false;
        }
    }

    void stop() {
        Object ctx = context;
        context = null;
        if (ctx == null) {
            state = DebugProtocol.STATE_STOPPED;
            return;
        }
        try {
            Class<?> contextType = Class.forName(DebugProtocol.CONTEXT_TYPE);
            contextType.getMethod("close").invoke(ctx);
        } catch (Throwable ignored) {
            // 关闭失败不应阻塞进程退出
        } finally {
            state = DebugProtocol.STATE_STOPPED;
        }
    }

    String state() {
        return state;
    }

    String mainClass() {
        return mainClass;
    }

    String failureMessage() {
        return failureMessage;
    }

    String failureStack() {
        return failureStack;
    }

    private Object newBuilder(Class<?>... sources) throws Exception {
        Class<?> type = Class.forName("org.springframework.boot.builder.SpringApplicationBuilder");
        Constructor<?> constructor = type.getConstructor(Class[].class);
        return constructor.newInstance((Object) sources);
    }

    private Object run(Object builder, String[] args) throws Exception {
        Method run = builder.getClass().getMethod("run", String[].class);
        return run.invoke(builder, (Object) args);
    }

    private Throwable unwrap(Throwable error) {
        if (error instanceof InvocationTargetException) {
            Throwable target = ((InvocationTargetException) error).getTargetException();
            return target == null ? error : target;
        }
        return error;
    }
}
