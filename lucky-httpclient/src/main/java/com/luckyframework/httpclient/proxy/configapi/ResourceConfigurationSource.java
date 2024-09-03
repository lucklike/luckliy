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
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 基于{@link Resource}实现的配置源
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/5 00:21
 */
public class ResourceConfigurationSource implements ConfigurationSource {

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
            Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            Map configMap;
            fileType = fileType.trim().toLowerCase();
            switch (fileType) {
                case "properties": configMap = Resources.getConfigMapReader(reader); break;
                case "yml":
                case "yaml": configMap = Resources.fromYamlReader(reader, Map.class); break;
                case "json": configMap = Resources.fromJsonReader(reader, Map.class); break;
                default: throw new ConfigurationParserException("Unsupported onfiguration source file type: '{}'", fileType);
            }

            ConfigurationMap resultMap = new ConfigurationMap();
            if (configMap.containsKey(prefix)) {
                resultMap.addProperty(prefix, configMap.get(prefix));
            }
            return resultMap;
        } catch (IOException e) {
            throw new ConfigurationParserException(e, "An exception occurred while reading the local configuration source file. Procedure：{}", source);
        }
    }
}
