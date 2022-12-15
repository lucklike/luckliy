package com.luckyframework.datasources;

import com.luckyframework.reflect.ClassUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/21 0021 11:27
 */
public class HikariCPDataSourceBuilder extends AbstractDataSourceBuilder {

    private final static String DATA_SOURCE_CLASS_NAME      = "data-source-class-name";
    private final static String AUTO_COMMIT                 = "auto-commit";
    private final static String CONNECTION_TIMEOUT          = "connection-timeout";
    private final static String IDLE_TIMEOUT                = "idle-timeout";
    private final static String MAX_LIFE_TIME               = "max-life-time";
    private final static String CONNECTION_TEST_QUERY       = "connection-test-query";
    private final static String MINIMUM_IDLE                = "minimum-idle";
    private final static String MAXIMUM_POOL_SIZE           = "maximum-pool-size";
    private final static String POOL_NAME                   = "pool-name";
    private final static String METRIC_REGISTRY             = "metric-registry";
    private final static String HEALTH_CHECK_REGISTRY       = "health-check-registry";
    private final static String INITIALIZATION_FAIL_TIMEOUT = "initialization-fail-timeout";
    private final static String ISOLATE_INTERNAL_QUERIES    = "isolate-internal-queries";
    private final static String ALLOW_POOL_SUSPENSION       = "allow-pool-suspension";
    private final static String READ_ONLY                   = "read-only";
    private final static String REGISTER_MBEANS             = "register-mbeans";
    private final static String CATALOG                     = "catalog";
    private final static String CONNECTION_INIT_SQL         = "connection-init-sql";
    private final static String TRANSACTION_ISOLATION       = "transaction-isolation";
    private final static String VALIDATION_TIMEOUT          = "validation-timeout";
    private final static String LEAK_DETECTION_THRESHOLD    = "leak-detection-threshold";
    private final static String DATA_SOURCE                 = "data-source";
    private final static String SCHEMA                      = "schema";
    private final static String THREAD_FACTORY              = "thread-factory";
    private final static String SCHEDULED_EXECUTOR          = "scheduled-executor";


    @Override
    protected DataSource createDataSource() {
        return new HikariDataSource(createHikariConfig());
    }

    private HikariConfig createHikariConfig(){
        HikariConfig hikariCfg = new HikariConfig();
        hikariCfg.setDriverClassName(getConfValue(DRIVER_CLASS_NAME,String.class));
        hikariCfg.setJdbcUrl(getConfValue(JDBC_URL,String.class));
        hikariCfg.setUsername(getConfValue(USER_NAME,String.class));
        hikariCfg.setPassword(getConfValue(PASSWORD,String.class));
        hikariCfg.setDataSourceClassName(getConfValue(DATA_SOURCE_CLASS_NAME,String.class));
        hikariCfg.setAutoCommit(getConfValue(AUTO_COMMIT,boolean.class,true));
        hikariCfg.setConnectionTimeout(getConfValue(CONNECTION_TIMEOUT,Long.class,30000L));
        hikariCfg.setIdleTimeout(getConfValue(IDLE_TIMEOUT,long.class,600000L));
        hikariCfg.setMaxLifetime(getConfValue(MAX_LIFE_TIME,long.class,1800000L));
        hikariCfg.setMinimumIdle(getConfValue(MINIMUM_IDLE,int.class,10));
        hikariCfg.setConnectionTestQuery(getConfValue(CONNECTION_TEST_QUERY,String.class,"SELECT 1"));
        hikariCfg.setMaximumPoolSize(getConfValue(MAXIMUM_POOL_SIZE,int.class,10));
        String metricRegistry = getConfValue(METRIC_REGISTRY, String.class);
        if(metricRegistry!=null) hikariCfg.setMetricRegistry(ClassUtils.newObject(metricRegistry));
        String healthCheckRegistry = getConfValue(HEALTH_CHECK_REGISTRY, String.class);
        if(healthCheckRegistry!=null) hikariCfg.setHealthCheckRegistry(ClassUtils.newObject(healthCheckRegistry));
        String poolName = getConfValue(POOL_NAME, String.class);
        if(poolName!=null) hikariCfg.setPoolName(poolName);
        hikariCfg.setIsolateInternalQueries(getConfValue(ISOLATE_INTERNAL_QUERIES,boolean.class,false));
        hikariCfg.setAllowPoolSuspension(getConfValue(ALLOW_POOL_SUSPENSION,boolean.class,false));
        hikariCfg.setReadOnly(getConfValue(READ_ONLY,boolean.class,false));
        hikariCfg.setRegisterMbeans(getConfValue(REGISTER_MBEANS,boolean.class,false));
        hikariCfg.setInitializationFailTimeout(getConfValue(INITIALIZATION_FAIL_TIMEOUT,long.class,1L));
        hikariCfg.setConnectionInitSql(getConfValue(CONNECTION_INIT_SQL,String.class));
        hikariCfg.setLeakDetectionThreshold(getConfValue(LEAK_DETECTION_THRESHOLD,long.class,0L));
        hikariCfg.setDataSource(getConfValue(DATA_SOURCE,DataSource.class));
        hikariCfg.setSchema(getConfValue(SCHEMA,String.class));
        hikariCfg.setCatalog(getConfValue(CATALOG,String.class));
        hikariCfg.setValidationTimeout(getConfValue(VALIDATION_TIMEOUT,long.class, 5000L));
        hikariCfg.setTransactionIsolation(getConfValue(TRANSACTION_ISOLATION,String.class));
        hikariCfg.setThreadFactory(getConfValue(THREAD_FACTORY, ThreadFactory.class));
        hikariCfg.setScheduledExecutor(getConfValue(SCHEDULED_EXECUTOR, ScheduledExecutorService.class));
        return hikariCfg;
    }

    @Override
    public String getPoolType() {
        return "HikariCP";
    }

    @Override
    public Class<? extends DataSource> getDataSourceType() {
        return HikariDataSource.class;
    }
}
