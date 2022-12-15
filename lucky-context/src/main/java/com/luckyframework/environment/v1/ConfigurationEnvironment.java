package com.luckyframework.environment.v1;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.MapUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.common.TempPair;
import com.luckyframework.configuration.ConfigurationReader;
import com.luckyframework.configuration.ConfigurationUtils;
import org.springframework.lang.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.luckyframework.configuration.ConfigurationUtils.*;

/**
 * 基于配置文件的环境变量
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/25 下午10:48
 */
public class ConfigurationEnvironment extends AbstractEnvironment{

    private String ACTIVE = COMMON;
    private final StorageUnit dataUnit;

    public ConfigurationEnvironment(){
        ConfigurationMap dataMap = new ConfigurationMap();
        ConfigurationMap confMap = ConfigurationUtils.getDefaultConfigurationMap();
        ConfigurationMap importMap = getImportConfigMap(confMap);
        TempPair<String,ConfigurationMap> activeTempPair = getProfilesActiveMap(confMap);
        dataMap.mergeConfig(confMap);
        dataMap.mergeConfig(importMap);
        if(activeTempPair != null){
            ACTIVE = activeTempPair.getOne();
            dataMap.mergeConfig(activeTempPair.getTwo());
        }
        dataUnit = new ConfigurationMapStorageUnit(dataMap);
    }

    public ConfigurationEnvironment(@NonNull PropertySourceEnvironment propertyEnvironment){
        ConfigurationMap dataMap = new ConfigurationMap();
        ConfigurationMap confMap = ConfigurationUtils.getDefaultConfigurationMap();
        ConfigurationMap importMap = getImportConfigMap(confMap);
        TempPair<String,ConfigurationMap> activeTempPair = getProfilesActiveMap(confMap);
        dataMap.mergeConfig(propertyEnvironment.getOriginalMap());
        dataMap.mergeConfig(confMap);
        dataMap.mergeConfig(importMap);
        if(activeTempPair != null){
            ACTIVE = activeTempPair.getOne();
            dataMap.mergeConfig(activeTempPair.getTwo());
        }
        dataUnit = new ConfigurationMapStorageUnit(dataMap);
    }



    private static TempPair<String, ConfigurationMap> getProfilesActiveMap(ConfigurationMap confMap) {
        String profilesActive = System.getProperty(LUCKY_PROFILES_ACTIVE);
        //使用默认的环境，无需加载其他环境的配置
        if("def".equalsIgnoreCase(profilesActive)){
            return null;
        }

        if(profilesActive == null){
            try {
                Object active = confMap.getConfigProperty("lucky.profiles.active");
                if(active != null){
                    profilesActive = String.valueOf(active);
                }
            }catch (Exception ignored){}
        }

        if(profilesActive == null){
            return null;
        }

        String active = profilesActive.toUpperCase();

        ConfigurationMap profilesMap = new ConfigurationMap();
        if(CONF_TYPE.equals(YAML)){
            //加载内部profiles
            Reader reader = getReader();
            Iterable<Object> yamlSections = new Yaml().loadAll(reader);
            for (Object yaml : yamlSections) {
                Map<String, Object> currMap = (Map<String, Object>) yaml;
                try {
                    Object profiles = MapUtils.get(currMap, "lucky.profiles");
                    if ((profiles == null) || (profiles instanceof Map) || (profiles instanceof Collection) || (profiles.getClass().isArray())) {
                        continue;
                    }
                    if (String.valueOf(profiles).equalsIgnoreCase(profilesActive)) {
                        profilesMap.mergeConfig(currMap);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        //加载外部profiles
        String pro1 = "/application-" + profilesActive.toLowerCase() + ".yaml";
        String pro2 = "/application-" + profilesActive.toLowerCase() + ".yml";
        String pro3 = "/application-" + profilesActive.toLowerCase() + ".properties";
        String pro4 = "/application-" + profilesActive.toLowerCase() + ".json";
        if (Resources.classPathFileExists(pro1)) {
            MapUtils.weakFusionMap(profilesMap, loadYaml(Resources.getClassPathReader(pro1)));
        } else if (Resources.classPathFileExists(pro2)) {
            MapUtils.weakFusionMap(profilesMap, loadYaml(Resources.getClassPathReader(pro2)));
        }else if(Resources.classPathFileExists(pro3)){
            MapUtils.weakFusionMap(profilesMap, loadProperties(Resources.getClassPathReader(pro3)));
        }else if(Resources.classPathFileExists(pro4)){
            MapUtils.weakFusionMap(profilesMap, loadJson(Resources.getClassPathReader(pro4)));
        }

        //去除application.yaml和Profiles.yaml的冲突
        if(MapUtils.containsKey(confMap,"lucky.profiles.active")){
            if(MapUtils.containsKey(profilesMap,"lucky.profiles")){
                MapUtils.remove(profilesMap,"lucky.profiles");
            }
        }
        return TempPair.of(active,profilesMap);
    }

    private static ConfigurationMap getImportConfigMap(ConfigurationMap confMap) {
        ConfigurationMap outMap = new ConfigurationMap();
        String importKey = "lucky.imports";
        if(confMap.containsConfigKey(importKey)){
            Object importObj = confMap.getConfigProperty(importKey);
            if(importObj instanceof List){
                List<String> imports = (List<String>) importObj;
                for (String path : imports) {
                    outMap.mergeConfig(new ConfigurationReader(path).getResourceData());
                }
            }
            if(importObj instanceof String){
                outMap.mergeConfig(new ConfigurationReader(importObj.toString()).getResourceData());
            }
        }
        return outMap;
    }

    @Override
    public Object getProperty(String key) {
        return dataUnit.getRealValue(key);
    }

    @Override
    public Object parsSingleExpression(String single$Expression) {
        return dataUnit.parsSingleExpression(single$Expression);
    }

    @Override
    public Object parsExpression(Object $Expression) {
        return dataUnit.parsExpression($Expression);
    }

    @Override
    public void setProperty(String key, Object value) {
        dataUnit.setProperties(key, value);
    }

    @Override
    public Map<String, Object> getProperties() {
        return dataUnit.getRealMap();
    }

    @Override
    public Map<String, Object> getOriginalMap() {
        return dataUnit.getOriginalMap();
    }

    @Override
    public String getProfiles() {
        return ACTIVE;
    }

    @Override
    public boolean containsKey(String key) {
        if(getOriginalMap().containsKey(key)){
            return true;
        }
        return MapUtils.containsKey(getOriginalMap(),key);
    }
}
