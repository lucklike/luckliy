package com.luckyframework.loosebind;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 12:59
 */
public class FieldInvalidException extends LooseBindingException{

    private Object factor;

    public Object getFactor() {
        return factor;
    }

    public FieldInvalidException(String exKey) {
        super(exKey);
    }

    public FieldInvalidException(String exKey, Object factor, Throwable ex) {
        super(exKey, ex);
        this.factor = factor;
    }
}
