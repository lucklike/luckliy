package com.luckyframework.exception;

import java.lang.reflect.Field;

/**
 * 获取配置信息异常
 * @author fk
 * @version 1.0
 * @date 2021/1/21 0021 14:58
 */
public class GetConfigurationInfoException extends RuntimeException {

    public GetConfigurationInfoException(String $prefix, Throwable e){
        super("获取配置信息时出现错误，意外的表达式：`"+$prefix+"`",e);
    }

    public GetConfigurationInfoException(String msg){
        super(msg);
    }

    public GetConfigurationInfoException(Field field, String $prefix, Throwable e){
        super(String.format("为`%s`注入配置信息时出现错误！意外的表达式：`%s`", field, $prefix),e);

    }
}
