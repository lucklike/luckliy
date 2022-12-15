package com.luckyframework.exception;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2020/12/17 下午11:44
 */
public class LuckyConversionTypeErrorException extends RuntimeException{

    public LuckyConversionTypeErrorException(Class<?> errType){
        super("错误的类型["+errType+"],无法执行代理！如果需要使用「@Conversion」类型转换代理，您需要实现[com.lucky.test.conversion.LuckyConversion]");
    }
}
