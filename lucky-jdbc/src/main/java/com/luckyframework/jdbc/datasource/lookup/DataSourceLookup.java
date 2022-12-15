package com.luckyframework.jdbc.datasource.lookup;

import com.luckyframework.jdbc.exceptions.DataSourceLookupFailureException;

import javax.sql.DataSource;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/11/8 7:58 下午
 */
public interface DataSourceLookup {

    DataSource getDataSource(String dataSourceName) throws DataSourceLookupFailureException;
}
