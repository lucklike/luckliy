package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.Resources;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * 基于本地文件实现的配置源
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/5 00:21
 */
public class LocalFileConfigurationSource implements ConfigurationSource {

    @Override
    @SuppressWarnings("all")
    public ConfigurationMap getConfigMap(String source, String prefix) {
        if (!StringUtils.hasText(source)) {
            throw new ConfigurationParserException("The local file configuration source cannot be empty.");
        }
        String fileType = StringUtils.getFilenameExtension(source);
        if (fileType == null) {
            throw new ConfigurationParserException("Unable to resolve the problem configuration source file: '{}'", source);
        }
        Resource resource = ConversionUtils.conversion(source, Resource.class);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            ConfigurationMap configMap;
            fileType = fileType.trim().toLowerCase();
            switch (fileType) {
                case "properties": configMap = Resources.getConfigMapReader(reader); break;
                case "yml":
                case "yaml": configMap = new ConfigurationMap(Resources.fromYamlReader(reader, Map.class)); break;
                case "json": configMap = new ConfigurationMap(Resources.fromJsonReader(reader, Map.class)); break;
                case "xml": configMap = new ConfigurationMap(Resources.fromXmlReader(reader, Map.class)); break;
                default: throw new ConfigurationParserException("Unsupported onfiguration source file type: '{}'", fileType);
            }
            Properties properties = configMap.toProperties();
            Properties targetProperties = new Properties();
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                if (key.startsWith(prefix)) {
                    targetProperties.put(key, properties.get(key));
                }
            }
            return ConfigurationMap.create(targetProperties);
        } catch (IOException e) {
            throw new ConfigurationParserException(e, "An exception occurred while reading the local configuration source file. Procedure：{}", source);
        }
    }
}
