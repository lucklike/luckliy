package com.luckyframework.jdbc.connection;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.jdbc.core.ResultSetExtractor;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于JDBC的SQL操作类
 * @author fk7075
 * @version 1.0
 * @date 2021/9/30 10:12 上午
 */
public interface SqlOperation extends Closeable {

    /**
     * 开启事务
     */
    void openTransaction();

    /**
     * 设置事务的隔离级别
     * @param level 隔离级别
     */
    void setTransactionIsolation(int level);

    /**
     * 写操作
     * @param sqlTemp    预编译的SQL
     * @param parameters SQL参数值
     * @return 执行此条SQL后受影响的行数
     */
    int update(String sqlTemp,Object...parameters);

    /**
     * 批量写操作，单一SQL模板模式
     * @param sqlTemp 预编译的SQL
     * @param parameterList 多组SQL参数值集合
     */
    int[] updateBatch(String sqlTemp,List<Object[]> parameterList);

    /**
     * 批量写操作
     * @param completeSql 完整的SQL集合
     * @return 执行每此条SQL后受影响的行数所组成的数组
     */
    int[] updateBatch(String...completeSql);

    /**
     * 查询操作,将查询结果封装为List<Map<String,Object>>类型对象
     * @param sqlTemp     预编译的SQL
     * @param parameters  SQL参数值
     * @return 查询结果
     */
    List<Map<String,Object>> query(String sqlTemp, Object...parameters);

    <T> T query(String sqlTemp, ResultSetExtractor<T> resultSetExtractor, Object...parameters);


    /**
     *
     * @param sqlTemp       预编译的SQL
     * @param requiredType  封装类型
     * @param parameters    SQL参数值
     * @param <T>           封装类型泛型
     * @return 查询结果
     */
   default <T> List<T> queryForList(String sqlTemp,Class<T> requiredType,Object...parameters){
       List<Map<String, Object>> queryResult = query(sqlTemp, parameters);
       List<T> resultList = new LinkedList<>();
       for (Map<String, Object> map : queryResult) {
           resultList.add(ConversionUtils.conversion(map,requiredType));
       }
       return resultList;
   }

   default <T> T queryForObject(String sqlTemp,Class<T> requiredType,Object...parameters){
       List<T> resultList = queryForList(sqlTemp, requiredType, parameters);
       return resultList.isEmpty() ? null : resultList.get(0);
   }

    /**
     * 插入一条数据并返回自增的主键ID
     * @param sqlTemp  预编译的SQL
     * @param params   SQL参数值
     * @return 自增ID
     */
    int insertReturnGeneratedKey(String sqlTemp, Object...params);

    /**
     * 提交
     */
    void commit();

    /**
     * 回滚
     */
    void rollback();

    @Override
    default void close(){

    }


}
