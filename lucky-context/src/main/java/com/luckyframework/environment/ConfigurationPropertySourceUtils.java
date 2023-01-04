package com.luckyframework.environment;

import com.luckyframework.annotations.PropertySource;
import com.luckyframework.annotations.PropertySources;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    /*---------------------------------固定配置源名称字符串---------------------------------*/


    /** 由环境变量、jvm变量或命令行中指定的{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}默认加载源*/
    public static final String LUCKY_CONFIG_LOCATION_SOURCE_NAME                = "[CONFIG-LOCATION] lucky.config.location";

    /** 由{@link PropertySource @PropertySource}注解引入的文件配置源*/
    public static final String PROPERTY_SOURCE_ANNOTATION_SOURCE_NAME           = "[@PropertySource] annotation file source";


    /*---------------------------------配置文件源名称模版字符串---------------------------------*/

    /** ${user.dir}/config/文件夹下某个特定环境的所有配置源的统一名称*/
    private static final String USER_DIR_SOURCE_NAME_CONFIG_TEMP                = "{}userDir:/config/";

    /** ${user.dir}/文件夹下某个特定环境的所有配置源的统一名称*/
    private static final String USER_DIR_SOURCE_NAME_TEMP                       = "{}userDir:/";

    /** ${classpath}/config/文件夹下某个特定环境的所有配置源的统一名称*/
    private static final String CLASSPATH_SOURCE_NAME_CONFIG_TEMP               = "{}classpath:/config/";

    /** ${classpath}/文件夹下某个特定环境的所有配置源的统一名称*/
    private static final String CLASSPATH_SOURCE_NAME_TEMP                      = "{}classpath:/";


    /*---------------------------------固定文件路径字符串---------------------------------*/


    /** 路径拼接字符串 '/config/' */
    private static final String CONFIG_PATH_TEMP                                = "/config";

    /** 路径拼接字符串 '/' */
    private static final String PATH_TEMP                                       = "";


    /*---------------------------------配置文件名称模版字符串---------------------------------*/


    /** .yaml配置文件名称模板*/
    private static final String YAML_TEMP                                       = "/application{}.yaml";
    /** .yml配置文件名称模板*/
    private static final String YML_TEMP                                        = "/application{}.yml";
    /** .properties配置文件名称模板*/
    private static final String PROPERTIES_TEMP                                 = "/application{}.properties";
    /** .json配置文件名称模板*/
    private static final String JSON_TEMP                                       = "/application{}.json";


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
            if(!classPs.isEmpty()){
                mps.addLast(classPs);
            }
        }
        return new CompositePropertySource(PROPERTY_SOURCE_ANNOTATION_SOURCE_NAME, mps);
    }

    /**
     * 加载所有特定位置特定格式的配置文件源，其中包括并且优先级如下：<br/>
     * <ol>
     *     <li>{@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}所指定的源</li>
     *     <li>${user.dir}/config/    目录下所有被激活环境对应的<b>application-{profile}</b>格式的配置文件源</li>
     *     <li>${user.dir}/           目录下所有被激活环境对应的<b>application-{profile}</b>格式的配置文件源</li>
     *     <li>${classpath}/config/   目录下所有被激活环境对应的<b>application-{profile}</b>格式的配置文件源</li>
     *     <li>${classpath}/          目录下所有被激活环境对应的<b>application-{profile}</b>格式的配置文件源</li>
     * </ol>
     *
     * 其中同一目录同一环境的不同后缀的文件加载优先级为：<br/>
     * .yml > .yaml > .properties > .json
     *
     * @param activeProfiles        被激活的Profiles
     * @param configLocationValue   {@link LuckyConfigurationEnvironment#LUCKY_CONFIG_LOCATION lucky.config.location}配置对应的值
     * @return 又有固定配置文件所组成的配置源
     */
    public static List<CompositePropertySource> getConfigPropertySource(String[] activeProfiles, String configLocationValue) {
        List<CompositePropertySource> propertySources = new ArrayList<>();

        // 尝试加载用户通过 lucky.config.location 命令行参数指定的配置文件
        tryLoaderConfigLocationPropertySource(propertySources, configLocationValue);

        // 尝试加载 ${user.dir}/config、${user.dir}/ 目录下的application配置文件
        tryLoaderUserDirPropertySources(propertySources, USER_DIR_SOURCE_NAME_CONFIG_TEMP, CONFIG_PATH_TEMP, activeProfiles);
        tryLoaderUserDirPropertySources(propertySources, USER_DIR_SOURCE_NAME_TEMP, PATH_TEMP, activeProfiles);

        // 尝试加载 ${classpath}/config、${classpath}/ 目录下的application配置文件
        tryLoaderClassPathPropertySources(propertySources, CLASSPATH_SOURCE_NAME_CONFIG_TEMP, CONFIG_PATH_TEMP, activeProfiles);
        tryLoaderClassPathPropertySources(propertySources, CLASSPATH_SOURCE_NAME_TEMP, PATH_TEMP, activeProfiles);

        return propertySources;
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
                CompositePropertySource cps = new CompositePropertySource(LUCKY_CONFIG_LOCATION_SOURCE_NAME, new MutablePropertySources());
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
                if(!configMap.isEmpty() && !sourceIsExist(okmps, propertyName) && !mps.contains(propertyName)){
                    mps.addLast(new ConfigurationMapPropertySource(propertyName, configMap));
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
     * 尝试将为指定${user.dir}路径下指定激活环境对应的所有配置文件创建一个复合属性源，并将其添加到集合中
     * @param propertySources   属性源集合
     * @param sourceNameTemp    属性源名称模板字符串
     * @param configFolder      指定的路径
     * @param activeProfile     激活的环境
     */
    private static void tryLoaderUserDirPropertySources(List<CompositePropertySource> propertySources, String sourceNameTemp, String configFolder, String[] activeProfile){
        for (String profile : activeProfile) {
            String prefix = RESERVED_DEFAULT_PROFILE_NAME.equals(profile) ? "" : "[" + profile + "] ";
            String sourceName = StringUtils.format(sourceNameTemp, prefix);
            CompositePropertySource userDirSource = createUserDirCompositePropertySource(sourceName, configFolder, profile);
            if(!userDirSource.isEmpty()){
                propertySources.add(userDirSource);
            }
        }
    }

    /**
     * 尝试将为指定${classpath}路径下指定激活环境对应的所有配置文件创建一个复合属性源，并将其添加到集合中
     * @param propertySources   属性源集合
     * @param sourceNameTemp    属性源名称模板字符串
     * @param configFolder      指定的路径
     * @param activeProfile     激活的环境
     */
    private static void tryLoaderClassPathPropertySources(List<CompositePropertySource> propertySources, String sourceNameTemp, String configFolder, String[] activeProfile){
        for (String profile : activeProfile) {
            String prefix = RESERVED_DEFAULT_PROFILE_NAME.equals(profile) ? "" : "[" + profile + "] ";
            String sourceName = StringUtils.format(sourceNameTemp, prefix);
            CompositePropertySource classPathSource = createClassPathCompositePropertySource(sourceName, configFolder, profile);
            if(!classPathSource.isEmpty()){
                propertySources.add(classPathSource);
            }
        }
    }


    /**
     * 为指定${user.dir}路径下指定激活环境对应的所有配置文件创建一个复合属性源
     * @param sourceName        属性源名称
     * @param configFolder      指定的文件夹
     * @param activeProfile     激活的环境
     * @return  复合属性源
     */
    private static CompositePropertySource createUserDirCompositePropertySource(String sourceName, String configFolder, String activeProfile){
        CompositePropertySource cmps = new CompositePropertySource(sourceName, new MutablePropertySources());
        tryAddPropertySource(cmps, createUserDirPropertySource(getCompletePath(configFolder + YML_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createUserDirPropertySource(getCompletePath(configFolder + YAML_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createUserDirPropertySource(getCompletePath(configFolder + PROPERTIES_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createUserDirPropertySource(getCompletePath(configFolder + JSON_TEMP, activeProfile)));
        return cmps;
    }

    /**
     * 为指定${classpath}路径下指定激活环境对应的所有配置文件创建一个复合属性源
     * @param sourceName        属性源名称
     * @param configFolder      指定的文件夹
     * @param activeProfile     激活的环境
     * @return  复合属性源
     */
    private static CompositePropertySource createClassPathCompositePropertySource(String sourceName, String configFolder, String activeProfile){
        CompositePropertySource cmps = new CompositePropertySource(sourceName, new MutablePropertySources());
        tryAddPropertySource(cmps, createClassPathPropertySource(getCompletePath(configFolder + YML_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createClassPathPropertySource(getCompletePath(configFolder + YAML_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createClassPathPropertySource(getCompletePath(configFolder + PROPERTIES_TEMP, activeProfile)));
        tryAddPropertySource(cmps, createClassPathPropertySource(getCompletePath(configFolder + JSON_TEMP, activeProfile)));
        return cmps;
    }

    /**
     * 尝试将一个属性源加入复合属性源中,只有当前属性源中存在属性配置时才会加入
     * @param containerSource 复合数组源
     * @param specificSource  待加入的属性源
     */
    private static void tryAddPropertySource(CompositePropertySource containerSource, ConfigurationMapPropertySource specificSource){
        if(specificSource != null && !specificSource.isEmpty()){
            containerSource.addLast(specificSource);
        }
    }

    /**
     * 根据文件路径模版已经激活的环境得到一个完整的文件名
     * 例如：
     *  in:
     *      /config/application{}.yml, dev
     *  out:
     *      /config/application-dev.yml
     * @param pathTemp          文件路径模版字符串
     * @param activeProfile     被激活的环境
     * @return                  完整文件路径
     */
    private static String getCompletePath(String pathTemp, String activeProfile){
        String envSuffix = !StringUtils.hasText(activeProfile) || RESERVED_DEFAULT_PROFILE_NAME.equals(activeProfile) ? "" : "-" + activeProfile.toLowerCase();
        return StringUtils.format(pathTemp, envSuffix);
    }

    /**
     * 为${user.dir}目录下的配置文件生成一个属性源
     * @param completePath 相对${user.dir}目录的相对路径
     * @return 属性源
     */
    private static UserDirConfigurationPropertySource createUserDirPropertySource(String completePath){
        UserDirConfigurationPropertySource userDirPs = userDirPropertySourceCache.get(completePath);
        if(userDirPs == null && Resources.workingDirectoryFileExists(completePath)){
            userDirPs = new UserDirConfigurationPropertySource(completePath);
            userDirPropertySourceCache.put(completePath, userDirPs);
        }
        return userDirPs;
    }

    /**
     * 为${classpath}目录下的配置文件生成一个属性源
     * @param completePath 相对${classpath}目录的相对路径
     * @return 属性源
     */
    private static ClassPathConfigurationPropertySource createClassPathPropertySource(String completePath){
        ClassPathConfigurationPropertySource classpathPs = classpathPropertySourceCache.get(completePath);
        if(classpathPs == null && Resources.classPathFileExists(completePath)) {
            classpathPs = new ClassPathConfigurationPropertySource(completePath);
            classpathPropertySourceCache.put(completePath, classpathPs);
        }
        return classpathPs;
    }

}
