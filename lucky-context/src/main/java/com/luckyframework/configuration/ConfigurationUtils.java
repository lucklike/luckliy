package com.luckyframework.configuration;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.MapUtils;
import com.luckyframework.common.Resources;
import com.luckyframework.reflect.ClassUtils;
import com.luckyframework.serializable.JsonSerializationScheme;
import com.luckyframework.serializable.SerializationException;
import com.luckyframework.serializable.SerializationSchemeFactory;
import com.luckyframework.serializable.SerializationTypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/7/24 下午9:06
 */
public class ConfigurationUtils {
    private final static Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);
    private final static JsonSerializationScheme jsonScheme = SerializationSchemeFactory.getJsonScheme();

    private final static String SEPARATOR = File.separator;
    private final static String ROOT_CONF = SEPARATOR + "config" + SEPARATOR;
    private final static String CLASS_PATH_CONF = "/config/";
    /**
     * 当前使用的配置文件类型
     */
    public static String CONF_TYPE;
    /**
     * 当前使用的配置文件的名字
     */
    public static String CONF_FILE_NAME;

    /*-----------------------使用的配置文件的类型---------------------------*/
    public final static String NON = "NON";
    public final static String YAML = "YAML";
    public final static String YML = "YML";
    public final static String JSON = "JSON";
    public final static String PROPERTIES = "PROPERTIES";

    /*-----------------------（优先级1）命令行参数中加载、决定使用的配置----------*/
    public static final String LUCKY_CONFIG_LOCATION = "lucky.conf.location";
    public static final String LUCKY_PROFILES_ACTIVE = "lucky.profiles.active";

    /*-----------------------（优先级2）默认的配置文件 ${user.dir}/config/...-----------------*/

    public static final String USER_DIR_CONFIG_YAML = ROOT_CONF + "application.yaml";
    public static final String USER_DIR_CONFIG_YML = ROOT_CONF + "application.yml";
    public static final String USER_DIR_CONFIG_PROPERTIES = ROOT_CONF + "application.properties";
    public static final String USER_DIR_CONFIG_JSON = ROOT_CONF + "application.json";

    /*-----------------------（优先级3）默认的配置文件 ${user.dir}/...-------------------------*/

    public static final String USER_DIR_YAML = SEPARATOR + "application.yaml";
    public static final String USER_DIR_YML = SEPARATOR + "application.yml";
    public static final String USER_DIR_PROPERTIES = SEPARATOR + "application.properties";
    public static final String USER_DIR_JSON = SEPARATOR + "application.json";

    /*-----------------------（优先级4）默认的配置文件 ${user.dir}/config/...------------------*/

    public static final String CLASSPATH_CONFIG_YAML = CLASS_PATH_CONF + "application.yaml";
    public static final String CLASSPATH_CONFIG_YML = CLASS_PATH_CONF + "application.yml";
    public static final String CLASSPATH_CONFIG_PROPERTIES = CLASS_PATH_CONF + "application.properties";
    public static final String CLASSPATH_CONFIG_JSON = CLASS_PATH_CONF + "application.json";

    /*-----------------------（优先级5）默认的配置文件 ${classpath}/...-------------------------*/

    public static final String CLASSPATH_YAML = "/application.yaml";
    public static final String CLASSPATH_YML = "/application.yml";
    public static final String CLASSPATH_PROPERTIES = "/application.properties";
    public static final String CLASSPATH_JSON = "/application.json";


    public static ConfigurationMap getDefaultConfigurationMap() {
        Reader reader = getReader();
        switch (CONF_TYPE) {
            case YAML:
                return loadYaml(reader);
            case PROPERTIES:
                return loadProperties(reader);
            case JSON:
                return loadJson(reader);
            default:
                return new ConfigurationMap();
        }
    }

    public static void printConfiguration() throws IOException {
        Reader reader = getReader();
        if (reader != null) {
            StringWriter sw = new StringWriter();
            FileCopyUtils.copy(reader, sw);
            System.out.println(sw.toString());
            return;
        }
        System.out.println("没有找到默认配置文件application.yaml/application.yml/application.properties/application.json...");
    }

    public static Reader getReader() {
        String runYamlPath = System.getProperty(LUCKY_CONFIG_LOCATION);
        if (StringUtils.hasText(runYamlPath)) {
            try {
                return new BufferedReader(new InputStreamReader(new FileInputStream(runYamlPath), StandardCharsets.UTF_8));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // ${user.dir}/config/
        if (Resources.workingDirectoryFileExists(USER_DIR_CONFIG_YAML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = USER_DIR_CONFIG_YAML.substring(ROOT_CONF.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_CONFIG_YAML);
        }
        if (Resources.workingDirectoryFileExists(USER_DIR_CONFIG_YML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = USER_DIR_CONFIG_YML.substring(ROOT_CONF.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_CONFIG_YML);
        }

        if (Resources.workingDirectoryFileExists(USER_DIR_CONFIG_PROPERTIES)) {
            CONF_TYPE = PROPERTIES;
            CONF_FILE_NAME = USER_DIR_CONFIG_PROPERTIES.substring(ROOT_CONF.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_CONFIG_PROPERTIES);
        }
        if (Resources.workingDirectoryFileExists(USER_DIR_CONFIG_JSON)) {
            CONF_TYPE = JSON;
            CONF_FILE_NAME = USER_DIR_CONFIG_JSON.substring(ROOT_CONF.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_CONFIG_JSON);
        }

        // ${user.dir}/
        if (Resources.workingDirectoryFileExists(USER_DIR_YAML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = USER_DIR_YAML.substring(SEPARATOR.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_YAML);
        }
        if (Resources.workingDirectoryFileExists(USER_DIR_YML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = USER_DIR_YML.substring(SEPARATOR.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_YML);
        }
        if (Resources.workingDirectoryFileExists(USER_DIR_PROPERTIES)) {
            CONF_TYPE = PROPERTIES;
            CONF_FILE_NAME = USER_DIR_PROPERTIES.substring(SEPARATOR.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_PROPERTIES);
        }
        if (Resources.workingDirectoryFileExists(USER_DIR_JSON)) {
            CONF_TYPE = JSON;
            CONF_FILE_NAME = USER_DIR_JSON.substring(SEPARATOR.length());
            return Resources.getWorkingDirectoryReader(USER_DIR_JSON);
        }

        // ${classpath}/config/
        if (Resources.classPathFileExists(CLASSPATH_CONFIG_YAML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = CLASSPATH_CONFIG_YAML.substring(CLASS_PATH_CONF.length());
            return Resources.getClassPathReader(CLASSPATH_CONFIG_YAML);
        }
        if (Resources.classPathFileExists(CLASSPATH_CONFIG_YML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = CLASSPATH_CONFIG_YML.substring(CLASS_PATH_CONF.length());
            return Resources.getClassPathReader(CLASSPATH_CONFIG_YML);
        }
        if (Resources.classPathFileExists(CLASSPATH_CONFIG_PROPERTIES)) {
            CONF_TYPE = PROPERTIES;
            CONF_FILE_NAME = CLASSPATH_CONFIG_PROPERTIES.substring(CLASS_PATH_CONF.length());
            return Resources.getClassPathReader(CLASSPATH_CONFIG_PROPERTIES);
        }
        if (Resources.classPathFileExists(CLASSPATH_CONFIG_JSON)) {
            CONF_TYPE = JSON;
            CONF_FILE_NAME = CLASSPATH_CONFIG_JSON.substring(CLASS_PATH_CONF.length());
            return Resources.getClassPathReader(CLASSPATH_CONFIG_JSON);
        }

        // ${classpath}/
        if (Resources.classPathFileExists(CLASSPATH_YAML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = CLASSPATH_YAML.substring(1);
            return Resources.getClassPathReader(CLASSPATH_YAML);
        }
        if (Resources.classPathFileExists(CLASSPATH_YML)) {
            CONF_TYPE = YAML;
            CONF_FILE_NAME = CLASSPATH_YML.substring(1);
            return Resources.getClassPathReader(CLASSPATH_YML);
        }
        if (Resources.classPathFileExists(CLASSPATH_PROPERTIES)) {
            CONF_TYPE = PROPERTIES;
            CONF_FILE_NAME = CLASSPATH_PROPERTIES.substring(1);
            return Resources.getClassPathReader(CLASSPATH_PROPERTIES);
        }
        if (Resources.classPathFileExists(CLASSPATH_JSON)) {
            CONF_TYPE = JSON;
            CONF_FILE_NAME = CLASSPATH_JSON.substring(1);
            return Resources.getClassPathReader(CLASSPATH_JSON);
        }
        CONF_TYPE = NON;
        return null;
    }

    public static ConfigurationMap loaderReader(String name, Reader reader) {
        String exName = StringUtils.getFilenameExtension(name).toUpperCase();
        switch (exName) {
            case YAML:
            case YML:
                return loadYaml(reader);
            case PROPERTIES:
                return loadProperties(reader);
            case JSON:
                return loadJson(reader);
            default:
                throw new IllegalArgumentException("Unsupported file types:" + name);
        }
    }

    /**
     * 加载.yml/.yaml配置文件
     *
     * @param yamlReader .yml/.yaml配置文件的Reader
     * @return Map
     */
    public static ConfigurationMap loadYaml(Reader yamlReader) {
        ConfigurationMap map = new ConfigurationMap();
        Iterable<Object> iterable = new Yaml().loadAll(yamlReader);
        for (Object entryObject : iterable) {
            Map<String, Object> tempMap = (Map<String, Object>) entryObject;
            if (MapUtils.containsKey(tempMap, "lucky.profiles")) {
                Class<?> aClass = MapUtils.get(tempMap, "lucky.profiles").getClass();
                if (ClassUtils.isSimpleBaseType(aClass)) {
                    continue;
                }
            }
            map.addProperties(tempMap);
            break;
        }
        return map;
    }

    /**
     * 加载.json配置文件
     *
     * @param jsonReader .json配置文件的Reader
     * @return Map
     */
    public static ConfigurationMap loadJson(Reader jsonReader) {
        try {
            ConfigurationMap map = new ConfigurationMap();
            Map<String, Object> jsonMap = jsonScheme.deserialization(jsonReader, new SerializationTypeToken<Map<String, Object>>() {
            });
            map.addProperties(jsonMap);
            return map;
        } catch (Exception e) {
            throw new SerializationException(e);
        }

    }

    /**
     * 加载.properties配置文件
     *
     * @param propertiesReader .properties配置文件的Reader
     * @return Map
     */
    public static ConfigurationMap loadProperties(Reader propertiesReader) {
        ConfigurationMap map = new ConfigurationMap();
        Properties properties = new Properties();
        try {
            properties.load(propertiesReader);
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                map.addProperty(key, value);
            }
        } catch (IOException ignored) {
        }
        return map;
    }

}
