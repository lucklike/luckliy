package com.luckyframework.datasources;


import com.luckyframework.bean.factory.BeanFactoryPostProcessor;
import com.luckyframework.bean.factory.DisposableBean;
import com.luckyframework.bean.factory.FactoryBean;
import com.luckyframework.bean.factory.VersatileBeanFactory;
import com.luckyframework.definition.BaseBeanDefinition;
import com.luckyframework.definition.BeanDefinition;
import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.exception.LuckyBeanCreateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.datasources.DataSourceBuilder.DATA_SOURCE_PREFIX;
import static com.luckyframework.datasources.DataSourceBuilder.POOL_TYPE;

/**
 * Lucky数据源管理器
 * @author fk
 * @version 1.0
 * @date 2021/4/21 0021 9:36
 */
@SuppressWarnings("unchecked")
public class LuckyDataSourceManager implements DataSourceManager,
        BeanFactoryPostProcessor, DisposableBean {

    private final static Logger log = LoggerFactory.getLogger(LuckyDataSourceManager.class);
    //利用DataSourceFactory达到懒加载的效果
    private final static Map<String,DataSourceFactory> configDataSourceFactoryMap = new ConcurrentHashMap<>();

    private static DataSourceBuilder defaultDataSourceBuilder;

    static {
        setDefaultDataSourceBuilder(new HikariCPDataSourceBuilder());
    }

    public static Map<String,DataSource> getAllDataSources() throws Exception {
        if(!configDataSourceFactoryMap.isEmpty()){
            Set<String> factoryName = configDataSourceFactoryMap.keySet();
            for (String dbname : factoryName) {
                dataSourceMap.put(dbname,configDataSourceFactoryMap.get(dbname).getDataSource());
            }
        }
        return dataSourceMap;
    }

    public static Set<String> getAllDataSourceNames(){
       return configDataSourceFactoryMap.keySet();
    }

    public static DataSource getDataSourceByName(String dbname) throws Exception {
        if(dataSourceMap.containsKey(dbname)){
            return dataSourceMap.get(dbname);
        }
        if(configDataSourceFactoryMap.containsKey(dbname)){
            DataSourceFactory dataSourceFactory = configDataSourceFactoryMap.get(dbname);
            DataSource dataSource = dataSourceFactory.getDataSource();
            dataSourceMap.put(dbname,dataSource);
            return dataSource;
        }
        return null;
    }

    public static DataSource defaultDataSource() throws Exception {
        return getDataSourceByName(DEFAULT_DBNAME);
    }

    public static DataSourceBuilder getDefaultDataSourceBuilder() {
        return defaultDataSourceBuilder;
    }

    public static void setDefaultDataSourceBuilder(DataSourceBuilder defaultDataSourceBuilder) {
        LuckyDataSourceManager.defaultDataSourceBuilder = defaultDataSourceBuilder;
        if(!dataSourceBuilderMap.containsKey(defaultDataSourceBuilder.getPoolType())){
            dataSourceBuilderMap.put(defaultDataSourceBuilder.getPoolType().toUpperCase(),defaultDataSourceBuilder);
        }
    }

    @Override
    public DataSource getDataSource(String dbname) throws Exception {
        return getDataSourceByName(dbname);
    }

    @Override
    public Collection<DataSource> getDataSources() throws Exception {
        return getAllDataSources().values();
    }

    @Override
    public void addDataSource(String dbname, DataSource dataSource) {
        Assert.notNull(dbname,"Data source registration failed! dbname is null");
        Assert.notNull(dataSource,"Data source registration failed! data source is null");
        if (containsDataSource(dbname)){
            throw new DataSourceRegistrationException("Data source registration failed! The data source named '"+dbname+"' has been registered");
        }
        dataSourceMap.put(dbname, dataSource);
    }

    public void addDataSourceFactory(String dbname, DataSourceFactory dataSourceFactory){
        Assert.notNull(dbname,"Data source registration failed! dbname is null");
        Assert.notNull(dataSourceFactory,"Data source factory registration failed! data source factory is null");
        if (containsDataSource(dbname)){
            throw new DataSourceRegistrationException("Data source registration failed! The data source named '"+dbname+"' has been registered");
        }
        configDataSourceFactoryMap.put(dbname, dataSourceFactory);
    }

    @Override
    public void removeDataSource(String dbname) {
        dataSourceMap.remove(dbname);
        configDataSourceFactoryMap.remove(dbname);
    }

    @Override
    public boolean containsDataSource(String dbname) {
        return dataSourceMap.containsKey(dbname) || configDataSourceFactoryMap.containsKey(dbname);
    }

    @Override
    public void destroy() throws Exception {
        for(Map.Entry<String,DataSourceFactory> dataSourceFactoryEntry : configDataSourceFactoryMap.entrySet()){
            DataSource dataSource = dataSourceFactoryEntry.getValue().getDataSource();
            if(dataSource instanceof Closeable){
                ((Closeable)dataSource).close();
            }
        }

    }

    @Override
    public void postProcessorBeanFactory(VersatileBeanFactory listableBeanFactory) {
        parseAndRegisterDataSources(listableBeanFactory);
    }

    private void parseAndRegisterDataSources(VersatileBeanFactory listableBeanFactory){
        //注册IOC容器中的DataSource
        Environment environment = listableBeanFactory.getEnvironment();
        String[] dbNames = listableBeanFactory.getBeanNamesForType(DataSource.class);
        for (String dbName : dbNames) {
            if(containsDataSource(dbName)) break;
            addDataSourceFactory(dbName, new SingletonDataSourceFactory() {
                @Override
                public DataSource createDataSource() {
                    return listableBeanFactory.getBean(dbName,DataSource.class);
                }

                @Override
                public Class<? extends DataSource> getDataSourceType() {
                    return (Class<? extends DataSource>) listableBeanFactory.getType(dbName);
                }
            });
        }

        if(!environment.containsProperty(DATA_SOURCE_PREFIX)){
            return;
        }
        Object dataConfig = ((LuckyStandardEnvironment)environment).getPropertyForObject(DATA_SOURCE_PREFIX);

        //注册配置文件中的DataSource
        if(!(dataConfig instanceof Map)){
            throw new DataSourceParsingException("An exception occurred while parsing the data source configuration in the default configuration file, please check the '"+DATA_SOURCE_PREFIX+"' configuration");
        }else{
            Map<String,Object> dataMap = (Map<String, Object>) dataConfig;
            for(Map.Entry<String,Object> data : dataMap.entrySet()){
                Object dataValue = data.getValue();

                //单一数据源，使用省略"dbname"方式配置
                if(!(dataValue instanceof Map)){
                    Object poolTypeObj = dataMap.get(POOL_TYPE);
                    DataSourceBuilder builder = poolTypeObj == null
                            ? defaultDataSourceBuilder
                            : DataSourceManager.getDataSourceBuilder(poolTypeObj.toString());
                    if(builder == null){
                        throw new DataSourceParsingException("An exception occurred while parsing the data source configuration in the default configuration file,There is no DataSourceBuilder of type '"+poolTypeObj+"'");
                    }
                    registerDataSource(listableBeanFactory,builder,DATA_SOURCE_PREFIX,DEFAULT_DBNAME);
                    break;
                }
                Map<String,Object> dataSourceInfo = (Map<String, Object>) dataValue;
                Object poolTypeObj = dataSourceInfo.get(POOL_TYPE);
                DataSourceBuilder builder = poolTypeObj == null
                        ? defaultDataSourceBuilder
                        : DataSourceManager.getDataSourceBuilder(poolTypeObj.toString());
                if(builder == null){
                    throw new DataSourceParsingException("An exception occurred while parsing the data source configuration in the default configuration file,There is no DataSourceBuilder of type '"+poolTypeObj+"'");
                }
                String dbname= data.getKey();
                String dataSourceConfigPrefix = DATA_SOURCE_PREFIX+"."+dbname;
                registerDataSource(listableBeanFactory,builder,dataSourceConfigPrefix,dbname);
            }
        }
    }

    private void registerDataSource(VersatileBeanFactory beanFactory,DataSourceBuilder dataSourceBuilder,String dataSourceConfigPrefix,String dbname){
        DataSourceFactory dataSourceFactory = getSingletonDataSourceFactory(dataSourceBuilder, beanFactory.getEnvironment(), dataSourceConfigPrefix);
        BeanDefinition dataSourceBeanDefinition = getDataSourceBeanDefinition(dataSourceFactory, dbname);
        beanFactory.registerBeanDefinition(dbname,dataSourceBeanDefinition);
        addDataSourceFactory(dbname,dataSourceFactory);
    }

    private BeanDefinition getDataSourceBeanDefinition(DataSourceFactory dataSourceFactory,String dbName){
        BeanDefinition definition = new BaseBeanDefinition();
        definition.setLazyInit(true);
        FactoryBean factoryBean = new FactoryBean() {
            @Override
            public Object createBean() {
                try {
                    return dataSourceFactory.getDataSource();
                }catch (Exception e){
                    log.error("The configuration of data named '"+DEFAULT_DBNAME+"' in the parsing environment is abnormal",e);
                    throw new LuckyBeanCreateException(e);
                }
            }

            @Override
            public ResolvableType getResolvableType() {
                return ResolvableType.forRawClass(dataSourceFactory.getDataSourceType());
            }
        };
        definition.setFactoryBean(factoryBean);
        if(DEFAULT_DBNAME.equals(dbName)){
            definition.setPrimary(true);
        }
        return definition;
    }

    private DataSourceFactory getSingletonDataSourceFactory(DataSourceBuilder builder, Environment environment, String dataSourceConfigPrefix){
       return  new SingletonDataSourceFactory() {
           @Override
           public Class<? extends DataSource> getDataSourceType() {
               return builder.getDataSourceType();
           }

           @Override
            public DataSource createDataSource() throws Exception {
                return builder.builder(environment,dataSourceConfigPrefix);
            }
        };
    }

    public interface DataSourceFactory{
        DataSource getDataSource() throws Exception;

        Class<? extends DataSource> getDataSourceType();

    }

    public static abstract class  SingletonDataSourceFactory implements DataSourceFactory{

        private DataSource dataSource;

        @Override
        public DataSource getDataSource() throws Exception {
            if(dataSource == null){
                dataSource = createDataSource();
            }
            return dataSource;
        }

        public abstract DataSource createDataSource() throws Exception;
    }
}
