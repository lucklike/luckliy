package com.luckyframework.datasources;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源管理器
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/21 下午7:12
 */
public interface DataSourceManager {

    Map<String, DataSourceBuilder> dataSourceBuilderMap = new ConcurrentHashMap<>();
    Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    String DEFAULT_DBNAME = "defaultDB";

    /** 根据dbname获取一个数据源*/
    DataSource getDataSource(String dbname) throws Exception;

    default DataSource getDefaultDataSource() throws Exception {
        return getDataSource(DEFAULT_DBNAME);
    }

    /** 获取所有已经注册的数据源 */
    Collection<DataSource> getDataSources() throws Exception;

    /** 添加一个数据源 */
    void addDataSource(String dbname, DataSource dataSource);

    /** 移除一个数据源 */
    void removeDataSource(String dbname);

    /** 判断一个数据源是否已经存在 */
    boolean containsDataSource(String dbname);


    //-----------------------------------------
    //           DataSourceBuilder
    //------------------------------------------


    static DataSourceBuilder getDataSourceBuilder(String poolType) {
        return dataSourceBuilderMap.get(poolType.toUpperCase());
    }

    static void registerDataSourceBuilder(DataSourceBuilder dataSourceBuilder) {
        dataSourceBuilderMap.put(dataSourceBuilder.getPoolType().toUpperCase(), dataSourceBuilder);
    }

    static boolean containsDataSourceBuilder(DataSourceBuilder dataSourceBuilder) {
        return dataSourceBuilderMap.containsKey(dataSourceBuilder.getPoolType().toUpperCase());
    }

    static void removeDataSourceBuilder(String poolType) {
        dataSourceBuilderMap.remove(poolType);
    }
}
