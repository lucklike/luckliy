package com.luckyframework.datasources;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import com.luckyframework.serializable.SerializationTypeToken;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;

/**
 * DruidDataSource构建器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/21 下午7:34
 */
public class DruidDataSourceBuilder extends AbstractDataSourceBuilder{

    private final static String AUTO_COMMIT                            = "auto-commit";
    private final static String INITIAL_SIZE                           = "initial-size";
    private final static String MAX_ACTIVE                             = "max-active";
    private final static String MAX_IDLE                               = "max-idle";
    private final static String MIN_IDLE                               = "min-idle";
    private final static String MAX_WAIT                               = "max-wait";
    private final static String POOL_PREPARED_STATEMENTS               = "pool-prepared-statements";
    private final static String MAX_OPEN_PREPARED_STATEMENTS           = "max-open-prepared-statements";
    private final static String VALIDATION_QUERY                       = "validation-query";
    private final static String TEST_ON_BORROW                         = "test-on-borrow";
    private final static String TEST_ON_RETURN                         = "test-on-return";
    private final static String TEST_WHILE_IDLE                        = "test-while-idle";
    private final static String TIME_BETWEEN_EVICTION_RUNS_MILLIS      = "time-between-eviction-runs-millis";
    private final static String NUM_TESTS_PER_EVICTION_RUN             = "num-tests-per-eviction-run";
    private final static String MIN_EVICTABLE_IDLE_TIME_MILLIS         = "min-evictable-idle-time-millis";
    private final static String CONNECTION_INIT_SQLS                   = "connection-init-sqls";
    private final static String EXCEPTION_SORTER                       = "exception-sorter";
    private final static String FILTERS                                = "filters";
    private final static String PROXY_FILTERS                          = "proxy-filters";


    @Override
    protected DataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(getConfValue(DRIVER_CLASS_NAME,String.class));
        dataSource.setUrl(getConfValue(JDBC_URL,String.class));
        dataSource.setUsername(getConfValue(USER_NAME,String.class));
        dataSource.setPassword(getConfValue(PASSWORD,String.class));
        dataSource.setDefaultAutoCommit(getConfValue(AUTO_COMMIT,boolean.class,true));
        dataSource.setInitialSize(getConfValue(INITIAL_SIZE,int.class,0));
        dataSource.setMaxActive(getConfValue(MAX_ACTIVE,int.class,8));
        dataSource.setMaxIdle(getConfValue(MAX_IDLE,int.class,8));
        dataSource.setMinIdle(getConfValue(MIN_IDLE,int.class));//
        dataSource.setMaxWait(getConfValue(MAX_WAIT,long.class));
        dataSource.setPoolPreparedStatements(getConfValue(POOL_PREPARED_STATEMENTS,boolean.class,false));
        dataSource.setMaxOpenPreparedStatements(getConfValue(MAX_OPEN_PREPARED_STATEMENTS,int.class,-1));
        dataSource.setValidationQuery(getConfValue(VALIDATION_QUERY,String.class));
        dataSource.setTestOnBorrow(getConfValue(TEST_ON_BORROW,boolean.class,true));
        dataSource.setTestOnReturn(getConfValue(TEST_ON_RETURN,boolean.class,false));
        dataSource.setTestWhileIdle(getConfValue(TEST_WHILE_IDLE,boolean.class,false));
        dataSource.setTimeBetweenEvictionRunsMillis(getConfValue(TIME_BETWEEN_EVICTION_RUNS_MILLIS,long.class));
        dataSource.setNumTestsPerEvictionRun(getConfValue(NUM_TESTS_PER_EVICTION_RUN,int.class));
        dataSource.setMinEvictableIdleTimeMillis(getConfValue(MIN_EVICTABLE_IDLE_TIME_MILLIS,long.class));
        dataSource.setConnectionInitSqls(getConfValue(CONNECTION_INIT_SQLS,new SerializationTypeToken<Set<String>>(){}));
        dataSource.setFilters(getConfValue(FILTERS,String.class));
        dataSource.setProxyFilters(getConfValue(PROXY_FILTERS, new SerializationTypeToken<List<Filter>>(){}));
        dataSource.setExceptionSorter(getConfValue(EXCEPTION_SORTER,String.class));
        return dataSource;
    }

    @Override
    public String getPoolType() {
        return "Druid";
    }

    @Override
    public Class<? extends DataSource> getDataSourceType() {
        return DruidDataSource.class;
    }
}
