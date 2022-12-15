package com.luckyframework.datasources;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/21 0021 16:41
 */
public class C3P0DataSourceBuilder extends AbstractDataSourceBuilder {


    private final static String AUTO_COMMIT                     = "auto-commit";
    private final static String ACQUIRE_INCREMENT               = "acquire-increment";
    private final static String INITIAL_POOL_SIZE               = "initial-pool-size";
    private final static String MAX_POOL_SIZE                   = "max-pool-size";
    private final static String MIN_POOL_SIZE                   = "min-pool-size";
    private final static String MAX_IDLE_TIME                   = "max-idle-time";
    private final static String MAX_CONNECTION_AGE              = "max-connection-age";
    private final static String MAX_STATEMENTS                  = "max-statements";
    private final static String MAX_STATEMENTS_PER_CONNECTION   = "max-statements-per-connection";
    private final static String CHECKOUT_TIMEOUT                = "checkout-timeout";


    @Override
    protected DataSource createDataSource() throws Exception {
        ComboPooledDataSource dataSource =new ComboPooledDataSource(true);
        dataSource.setJdbcUrl(getConfValue(JDBC_URL,String.class));
        dataSource.setDriverClass(getConfValue(DRIVER_CLASS_NAME,String.class));
        dataSource.setUser(getConfValue(USER_NAME,String.class));
        dataSource.setPassword(getConfValue(PASSWORD,String.class));
        dataSource.setAutoCommitOnClose(getConfValue(AUTO_COMMIT,boolean.class,true));
        dataSource.setAcquireIncrement(getConfValue(ACQUIRE_INCREMENT,int.class,3));
        dataSource.setInitialPoolSize(getConfValue(INITIAL_POOL_SIZE,int.class,3));
        dataSource.setMaxPoolSize(getConfValue(MAX_POOL_SIZE,int.class,15));
        dataSource.setMinPoolSize(getConfValue(MIN_POOL_SIZE,int.class,1));
        dataSource.setMaxIdleTime(getConfValue(MAX_IDLE_TIME,int.class,0));
        dataSource.setMaxStatements(getConfValue(MAX_STATEMENTS,int.class,0));
        dataSource.setMaxConnectionAge(getConfValue(MAX_CONNECTION_AGE,int.class,0));
        dataSource.setCheckoutTimeout(getConfValue(CHECKOUT_TIMEOUT,int.class,30000));
        dataSource.setMaxStatementsPerConnection(getConfValue(MAX_STATEMENTS_PER_CONNECTION,int.class,0));
        return dataSource;
    }

    @Override
    public String getPoolType() {
        return "C3P0";
    }

    @Override
    public Class<? extends DataSource> getDataSourceType() {
        return ComboPooledDataSource.class;
    }
}
