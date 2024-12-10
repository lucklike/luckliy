package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.proxy.annotations.AutoCloseResponse;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

/**
 * 方法元信息获取能力接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/20 00:17
 */
public interface MethodMetaAcquireAbility {

    /**
     * 获取所有的参数信息
     *
     * @return 参数信息数组
     */
    Parameter[] getParameters();

    /**
     * 获取参数列表类型
     *
     * @return 参数列表类型
     */
    ResolvableType[] getParameterResolvableTypes();

    /**
     * 获取所有的参数名
     *
     * @return 参数列表对应的参数名
     */
    String[] getParameterNames();

    /**
     * 获取方法返回值类型{@link ResolvableType}
     *
     * @return 方法返回值类型ResolvableType
     */
    ResolvableType getReturnResolvableType();

    /**
     * 获取方法返回值类型{@link Class}
     *
     * @return 方法返回值类型Class
     */
    Class<?> getReturnType();

    /**
     * 判断当前方法是否是一个void方法
     *
     * @return 当前方法是否是一个void方法
     */
    boolean isVoidMethod();

    /**
     * 当前方法是否需要自动关闭资源
     * <pre>
     *     1.如果方法、类上上有被{@link AutoCloseResponse @AutoCloseResponse}注解标注，则是否自动关闭资源取决于{@link AutoCloseResponse#value()}
     *     2.检查当前方法的返回值是否为不必自动关闭资源的类型
     * </pre>
     *
     * @return 当前方法是否需要自动关闭资源
     */
    boolean needAutoCloseResource();

    /**
     * 当前方法使用禁止使用转换器
     *
     * @return 当前方法使用禁止使用转换器
     */
    boolean isConvertProhibition();

    /**
     * 当前方法是否为一个异步的void方法
     *
     * @return 当前方法是否为一个异步的void方法
     */
    boolean isAsyncMethod();

    /**
     * 是否为一个包装器方法
     *
     * @return 是否为一个包装器方法
     */
    boolean isWrapperMethod();

    /**
     * 执行包装器方法
     *
     * @return 执行结果
     */
    Object invokeWrapperMethod();

    /**
     * 当前方法是否是一个{@link Future}方法
     *
     * @return 当前方法是否是一个Future方法
     */
    boolean isFutureMethod();

    /**
     * 获取当前方法的真实返回值类型，如果是{@link Future}方法则返回泛型类型
     *
     * @return 获取当前方法的真实返回值类型
     */
    Type getRealMethodReturnType();

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
    String getSimpleSignature();
}
