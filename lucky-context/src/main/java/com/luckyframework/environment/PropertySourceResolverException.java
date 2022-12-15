package com.luckyframework.environment;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/18 11:39
 */
public class PropertySourceResolverException extends RuntimeException{

    public PropertySourceResolverException(String message, Throwable e) {
        super(message, e);
    }
}
