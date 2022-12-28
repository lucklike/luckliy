package com.luckyframework.loosebind;

import java.util.Set;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/25 00:33
 */
public class NotFoundRequiredFieldException extends Exception {
    public NotFoundRequiredFieldException(String message) {
        super(message);
    }

    public NotFoundRequiredFieldException(Set<String> requiredFields) {
        this("The necessary attribute "+requiredFields+" was not found in the configuration.");
    }

}
