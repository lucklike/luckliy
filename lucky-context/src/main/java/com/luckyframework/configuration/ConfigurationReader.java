package com.luckyframework.configuration;

import com.luckyframework.annotations.PropertySource;
import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.exception.LuckyIOException;
import com.luckyframework.scanner.Scanner;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部资源解析器，将外部资源解析为一个Map
 * @author fk
 * @version 1.0
 * @date 2021/3/25 0025 17:40
 */
public class ConfigurationReader {

    private static final Logger log = LoggerFactory.getLogger("c.l.configuration.ConfigurationReader");
    private static final Map<String,ConfigurationMap> cacheResourceDataMap = new ConcurrentHashMap<>(32);
    private final String[] locationPatterns;
    private final String encoding;
    private final boolean ignoreResourceNotFound;

    public ConfigurationReader(String...locationPattern){
        this(locationPattern, "UTF-8",false);
    }

    public ConfigurationReader(PropertySource propertySource){
        this(propertySource.value(), propertySource.encoding(), propertySource.ignoreResourceNotFound());
    }

    public ConfigurationReader(String locationPattern, String encoding, boolean ignoreResourceNotFound){
        this.locationPatterns = new String[]{locationPattern};
        this.encoding = encoding;
        this.ignoreResourceNotFound = ignoreResourceNotFound;
    }

    public ConfigurationReader(String[] locationPatterns, String encoding, boolean ignoreResourceNotFound){
        this.locationPatterns=locationPatterns;
        this.encoding=encoding;
        this.ignoreResourceNotFound=ignoreResourceNotFound;
    }

    public static String getResourceDesc(String locationPattern){
        return Scanner.PM.getResource(locationPattern).getDescription();
    }

    /**
     * 将资源解析为Map，支持(.json、.yaml、.properties以及网络配置)
     * @return 资源数据
     */
    public ConfigurationMap getResourceData(){
        ConfigurationMap data =new ConfigurationMap();
        Set<Resource> resourceSet = new HashSet<>();
        for (String pattern : locationPatterns) {
            try {
                Resource[] resources = Scanner.PM.getResources(pattern);
                resourceSet.addAll(Arrays.asList(resources));
            } catch (IOException e) {
                throw new LuckyIOException(e,"An exception occurred while parsing resource expression '{}'.",pattern).printException(log);
            }
        }
        loadConfigByResources(data, resourceSet);
        return data;
    }

    private void loadConfigByResources(ConfigurationMap data, Set<Resource> resources){

        for (Resource resource : resources) {
            String description = resource.getDescription();
            ConfigurationMap resourceData = cacheResourceDataMap.get(description);
            if(resourceData == null){
                try {
                    resourceData = loadResource(resource);
                    cacheResourceDataMap.put(description,resourceData);
                } catch (IOException e) {
                    if(ignoreResourceNotFound){
                        log.warn("An exception occurred while loading resource '"+description+"'");
                    }else{
                        throw new LuckyIOException(e, "An exception occurred while loading the configuration file '{}'", description).printException(log);
                    }
                }
            }
            data.mergeConfig(resourceData);
        }
    }

    public ConfigurationMap loadResource(Resource resource) throws IOException {
        // 网络资源
        if(resource instanceof UrlResource){
            try {
                Map<String, Object> urlMap = SerializationSchemeFactory.getJsonScheme().deserialization(new InputStreamReader(resource.getInputStream()), new SerializationTypeToken<Map<String, Object>>() {
                });
                return new ConfigurationMap(urlMap);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // 文件资源
        else{
            ConfigurationMap resourceMap = new ConfigurationMap();
            String path = resource.getURL().toString();
            String logMsg = "Import environment config file ["+path+"]";
            String upperCasePath = path.toUpperCase();
            BufferedReader br =new BufferedReader(new InputStreamReader(resource.getInputStream(),encoding));
            //YAML
            if(upperCasePath.endsWith(".YAML")||upperCasePath.endsWith(".YML")){
                resourceMap = ConfigurationUtils.loadYaml(br);
            }
            //PROPERTIES
            else if(upperCasePath.endsWith(".PROPERTIES")){
                resourceMap = ConfigurationUtils.loadProperties(br);
            }
            //JSON
            else if(upperCasePath.endsWith(".JSON")){
                resourceMap = ConfigurationUtils.loadJson(br);
            }
            else{
                logMsg = "Invalid import ["+path+"]";
            }
            log.info(logMsg);
            return resourceMap;
        }
    }
}
