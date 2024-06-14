package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.exception.ResponseProcessException;
import com.luckyframework.httpclient.proxy.annotations.VoidResultConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/18 01:55
 */
public abstract class AbstractSpELVoidResponseConvert implements VoidResponseConvert {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpELVoidResponseConvert.class);

    /**
     * 主动抛出异常，如果表达式返回的是异常实例则直接抛出，否则抛出{@link ActivelyThrownException}异常
     *
     * @param context   转换注解上下文对象
     * @param exception 异常表达式
     * @throws Throwable 异常
     */
    protected void throwException(ConvertContext context, String exception) throws Throwable {
        Object exObj = context.parseExpression(exception);
        if (exObj instanceof Throwable) {
            throw (Throwable) exObj;
        }
        throw new ActivelyThrownException(String.valueOf(exObj));
    }


    /**
     * 获取默认值，如果存在默认值则返回默认值，否则返回null
     *
     * @param context 方法上下文
     * @param <T>     默认值的类型
     * @return 默认值
     * @throws Throwable 异常
     */
    protected <T> T getDefaultValue(ConvertContext context) throws Throwable {
        VoidResultConvert voidResultConvertAnn = context.toAnnotation(VoidResultConvert.class);
        String defaultValueSpEL = voidResultConvertAnn.defaultValue();
        String exception = voidResultConvertAnn.exception();
        if (StringUtils.hasText(defaultValueSpEL)) {
            if (log.isDebugEnabled()) {
                log.debug("The current request returns the default value :{}", defaultValueSpEL);
            }
            return context.parseExpression(defaultValueSpEL, context.getRealMethodReturnType());
        }
        if (StringUtils.hasText(exception)) {
            Object exObj = context.parseExpression(exception);
            if (exObj instanceof Throwable) {
                throw (Throwable) exObj;
            }
            throw new ActivelyThrownException(
                    String.valueOf((Object) context.parseExpression(exception))
            );
        }
        return null;
    }
}
