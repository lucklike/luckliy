package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerateUtil;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.exeception.FallbackException;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.reflect.MethodUtils;
import com.luckyframework.spel.SpelExpressionExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 支持降级处理的异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/01 16:38
 */
public class ExceptionFallbackHandle extends AbstractHttpExceptionHandle {

    private static final Logger log = LoggerFactory.getLogger(ExceptionFallbackHandle.class);

    @Override
    public Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        if (log.isDebugEnabled()) {
            log.debug("The HTTP interface {} call fails, the demotion process will be used...", request.getUrl());
        }
        ExceptionFallback fallbackAnn = methodContext.getMergedAnnotationCheckParent(ExceptionFallback.class);
        Class<?> proxyClass = methodContext.getClassContext().getCurrentAnnotatedElement();

        // 优先使用降级SpEL表达式来获取降级实现类实例对象
        String fallbackExp = fallbackAnn.fallbackExp();
        if (StringUtils.hasText(fallbackExp)) {
            Object fallbackInstance;
            try {
                fallbackInstance = methodContext.parseExpression(fallbackExp, proxyClass);
            }catch (SpelExpressionExecuteException e) {
                throw new FallbackException(e, "An exception occurred while obtaining the demoted implementation class of the '{}' interface using the SpEL expression '{}'", proxyClass, fallbackExp);
            }
            return invokeFallBackMethod(fallbackInstance, methodContext);
        }

        // 使用生成器对象来生成一个降级实现类对象
        ObjectGenerate fallbackGenerate = fallbackAnn.fallbackGenerate();
        if (ObjectGenerateUtil.isEffectiveObjectGenerate(fallbackGenerate, Void.class)) {
            Object fallbackInstance;
            try {
                fallbackInstance = methodContext.generateObject(fallbackGenerate);
            }catch (Exception e) {
                throw new FallbackException(e, "The demotion implementation class object of {} cannot be obtained by annotating the demotion generator {}!", proxyClass, fallbackGenerate);
            }
            return invokeFallBackMethod(fallbackInstance, methodContext);
        }

        // 使用配置的Class对象来生成降级实现类对象
        Class<?> fallbackClass = fallbackAnn.fallback();
        if (proxyClass.isAssignableFrom(fallbackClass)) {
            Object fallbackInstance = ClassUtils.newObject(fallbackClass);
            return invokeFallBackMethod(fallbackInstance, methodContext);
        }

        if (fallbackClass == Void.class) {
            throw new FallbackException("No degraded configuration, please check the configuration!");
        } else {
            throw new FallbackException("The configured downgrade implementation class type '{}' is incompatible with the current API type '{}'", fallbackClass, proxyClass);
        }

    }

    /**
     * 执行降级方法，返回方法执行后的结果
     *
     * @param fallbackInstance 降级实现类实例
     * @param methodContext    方法上下文实例
     * @return 降级方法执行后的结果
     */
    private Object invokeFallBackMethod(Object fallbackInstance, MethodContext methodContext) {
        return MethodUtils.invoke(fallbackInstance, methodContext.getCurrentAnnotatedElement(), methodContext.getArguments());
    }
}
