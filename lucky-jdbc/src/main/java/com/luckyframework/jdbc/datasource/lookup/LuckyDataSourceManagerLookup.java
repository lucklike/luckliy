package com.luckyframework.jdbc.datasource.lookup;

import com.luckyframework.datasources.LuckyDataSourceManager;
import com.luckyframework.jdbc.exceptions.DataSourceLookupFailureException;

import javax.sql.DataSource;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/11/8 8:05 下午
 */
public class LuckyDataSourceManagerLookup implements DataSourceLookup{

    @Override
    public DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException {
        try {
            return LuckyDataSourceManager.getDataSourceByName(dataSourceName);
        }catch (Exception e){
            throw new DataSourceLookupFailureException("An exception occurred while looking for a data source named '"+dataSourceName+"'.",e);
        }

    }
}
