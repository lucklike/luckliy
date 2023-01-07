package com.luckyframework.environment;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MutablePropertySources;

/**
 * Lucky配置文件环境
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/12 13:52
 */
public class LuckyConfigurationEnvironment extends LuckyStandardEnvironment {

    private static final Logger logger = LoggerFactory.getLogger(LuckyConfigurationEnvironment.class);

    private static final String[] DEFAULT_ACTIVE_PROFILES = { RESERVED_DEFAULT_PROFILE_NAME };

    public static final String LUCKY_CONFIG_LOCATION = "lucky.config.location";

    @Override
    protected void customizePropertySources(MutablePropertySources propertySources) {
        super.customizePropertySources(propertySources);
        loadCommandLinPropertySource(propertySources);
        String configLocationValue = getConfigLocationValue();
        setActiveProfiles(configLocationValue);
        loadRandomPropertySource(propertySources);
        loadApplicationConfigPropertySources(propertySources, configLocationValue);
    }

    /**
     * 设置命令行相关的PropertySource
     */
    private void loadCommandLinPropertySource(MutablePropertySources propertySources) {
        CommandLinePropertySource commandLineSource = CommandLinePropertySource.getInstance();
        if(!commandLineSource.isEmpty())
            propertySources.addFirst(commandLineSource);
    }


    /**
     * 加载随机数相关的PropertySource
     * @param propertySources MutablePropertySources
     */
    private void loadRandomPropertySource(MutablePropertySources propertySources) {
        propertySources.addLast(new RandomValuePropertySource());
    }

    /**
     * 加载程序配置文件{application-{profile}.yml/yaml/properties/json}相关的PropertySource
     * @param propertySources MutablePropertySources
     */
    private void loadApplicationConfigPropertySources(MutablePropertySources propertySources, String configLocationValue) {
        ConfigurationPropertySourceUtils.getConfigPropertySource(activeProfilesAddDefault(getActiveProfiles()), configLocationValue)
                .forEach(propertySources::addLast);
    }

    private String getConfigLocationValue(){
        ConfigurationMap commandLinSourceMap = CommandLinePropertySource.getInstance().getSource();
        String configLocationValue;
        if(commandLinSourceMap.containsConfigKey(LUCKY_CONFIG_LOCATION)){
            configLocationValue = (String) commandLinSourceMap.getConfigProperty(LUCKY_CONFIG_LOCATION);
        }else{
            configLocationValue = System.getProperty(LUCKY_CONFIG_LOCATION);
        }
        return configLocationValue;
    }

    /**
     * 获取完整的激活的Profiles
     * @param activeProfiles 配置的Profiles
     * @return 完整的激活的Profiles
     */
    private String[] activeProfilesAddDefault(String[] activeProfiles){
        String[] realActiveProfiles = new String[activeProfiles.length + 1];
        System.arraycopy(activeProfiles, 0, realActiveProfiles, 0, activeProfiles.length);
        realActiveProfiles[activeProfiles.length] = RESERVED_DEFAULT_PROFILE_NAME;
        return realActiveProfiles;
    }

    /**
     * 设置激活的Profiles
     */
    private void setActiveProfiles(String configLocationValue){
        LuckyStandardEnvironment tempEnv = new LuckyStandardEnvironment();
        MutablePropertySources tempPs = tempEnv.getPropertySources();
        ConfigurationPropertySourceUtils.getConfigPropertySource(DEFAULT_ACTIVE_PROFILES, configLocationValue).forEach(tempPs::addLast);
        String[] activeProfiles = tempEnv.getActiveProfiles();
        this.setActiveProfiles(activeProfiles);
        if(!ContainerUtils.isEmptyArray(activeProfiles)){
            logger.info("The current application launch environment : {}", StringUtils.arrayToString(activeProfiles));
        }
    }


}
