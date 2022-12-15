package com.luckyframework.jdbc.exceptions;

import org.springframework.core.NestedRuntimeException;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/11/8 8:09 下午
 */
public class DataSourceLookupFailureException extends NestedRuntimeException {

    public DataSourceLookupFailureException(String msg) {
        super(msg);
    }

    public DataSourceLookupFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
