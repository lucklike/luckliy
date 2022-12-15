package com.luckyframework.datasources;


import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 数据源构造器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/21 下午7:13
 */
public interface DataSourceBuilder {

    String DATA_SOURCE_PREFIX = "lucky.datasource";
    String DRIVER_CLASS_NAME = "driver-class-name";
    String JDBC_URL = "jdbc-url";
    String USER_NAME = "username";
    String PASSWORD = "password";
    String POOL_TYPE = "pool-type";

    /**
     * 返回一个数据源对象
     * @param environment 环境变量
     * @param dataSourceConfigPrefix 数据源配置的前缀
     * @return
     * @throws Exception
     */
    DataSource builder(Environment environment, String dataSourceConfigPrefix) throws Exception;

    /**
     * 数据连接池类型
     */
    String getPoolType();

    Class<? extends DataSource> getDataSourceType();
}
