package com.luckyframework.jdbc.datasource.dynamic;

import com.luckyframework.annotations.Component;
import com.luckyframework.annotations.DependsOn;
import com.luckyframework.condition.ConditionalOnProperty;
import com.luckyframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * @author fk7075
 * @version 1.0
 * @date 2021/11/8 8:37 下午
 */
@Component("dynamicDataSource")
@DependsOn("com.luckyframework.datasources.LuckyDataSourceManager")
@ConditionalOnProperty("lucky.registerDynamicDataSource")
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }

}
