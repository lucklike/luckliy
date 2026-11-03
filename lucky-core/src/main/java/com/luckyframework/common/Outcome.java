package com.luckyframework.common;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 结果
 * <p>
 * 用于封装一次可能失败的操作结果：成功时携带业务结果值，失败时携带异常信息，
 * 调用方通过 {@link #isSuccess()} 判断结果状态，无需感知异常即可拿到失败原因。
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-11-03
 */
public class Outcome<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 纯失败（仅消息）时使用的异常占位消息前缀
     */
    private static final String MESSAGE_EXCEPTION_PREFIX = "Operation failed: ";

    private final boolean success;
    private final T result;
    private final Throwable throwable;

    private Outcome(boolean success, T result, Throwable throwable) {
        this.success = success;
        this.result = result;
        this.throwable = throwable;
    }

    /**
     * 构建成功结果
     *
     * @param result 业务结果值，允许为null
     */
    public static <T> Outcome<T> ok(T result) {
        return new Outcome<T>(true, result, null);
    }

    /**
     * 构建成功结果（结果为void场景）
     */
    public static Outcome<Void> ok() {
        return new Outcome<>(true, null, null);
    }

    /**
     * 构建失败结果
     *
     * @param throwable 导致失败的异常
     */
    public static <T> Outcome<T> fail(Throwable throwable) {
        return new Outcome<>(false, null, throwable);
    }

    /**
     * 构建失败结果（仅失败消息，无异常对象）
     *
     * @param message 失败消息
     */
    public static <T> Outcome<T> fail(String message) {
        return new Outcome<>(false, null, new IllegalStateException(MESSAGE_EXCEPTION_PREFIX + message));
    }

    /**
     * 执行一个可能抛异常的操作并封装为结果对象
     *
     * @param supplier 结果提供者
     */
    public static <T> Outcome<T> of(Supplier<T> supplier) {
        try {
            return ok(supplier.get());
        } catch (Throwable e) {
            return fail(e);
        }
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 是否失败
     */
    public boolean isFail() {
        return !success;
    }

    /**
     * 获取业务结果值，失败时返回null
     */
    public T getResult() {
        return result;
    }

    /**
     * 获取业务结果值，失败时返回默认值
     *
     * @param defaultValue 失败时的默认值
     */
    public T getResultOrDefault(T defaultValue) {
        return success ? result : defaultValue;
    }

    /**
     * 获取导致失败的异常，成功时返回null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * 获取失败原因消息，成功时返回null
     */
    public String getMessage() {
        return throwable == null ? null : throwable.getMessage();
    }

    /**
     * 成功时执行action
     *
     * @param action 成功回调，入参为业务结果值
     */
    public Outcome<T> onSuccess(Consumer<T> action) {
        if (success) {
            action.accept(result);
        }
        return this;
    }

    /**
     * 失败时执行action
     *
     * @param action 失败回调，入参为导致失败的异常
     */
    public Outcome<T> onFail(Consumer<Throwable> action) {
        if (!success) {
            action.accept(throwable);
        }
        return this;
    }


    @Override
    public String toString() {
        return "Outcome{" +
                "success=" + success +
                ", result=" + result +
                ", throwable=" + throwable +
                '}';
    }
}
