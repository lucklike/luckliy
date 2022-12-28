package com.luckyframework.loosebind;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 12:58
 */
public class FieldUnknownException extends LooseBindingException{
    public FieldUnknownException(String exKey) {
        super(exKey);
    }
}
