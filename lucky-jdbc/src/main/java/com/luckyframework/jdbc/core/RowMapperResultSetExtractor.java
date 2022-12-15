package com.luckyframework.jdbc.core;

import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>>{

    private final RowMapper<T> rowMapping;
    private final int rowsExpected;

    public RowMapperResultSetExtractor(RowMapper<T> rowMapping) {
        this(rowMapping,0);
    }

    public RowMapperResultSetExtractor(RowMapper<T> rowMapping, int rowsExpected) {
        Assert.notNull(rowMapping,"RowMapping is required");
        this.rowMapping = rowMapping;
        this.rowsExpected = rowsExpected;
    }


    @Override
    public List<T> extractData(ResultSet rs) throws SQLException {
        List<T> result = this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>();
        int rowNum = 0;
        while (rs.next()){
            result.add(this.rowMapping.mapRow(rs,rowNum));
        }
        return result;
    }
}
