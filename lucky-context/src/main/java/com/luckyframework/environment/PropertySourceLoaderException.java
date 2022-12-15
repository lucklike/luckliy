package com.luckyframework.environment;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/17 17:48
 */
public class PropertySourceLoaderException extends RuntimeException{

    public PropertySourceLoaderException(String message, Exception e) {
        super(message, e);
    }
}
