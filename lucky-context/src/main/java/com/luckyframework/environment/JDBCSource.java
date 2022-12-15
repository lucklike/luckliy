package com.luckyframework.environment;


import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.StringUtils;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/12/5 14:56
 */
public class JDBCSource {

    private static final NullValue NULL_VALUE = new NullValue();


    public static final String DEFAULT_TABLE_NAME = "lucky_env";
    public static final String DEFAULT_KEY_COLUMN = "name";
    public static final String DEFAULT_VALUE_COLUMN = "value";

    private final ConfigurationMap cacheDataMap = new ConfigurationMap();
    private final Map<String, Long> cacheTimeMap = new ConcurrentHashMap<>(32);

    private final DataSource dataSource;
    private final String tableName;
    private final String keyColumn;
    private final String valueColumn;
    private final long cacheMillis;
    private final boolean lazy;

    public JDBCSource(DataSource dataSource, String tableName, String keyColumn, String valueColumn, long cacheMillis, boolean lazy) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.valueColumn = valueColumn;
        this.cacheMillis = cacheMillis;
        this.lazy = lazy;
        dataInitial();
    }

    public JDBCSource(DataSource dataSource){
        this(dataSource, DEFAULT_TABLE_NAME, DEFAULT_KEY_COLUMN, DEFAULT_VALUE_COLUMN, -1, false);
    }

    public JDBCSource(DataSource dataSource, long cacheMillis, boolean lazy){
        this(dataSource, DEFAULT_TABLE_NAME, DEFAULT_KEY_COLUMN, DEFAULT_VALUE_COLUMN, cacheMillis, lazy);
    }

    private void dataInitial() {
        if (!lazy) {
            loadAllDBData();
            initCacheMillis();
        }
    }

    private void initCacheMillis() {
        if(cacheMillis > 0){
            long expirationTime = System.currentTimeMillis() + cacheMillis;
            for (String key : cacheDataMap.keySet()) {
                cacheTimeMap.put(key, expirationTime);
            }
        }
    }

    public Object getProperty(String key){
        return doGetProperty(key);
    }

    public boolean containsKey(String key){
        return cacheDataMap.containsConfigKey(key) || keyIsExist(key);
    }

    private Object doGetProperty(String key){
        // 没有开启缓存刷新功能
        if(cacheMillis <= 0){
            if(cacheDataMap.containsConfigKey(key)){
                return cacheDataMap.getConfigProperty(key);
            }
            Object value = findProperty(key);
            if(value != null){
                cacheDataMap.addConfigProperty(key, value);
            }
            return value;
        }
        // 开启了缓存刷新功能
        else{
            long currentTimeMillis = System.currentTimeMillis();
            Long expirationTime = cacheTimeMap.get(key);
            if(expirationTime != null && currentTimeMillis <= expirationTime){
                return cacheDataMap.getConfigProperty(key);
            }
            Object value = findProperty(key);
            if(value != null){
                cacheTimeMap.put(key, currentTimeMillis + cacheMillis);
                cacheDataMap.addConfigProperty(key, value);
            }
            return value;
        }
    }



    private void updateExpirationTime(String key, long lastFetchTime) {
        this.cacheTimeMap.put(key, lastFetchTime + cacheMillis);

    }

    private void loadAllDBData(){
        this.cacheDataMap.addConfigProperties(findAllProperties());
    }

    private Map<String, Object> findAllProperties(){
        Map<String, Object> dataMap = new HashMap<>();
        String querySQL = StringUtils.format("SELECT {},{} FROM {}", keyColumn, valueColumn, tableName);
        try(Connection conn = dataSource.getConnection();
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(querySQL)){
            while(rs.next()) {
                String key = rs.getString(keyColumn);
                Object value = rs.getObject(valueColumn);
                Assert.notNull(key, "db environment keyValue is null");
                dataMap.put(key, value);
            }
            return dataMap;
        }catch (SQLException e){
            throw new PropertySourceLoaderException("An exception occurred while loading JDBC properties!", e);
        }
    }

    private Object findProperty(String key){
        String querySQL = StringUtils.format("SELECT {} FROM {} WHERE {}=?",  valueColumn, tableName, keyColumn);
        ResultSet rs = null;
        Object value = null;
        try(Connection conn = dataSource.getConnection();
            PreparedStatement statement = conn.prepareStatement(querySQL)){
            statement.setObject(1, key);
            rs = statement.executeQuery();
            while(rs.next()) {
                value = rs.getObject(valueColumn);
                break;
            }
            return value;
        }catch (SQLException e){
            throw new PropertySourceLoaderException("An exception occurred while loading JDBC properties!", e);
        }finally{
            if(rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean keyIsExist(String key) {
        String querySQL = StringUtils.format("SELECT COUNT({}) as count FROM {} WHERE {}=?", keyColumn, tableName, keyColumn);
        ResultSet rs = null;
        long count = 0L;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement statement = conn.prepareStatement(querySQL)) {
            statement.setObject(1, key);
            rs = statement.executeQuery();
            while (rs.next()) {
                count = rs.getLong("count");
                break;
            }
            return count != 0L;
        } catch (SQLException e) {
            throw new PropertySourceLoaderException("An exception occurred while loading JDBC properties!", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    final static class NullValue{

    }

}
