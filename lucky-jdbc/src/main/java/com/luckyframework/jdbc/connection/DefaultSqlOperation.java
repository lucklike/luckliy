package com.luckyframework.jdbc.connection;

import com.luckyframework.jdbc.core.ColumnMapRowMapper;
import com.luckyframework.jdbc.core.ResultSetExtractor;
import com.luckyframework.jdbc.core.RowMapperResultSetExtractor;
import com.luckyframework.jdbc.exceptions.SQLExecutorException;
import com.luckyframework.jdbc.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * 默认的SQL操作器
 * @author fk7075
 * @version 1.0
 * @date 2021/9/30 10:51 上午
 */
public class DefaultSqlOperation implements SqlOperation {

    private final static Logger logger = LoggerFactory.getLogger(DefaultSqlOperation.class);

    private final Connection connection;
    private boolean isAutoCommit = true;

    public DefaultSqlOperation(Connection connection) {
        this.connection = connection;
    }


    /**
     * 开启事务
     */
    public void openTransaction(){
        try {
            isAutoCommit = false;
            connection.setAutoCommit(false);
        }catch (SQLException e){
            throw new SQLExecutorException(e);
        }

    }

    /**
     * 设置事务的隔离级别
     * @param level 隔离级别
     */
    public void setTransactionIsolation(int level) {
        if(isAutoCommit){
            throw new SQLExecutorException("Isolation levels cannot be set in non-transactional.");
        }
        try {
            this.connection.setTransactionIsolation(level);
        }catch (Exception e){
            throw new SQLExecutorException(e);
        }

    }

    public int insertReturnGeneratedKey(String sqlTemp, Object...params){
        try(PreparedStatement ps = connection.prepareStatement(sqlTemp, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            for (Object param : params) {
                ps.setObject(i++,param);
            }
            ps.executeUpdate();
            try(ResultSet rs = ps.getGeneratedKeys()) {
                if(rs.next()){
                    return rs.getInt(1);
                }else{
                    throw new SQLExecutorException("Unable to get auto-incremental ID.");
                }
            }
        }catch (Exception e){
            throw new SQLExecutorException(e);
        }
    }

    @Override
    public int update(String sqlTemp, Object... parameters)  {
        try(PreparedStatement ps = connection.prepareStatement(sqlTemp)) {
            int i = 1;
            for (Object parameter : parameters) {
                ps.setObject(i++,parameter);
            }
            return ps.executeUpdate();
        }catch (Exception e){
            throw new SQLExecutorException(e);
        }
    }

    @Override
    public int[] updateBatch(String sqlTemp,List<Object[]> parameterList) {
        try(PreparedStatement ps = connection.prepareStatement(sqlTemp)) {
            for (Object[] parameters : parameterList) {
                int i = 1;
                for (Object parameter : parameters) {
                    ps.setObject(i++,parameter);
                }
                ps.addBatch();
            }
            return ps.executeBatch();
        } catch (SQLException e) {
            throw new SQLExecutorException(e);
        }
    }

    @Override
    public int[] updateBatch(String...completeSqls) {
        try(Statement st = connection.createStatement()) {
            for (String completeSql : completeSqls) {
                st.addBatch(completeSql);
            }
            return st.executeBatch();
        } catch (SQLException e) {
            throw new SQLExecutorException(e);
        }
    }

    @Override
    public List<Map<String, Object>> query(String sqlTemp, Object... parameters) {
        return query(sqlTemp,new RowMapperResultSetExtractor<>(new ColumnMapRowMapper()),parameters);
    }

    @Override
    public <T> T query(String sqlTemp, ResultSetExtractor<T> resultSetExtractor, Object... parameters) {
        try(PreparedStatement ps = connection.prepareStatement(sqlTemp)) {
            int i = 1;
            for (Object parameter : parameters) {
                ps.setObject(i++,parameter);
            }
            try(ResultSet rs = ps.executeQuery()) {
                return resultSetExtractor.extractData(rs);
            }
        }catch (Exception e){
            throw new SQLExecutorException(e);
        }
    }

    public void commit() {
        if(isAutoCommit){
            throw new SQLExecutorException("Manual commit is not supported in non-transaction mode.");
        }
        try {
            this.connection.commit();
        }catch (SQLException e){
            logger.error("An exception occurred during the commit operation. Procedure",e);
            throw new SQLExecutorException(e);
        }
    }

    public void rollback(){
        if(isAutoCommit){
            throw new SQLExecutorException("The rollback operation is not supported in non-transaction mode.");
        }
        try {
            this.connection.rollback();
        }catch (SQLException e){
            logger.error("An exception occurred during the rollback operation. Procedure",e);
            throw new SQLExecutorException(e);
        }
    }

    @Override
    public void close(){
        try {
            if(!connection.isClosed()){
                JdbcUtils.closeConnect(connection);
            }
        }catch (Exception e){
            throw new SQLExecutorException(e);
        }
    }
}
