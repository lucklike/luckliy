package com.luckyframework.datasources.v2;

import com.luckyframework.datasources.DataSourceManager;
import com.luckyframework.datasources.DataSourceRegistrationException;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/22 15:33
 */
public abstract class AbstractDataSourceManager implements DataSourceManager {

    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>(8);

    @Override
    public DataSource getDataSource(String dbname) throws Exception {
        return dataSourceMap.get(dbname);
    }

    @Override
    public Collection<DataSource> getDataSources() throws Exception {
        return dataSourceMap.values();
    }

    @Override
    public void addDataSource(String dbname, DataSource dataSource) {
        Assert.notNull(dbname,"Data source registration failed! dbname is null");
        Assert.notNull(dataSource,"Data source registration failed! data source is null");
        if (containsDataSource(dbname)){
            throw new DataSourceRegistrationException("Data source registration failed! The data source named '"+dbname+"' has been registered");
        }
        dataSourceMap.put(dbname, dataSource);
    }

    @Override
    public void removeDataSource(String dbname) {
        dataSourceMap.remove(dbname);
    }

    @Override
    public boolean containsDataSource(String dbname) {
        return dataSourceMap.containsKey(dbname);
    }
}
