package com.luckyframework.jdbc.datasource.dynamic;

/**
 * 数据源切换器
 * @author fk7075
 * @version 1.0
 * @date 2021/11/8 8:40 下午
 */
public class DynamicDataSourceContextHolder {

    private final static ThreadLocal<String> contextHolder = new ThreadLocal<>();

    /**
     * 获取上下文中的数据源，获取之后立马恢复为默认值
     * @return 数据源类型
     */
    public static String getDataSourceType(){
        String dataSourceType = contextHolder.get();
        contextHolder.set(null);
        return dataSourceType;
    }

    /**
     * 切换上下文中的数据源
     * @param dataSourceType 数据源类型
     */
    public static void setDataSourceType(String dataSourceType){
        contextHolder.set(dataSourceType);
    }

}
