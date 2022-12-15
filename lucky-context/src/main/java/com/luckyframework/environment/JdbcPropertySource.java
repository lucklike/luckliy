package com.luckyframework.environment;

import org.springframework.core.env.PropertySource;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/12/5 14:24
 */
public class JdbcPropertySource extends PropertySource<JDBCSource> {


    public JdbcPropertySource(String name, JDBCSource source) {
        super(name, source);
    }

    public JdbcPropertySource(String name) {
        super(name);
    }

    @Override
    public Object getProperty(String name) {
        return source.getProperty(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return source.containsKey(name);
    }
}
