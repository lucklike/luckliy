package com.luckyframework.transaction.core;


import org.springframework.lang.Nullable;

public interface TransactionDefinition {

    //传播行为

    int PROPAGATION_REQUIRED = 0;

    int PROPAGATION_SUPPORTS = 1;

    int PROPAGATION_MANDATORY = 2;

    int PROPAGATION_REQUIRES_NEW = 3;

    int PROPAGATION_NOT_SUPPORTED = 4;

    int PROPAGATION_NEVER = 5;

    int PROPAGATION_NESTED = 6;

    // 隔离级别

    int ISOLATION_DEFAULT = -1;

    int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;

    int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;

    int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;

    int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;

    //超时时间

    int TIMEOUT_DEFAULT = -1;


    /** 获取事务的传播行为*/
    default int getPropagationBehavior() {
        return PROPAGATION_REQUIRED;
    }

    /** 获取事务的隔离级别*/
    default int getIsolationLevel() {
        return ISOLATION_DEFAULT;
    }

    /** 获取事务的超时时间*/
    default int getTimeout() {
        return TIMEOUT_DEFAULT;
    }

    /** 是否为只读事务*/
    default boolean isReadOnly() {
        return false;
    }

    /** 获取事务的别名*/
    @Nullable
    default String getName() {
        return null;
    }


}
