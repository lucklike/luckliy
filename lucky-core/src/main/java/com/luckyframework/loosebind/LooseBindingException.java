package com.luckyframework.loosebind;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 12:56
 */
public class LooseBindingException extends Exception{

    private final String exKey;

    public String getExKey() {
        return exKey;
    }

    public LooseBindingException(String exKey) {
        super("An exception occurred while configuring the '" + exKey + "' binding property of the item");
        this.exKey = exKey;
    }

    public LooseBindingException(String exKey, Throwable ex) {
        super("An exception occurred while configuring the '" + exKey + "' binding property of the item", ex);
        this.exKey = exKey;
    }
}
