package com.luckyframework.jdbc.connection;

import java.util.List;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/30 11:19 上午
 */
public interface OperationInterceptorRegistry {

    List<OperationInterceptor> getAllInterceptors();

    void registryInterceptor(OperationInterceptor interceptor);
}
