package com.luckyframework.environment;

import com.luckyframework.annotations.PropertySource;
import com.luckyframework.annotations.PropertySources;
import com.luckyframework.common.CommonUtils;
import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.configuration.ConfigurationReader;
import com.luckyframework.configuration.ConfigurationUtils;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotationMetadata;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static com.luckyframework.environment.AbstractConfigurableEnvironment.RESERVED_DEFAULT_PROFILE_NAME;
import static com.luckyframework.environment.LuckyConfigurationEnvironment.LUCKY_CONFIG_LOCATION;

/**
 * PropertySource工具类，用于加载和生成PropertySource
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/12 19:26
 */
public class ConfigurationPropertySourceUtils {

    /** ${user.dir}/config/ 目录下所有.yaml类型的配置文件源的固定名称*/
    public static final String DIR_CONFIG_YAML                  = "[YAML] userDir:/config/";
    /** ${user.dir}/config/ 目录下所有.yml类型的配置文件源的固定名称*/
    public static final String DIR_CONFIG_YML                   = "[YML] userDir:/config/";
    /** ${user.dir}/config/ 目录下所有.properties类型的配置文件源的固定名称*/
    public static final String DIR_CONFIG_PROPERTIES            = "[PROPERTIES] userDir:/config/";
    /** ${user.dir}/config/ 目录下所有.json类型的配置文件源的固定名称*/
    public static final String DIR_CONFIG_JSON                  = "[JSON] userDir:/config/";



    /** ${user.dir}/ 目录下所有.yaml类型的配置文件源的固定名称*/
    public static final String DIR_YAML                         = "[YAML] userDir:/";
    /** ${user.dir}/ 目录下所有.yml类型的配置文件源的固定名称*/
    public static final String DIR_YML                          = "[YML] userDir:/";
    /** ${user.dir}/ 目录下所有.properties类型的配置文件源的固定名称*/
    public static final String DIR_PROPERTIES                   = "[PROPERTIES] userDir:/";
    /** ${user.dir}/ 目录下所有.json类型的配置文件源的固定名称*/
    public static final String DIR_JSON                         = "[JSON] userDir:/";



    /** ${classpath}/config/ 目录下所有.yaml类型的配置文件源的固定名称*/
    public static final String CLASSPATH_CONFIG_YAML            = "[YAML] classpath:/config/";
    /** ${classpath}/config/ 目录下所有.yml类型的配置文件源的固定名称*/
    public static final String CLASSPATH_CONFIG_YML             = "[YML] classpath:/config/";
    /** ${classpath}/config/ 目录下所有.properties类型的配置文件源的固定名称*/
    public static final String CLASSPATH_CONFIG_PROPERTIES      = "[PROPERTIES] classpath:/config/";
    /** ${classpath}/config/ 目录下所有.json类型的配置文件源的固定名称*/
    public static final String CLASSPATH_CONFIG_JSON            = "[JSON] classpath:/config/";



    /** ${classpath}/ 目录下所有.yaml类型的配置文件源的固定名称*/
    public static final String CLASSPATH_YAML                   = "[YAML] classpath:/";
    /** ${classpath}/ 目录下所有.yml类型的配置文件源的固定名称*/
    public static final String CLASSPATH_YML                    = "[YML] classpath:/";
    /** ${classpath}/ 目录下所有.properties类型的配置文件源的固定名称*/
    public static final String CLASSPATH_PROPERTIES             = "[PROPERTIES] classpath:/";
    /** ${classpath}/ 目录下所有.json类型的配置文件源的固定名称*/
    public static final String CLASSPATH_JSON                   = "[JSON] classpath:/";



    /** 由环境变量、jvm变量或命令行中指定的{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}默认加载源*/
    public static final String LUCKY_CONFIG_LOCATION_SOURCE      = "[CONFIG-LOCATION] lucky.config.location";
    /** 由{@link PropertySource @PropertySource}注解引入的文件配置源*/
    public static final String PROPERTY_SOURCE                  = "[@PropertySource] annotation file source";



    /** config目录.yaml配置文件路径模板*/
    private static final String CONFIG_YAML_TEMP                = "/config/application{}.yaml";
    /** config目录.yml配置文件路径模板*/
    private static final String CONFIG_YML_TEMP                 = "/config/application{}.yml";
    /** config目录.properties配置文件路径模板*/
    private static final String CONFIG_PROPERTIES_TEMP          = "/config/application{}.properties";
    /** config目录.json配置文件路径模板*/
    private static final String CONFIG_JSON_TEMP                = "/config/application{}.json";



    /** .yaml配置文件路径模板*/
    private static final String YAML_TEMP                       = "/application{}.yaml";
    /** .yml配置文件路径模板*/
    private static final String YML_TEMP                        = "/application{}.yml";
    /** .properties配置文件路径模板*/
    private static final String PROPERTIES_TEMP                 = "/application{}.properties";
    /** .json配置文件路径模板*/
    private static final String JSON_TEMP                       = "/application{}.json";


    /** ${user.dir}目录文件源的缓存*/
    private static final Map<String, UserDirConfigurationPropertySource> userDirPropertySourceCache = new HashMap<>(8);
    /** ${classpath}目录文件源的缓存*/
    private static final Map<String, ClassPathConfigurationPropertySource> classpathPropertySourceCache = new HashMap<>(8);

    /** 由{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}所指定的源 */
    private static ConfigurationMapPropertySource configLocationFileSource;

    /**
     * 加载所有由{@link PropertySource @PropertySource}注解指定的文件源
     * @param componentAnnotationMetadata 可能包含@PropertySource注解的注解元素集合
     * @return 所有由@PropertySource注解指定的文件源
     * @see #getClassPropertySource(Class, MutablePropertySources)
     */
    public static CompositePropertySource getPropertySourceAnnotationSource(List<AnnotationMetadata> componentAnnotationMetadata){

        // 过滤出所有被@PropertySource标注的注解元素并将其转化为Class
        List<? extends Class<?>> componentClassList = componentAnnotationMetadata
                .stream()
                .filter(ConfigurationPropertySourceUtils::isPropertySourceClass)
                .map(m -> ClassUtils.getClass(m.getClassName()))
                .collect(Collectors.toList());

        // 按照优先级进行排序
        AnnotationAwareOrderComparator.sort(componentClassList);
        MutablePropertySources mps = new MutablePropertySources();

        for (Class<?> componentClass : componentClassList) {
            CompositePropertySource classPs = getClassPropertySource(componentClass, mps);
            CommonUtils.trueIsRunning(!classPs.isEmpty(), () -> mps.addLast(classPs));
        }
        return new CompositePropertySource(PROPERTY_SOURCE, mps);
    }

    /**
     * 加载所有特定位置特定格式的配置文件源，其中包括并且优先级如下：<br/>
     * <p>
     * 1.{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}所指定的源<br/>
     * 2.${user.dir}/config/  目录下所有被激活环境对应的<b>application-{profile}.yml</b>格式的配置文件源<br/>
     * 3.${user.dir}/config/  目录下所有被激活环境对应的<b>application-{profile}.yaml</b>格式的配置文件源<br/>
     * 4.${user.dir}/config/  目录下所有被激活环境对应的<b>application-{profile}.properties</b>格式的配置文件源<br/>
     * 5.${user.dir}/config/  目录下所有被激活环境对应的<b>application-{profile}.json</b>格式的配置文件源<br/>
     * 6.${user.dir}/         目录下所有被激活环境对应的<b>application-{profile}.yml</b>格式的配置文件源<br/>
     * 7.${user.dir}/         目录下所有被激活环境对应的<b>application-{profile}.yaml</b>格式的配置文件源<br/>
     * 8.${user.dir}/         目录下所有被激活环境对应的<b>application-{profile}.properties</b>格式的配置文件源<br/>
     * 9.${user.dir}/         目录下所有被激活环境对应的<b>application-{profile}.json</b>格式的配置文件源<br/>
     * 10.classpath:config/   目录下所有被激活环境对应的<b>application-{profile}.yml</b>格式的配置文件源<br/>
     * 11.classpath:config/   目录下所有被激活环境对应的<b>application-{profile}.yaml</b>格式的配置文件源<br/>
     * 12.classpath:config/   目录下所有被激活环境对应的<b>application-{profile}.properties</b>格式的配置文件源<br/>
     * 13.classpath:config/   目录下所有被激活环境对应的<b>application-{profile}.json</b>格式的配置文件源<br/>
     * 14.classpath:/         目录下所有被激活环境对应的<b>application-{profile}.yml</b>格式的配置文件源<br/>
     * 15.classpath:/         目录下所有被激活环境对应的<b>application-{profile}.yaml</b>格式的配置文件源<br/>
     * 16.classpath:/         目录下所有被激活环境对应的<b>application-{profile}.properties</b>格式的配置文件源<br/>
     * 17.classpath:/         目录下所有被激活环境对应的<b>application-{profile}.json</b>格式的配置文件源<br/></p>
     *
     * @param activeProfiles        被激活的Profiles
     * @param configLocationValue   {@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}配置对应的值
     * @return 又有固定配置文件所组成的配置源
     */
    public static List<CompositePropertySource> getConfigPropertySource(String[] activeProfiles, String configLocationValue) {
        List<CompositePropertySource> psList = new ArrayList<>();

        tryLoaderConfigLocationPropertySource(psList, configLocationValue);

        tryLoaderUserDirPropertySource(psList, activeProfiles, CONFIG_YML_TEMP, DIR_CONFIG_YML);
        tryLoaderUserDirPropertySource(psList, activeProfiles, CONFIG_YAML_TEMP, DIR_CONFIG_YAML);
        tryLoaderUserDirPropertySource(psList, activeProfiles, CONFIG_PROPERTIES_TEMP, DIR_CONFIG_PROPERTIES);
        tryLoaderUserDirPropertySource(psList, activeProfiles, CONFIG_JSON_TEMP, DIR_CONFIG_JSON);

        tryLoaderUserDirPropertySource(psList, activeProfiles, YML_TEMP, DIR_YML);
        tryLoaderUserDirPropertySource(psList, activeProfiles, YAML_TEMP, DIR_YAML);
        tryLoaderUserDirPropertySource(psList, activeProfiles, PROPERTIES_TEMP, DIR_PROPERTIES);
        tryLoaderUserDirPropertySource(psList, activeProfiles, JSON_TEMP, DIR_JSON);

        tryLoaderClassPathPropertySource(psList, activeProfiles, CONFIG_YML_TEMP, CLASSPATH_CONFIG_YML);
        tryLoaderClassPathPropertySource(psList, activeProfiles, CONFIG_YAML_TEMP, CLASSPATH_CONFIG_YAML);
        tryLoaderClassPathPropertySource(psList, activeProfiles, CONFIG_PROPERTIES_TEMP, CLASSPATH_CONFIG_PROPERTIES);
        tryLoaderClassPathPropertySource(psList, activeProfiles, CONFIG_JSON_TEMP, CLASSPATH_CONFIG_JSON);

        tryLoaderClassPathPropertySource(psList, activeProfiles, YML_TEMP, CLASSPATH_YML);
        tryLoaderClassPathPropertySource(psList, activeProfiles, YAML_TEMP, CLASSPATH_YAML);
        tryLoaderClassPathPropertySource(psList, activeProfiles, PROPERTIES_TEMP, CLASSPATH_PROPERTIES);
        tryLoaderClassPathPropertySource(psList, activeProfiles, JSON_TEMP, CLASSPATH_JSON);

        return psList;
    }

    /**
     * 获取某个类级别PropertySource注解源的名称
     * @param propertySourceClass 类的Class
     * @return 源名称
     */
    public static String getClassPropertySourceAnnotationSourceName(Class<?> propertySourceClass){
        return getClassPropertySourceAnnotationSourceName(propertySourceClass.getName());
    }

    /**
     * 获取某个类级别PropertySource注解源的名称
     * @param propertySourceClassName 类的全路径
     * @return 源名称
     */
    public static String getClassPropertySourceAnnotationSourceName(String propertySourceClassName){
        return "[CLASS]: {" + propertySourceClassName + "}";
    }

    /**
     * 获取某个由{@link PropertySource @PropertySource}注解引用的文件源的名称
     * @param encoding        文件编码方式
     * @param absolutePath    文件的绝对路径
     * @return  源名称
     */
    public static String getPropertySourceAnnotationSourceName(String encoding, String absolutePath){
        return "[" + encoding + "] " + absolutePath;
    }

    /**
     * 获取某个由{@link PropertySource @PropertySource}注解引用的文件源的名称
     * @param encoding        文件编码方式
     * @param location        文件的表述符(fill:/ ,classpath:/, jarfile:/, ...)
     * @return  源名称
     */
    public static String getPropertySourceAnnotationSourceNameByLocation(String encoding, String location){
        return getPropertySourceAnnotationSourceName(encoding, ConfigurationReader.getResourceDesc(location));
    }

    /**
     * 尝试加载由{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}指定的配置文件源
     * 如果用户没有配置或者配置文件为空文件时则不会加载
     * @param psList                配置文件源集合
     * @param configLocationValue   {@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}配置对应的值
     */
    private static void tryLoaderConfigLocationPropertySource(List<CompositePropertySource> psList, String configLocationValue) {
        if(StringUtils.hasText(configLocationValue)){
            ConfigurationMapPropertySource configLocationSource = getConfigLocationFileSource(configLocationValue);
            if(!configLocationSource.getSource().isEmpty()){
                CompositePropertySource cps = new CompositePropertySource(LUCKY_CONFIG_LOCATION_SOURCE, new MutablePropertySources());
                cps.addFirst(configLocationSource);
                psList.add(cps);
            }
        }
    }

    /**
     * 加载由{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}指定的配置文件源，这个文件只会加载一次
     * @param configLocationValue {@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}配置对应的值
     * @return 配置文件源
     */
    private static ConfigurationMapPropertySource getConfigLocationFileSource(String configLocationValue){
        if(configLocationFileSource == null){
            File configLocationFile = new File(configLocationValue);
            if(!configLocationFile.exists()){
                throw new IllegalStateException("The configuration file '"+configLocationFile.getAbsolutePath()+"' specified by the '"+ LUCKY_CONFIG_LOCATION +"' parameter does not exist!");
            }
            if(!configLocationFile.isFile()){
                throw new IllegalStateException("The configuration file '"+configLocationFile.getAbsolutePath()+"' specified by the '"+ LUCKY_CONFIG_LOCATION +"' parameter is not a file");
            }
            try(BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(configLocationFile.toPath())))){
                configLocationFileSource = new ConfigurationMapPropertySource(configLocationFile.getAbsolutePath(), ConfigurationUtils.loaderReader(configLocationFile.getName(), br));
            }catch (IOException e){
                throw new PropertySourceLoaderException("An exception occurred when the configuration file "+configLocationFile.getAbsolutePath()+" corresponding to '"+ LUCKY_CONFIG_LOCATION +"' was loaded!",e);
            }
        }
        return configLocationFileSource;
    }

    /**
     * 获取某一个注解元素上由{@link PropertySource @PropertySource}注解标注引入的所有配置源
     * @param aClass    注解元素Class
     * @param okmps     已经加载的可变源集合
     * @return          类上的所有@PropertySource原集合
     * @see #getPropertySourceAnnotationSource(List)
     */
    private static CompositePropertySource getClassPropertySource(Class<?> aClass, MutablePropertySources okmps){
        // 获取类上的所有@PropertySource注解的实例集合
        List<PropertySource> propertySourceInstanceList = getPropertySourceInstanceList(aClass);
        MutablePropertySources mps = new MutablePropertySources();
        for (PropertySource ps : propertySourceInstanceList) {
            String encoding = ps.encoding();
            boolean ignored = ps.ignoreResourceNotFound();
            String[] filePaths = ps.value();
            for (String filePath : filePaths) {
                ConfigurationMap configMap = new ConfigurationReader(filePath, encoding, ignored).getResourceData();
                String propertyName = getPropertySourceAnnotationSourceNameByLocation(encoding, filePath);
                if(!configMap.isEmpty() && !sourceIsExist(okmps, propertyName)){
                    CommonUtils.trueIsRunning(!mps.contains(propertyName), () -> mps.addLast(new ConfigurationMapPropertySource(propertyName, configMap)));
                }
            }
        }
        return new CompositePropertySource(getClassPropertySourceAnnotationSourceName(aClass), mps);
    }

    /**
     * 判断某个源在可变属性源中是否已经存在
     * @param mps           可变属性源
     * @param sourceName    待判定的源名称
     * @return  是否已经存在
     * @see #getClassPropertySource(Class, MutablePropertySources)
     */
    private static boolean sourceIsExist(MutablePropertySources mps, String sourceName) {
        if(mps.size() == 0) return false;
        for (org.springframework.core.env.PropertySource<?> mp : mps) {
            CompositePropertySource cps = (CompositePropertySource) mp;
            if(cps.getSource().contains(sourceName)){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取某个类上的所有@PropertySource注解实例，支持组合注解
     * @param aClass 类Class
     * @return 该类上的所有@PropertySource注解实例所组成的集合
     * @see #getClassPropertySource(Class, MutablePropertySources)
     */
    private static List<PropertySource> getPropertySourceInstanceList(Class<?> aClass){
        List<PropertySource> propertySourceList = new ArrayList<>();
        if(AnnotationUtils.isAnnotated(aClass, PropertySource.class)){
            propertySourceList.add(AnnotationUtils.findMergedAnnotation(aClass, PropertySource.class));
        }
        if(AnnotationUtils.isAnnotated(aClass, PropertySources.class)){
            propertySourceList.addAll(Arrays.asList(AnnotationUtils.findMergedAnnotation(aClass, PropertySources.class).value()));
        }
        return propertySourceList;
    }

    /**
     * 判断一个类是否是PropertySource类
     * @param metadata 注解原数据
     * @return 是否是PropertySource类
     */
    private static boolean isPropertySourceClass(AnnotationMetadata metadata) {
        return ScannerUtils.annotationIsExist(metadata, PropertySource.class) || ScannerUtils.annotationIsExist(metadata, PropertySources.class);
    }

    /**
     *  尝试加载一个${user.dir}下的属性源到集合中，有且不为空才加载
     * @param prList            容纳属性源的集合
     * @param activeProfiles    被激活的Profiles
     * @param pathTemp          路径模板
     * @param sourceName        属性源的名称
     * @see #getConfigPropertySource(String[], String)
     */
    private static void tryLoaderUserDirPropertySource(List<CompositePropertySource> prList, String[] activeProfiles, String pathTemp, String sourceName){
        CompositePropertySource cps = createUserDirCompositePropertySource(activeProfiles, pathTemp, sourceName);
        if(!cps.isEmpty()){
            prList.add(cps);
        }
    }

    /**
     * 尝试加载一个classpath下的属性源到集合中，有且不为空才加载
     * @param prList            容纳属性源的集合
     * @param activeProfiles    被激活的Profiles
     * @param pathTemp          路径模板
     * @param sourceName        属性源的名称
     * @see #getConfigPropertySource(String[], String)
     */
    private static void tryLoaderClassPathPropertySource(List<CompositePropertySource> prList, String[] activeProfiles, String pathTemp, String sourceName){
        CompositePropertySource cps = createClassPathCompositePropertySource(activeProfiles, pathTemp, sourceName);
        if(!cps.isEmpty()){
            prList.add(cps);
        }
    }

    /**
     * 为${user.dir}下的文件创建一个属性源
     * @param activeProfiles    被激活的Profiles
     * @param pathTemp          路径模板
     * @param sourceName        属性源的名称
     * @return  属性源
     */
    private static CompositePropertySource createUserDirCompositePropertySource(String[] activeProfiles, String pathTemp, String sourceName){
        MutablePropertySources mps = new MutablePropertySources();
        for (String activeProfile : activeProfiles) {
            tryLoaderUserDirPropertySource(mps, pathTemp, activeProfile);
        }
        return new CompositePropertySource(sourceName, mps);
    }

    /**
     * 为classpath下的文件创建一个属性源
     * @param activeProfiles    被激活的Profiles
     * @param pathTemp          路径模板
     * @param sourceName        属性源的名称
     * @return  属性源
     */
    private static CompositePropertySource createClassPathCompositePropertySource(String[] activeProfiles, String pathTemp, String sourceName){
        MutablePropertySources mps = new MutablePropertySources();
        for (String activeProfile : activeProfiles) {
            tryLoaderClassPathPropertySource(mps, pathTemp, activeProfile);
        }
        return new CompositePropertySource(sourceName, mps);
    }

    private static void tryLoaderUserDirPropertySource(MutablePropertySources mps, String pathTemp, String profile){
        String envSuffix = !StringUtils.hasText(profile) || RESERVED_DEFAULT_PROFILE_NAME.equals(profile) ? "" : "-" + profile.toLowerCase();
        String filePath = StringUtils.format(pathTemp, envSuffix);
        UserDirConfigurationPropertySource userDirPs = userDirPropertySourceCache.get(filePath);
        if(userDirPs != null) {
            mps.addLast(userDirPs);
        }else if(Resources.workingDirectoryFileExists(filePath)){
            userDirPs = new UserDirConfigurationPropertySource(filePath);
            userDirPropertySourceCache.put(filePath, userDirPs);
            mps.addLast(userDirPs);
        }

    }

    private static void tryLoaderClassPathPropertySource(MutablePropertySources mps, String pathTemp, String profile){
        String envSuffix = !StringUtils.hasText(profile) || RESERVED_DEFAULT_PROFILE_NAME.equals(profile) ? "" : "-" + profile.toLowerCase();
        String filePath = StringUtils.format(pathTemp, envSuffix);
        ClassPathConfigurationPropertySource classpathPs = classpathPropertySourceCache.get(filePath);
        if(classpathPs != null) {
            mps.addLast(classpathPs);
        } else if(Resources.classPathFileExists(filePath)){
            classpathPs =  new ClassPathConfigurationPropertySource(filePath);
            classpathPropertySourceCache.put(filePath, classpathPs);
            mps.addLast(classpathPs);
        }
    }



}
