package com.luckyframework.jdbc.core;

import com.luckyframework.jdbc.utils.JdbcUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.sql.*;
import java.util.Map;

public class ColumnMapRowMapper implements RowMapper<Map<String, Object>>{
    @Override
    public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        Map<String, Object> mapOfColumnValues = createColumnMap(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String column = JdbcUtils.lookupColumnName(rsmd, i);
            mapOfColumnValues.putIfAbsent(getColumnKey(column),getColumnValue(rs, i));
        }
        return mapOfColumnValues;
    }

    protected Map<String,Object> createColumnMap(int columnCount){
        return new LinkedCaseInsensitiveMap<>(columnCount);
    }

    protected String getColumnKey(String columnName){
        return columnName;
    }

    protected Object getColumnValue(ResultSet rs,int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }
}
