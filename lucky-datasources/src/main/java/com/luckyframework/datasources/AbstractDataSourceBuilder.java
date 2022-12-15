package com.luckyframework.datasources;

import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.serializable.SerializationTypeToken;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/21 0021 16:58
 */
public abstract class AbstractDataSourceBuilder implements DataSourceBuilder{

    private String PREFIX;
    private Environment ENVIRONMENT;

    @Override
    public final DataSource builder(Environment environment, String dataSourceConfigPrefix) throws Exception {
        PREFIX = dataSourceConfigPrefix;
        ENVIRONMENT = environment;
        return createDataSource();

    }

    protected <T> T getConfValue(String key,Class<T> aClass,@Nullable T defaultValue){
        T value = getConfValue(key, aClass);
        return value == null ? defaultValue : value;
    }

    @SuppressWarnings("unchecked")
    protected  <T> T getConfValue(String key,Class<T> aClass){
        String realKey = PREFIX+"."+key;
        if(!ENVIRONMENT.containsProperty(realKey)){
            return null;
        }
        return ((LuckyStandardEnvironment)ENVIRONMENT).getPropertyForType(realKey,aClass);
    }

    protected <T> T getConfValue(String key, SerializationTypeToken<T> typeToken){
        String realKey = PREFIX+"."+key;
        if(!ENVIRONMENT.containsProperty(realKey)){
            return null;
        }
        return ((LuckyStandardEnvironment)ENVIRONMENT).getPropertyForType(realKey, typeToken);
    }

    protected abstract DataSource createDataSource() throws Exception;

}
