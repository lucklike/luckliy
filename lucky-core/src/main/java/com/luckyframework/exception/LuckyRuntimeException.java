package com.luckyframework.exception;

import com.luckyframework.common.StringUtils;
import org.slf4j.Logger;

/**
 * Lucky运行是异常
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/27 06:34
 */
public class LuckyRuntimeException extends RuntimeException {

    private String message = "";


    public LuckyRuntimeException(String message){
        super(message);
        this.message = message;
    }

    public LuckyRuntimeException(Throwable ex){
        super(ex);
        this.message = "The nested exception is[" +ex.getMessage() + "]";
    }

    public LuckyRuntimeException(String message, Throwable ex){
        super(message, ex);
        this.message = message;
    }

    public LuckyRuntimeException(String messageTemplate, Object... args){
        this(StringUtils.format(messageTemplate, args));
        this.message = StringUtils.format(messageTemplate, args);
    }

    public LuckyRuntimeException(Throwable ex, String messageTemplate, Object... args){
        this(StringUtils.format(messageTemplate, args), ex);
    }

    public LuckyRuntimeException printException(Logger logger){
        logger.error(message, this);
        return this;
    }


}
