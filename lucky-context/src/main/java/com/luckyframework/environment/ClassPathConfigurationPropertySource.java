package com.luckyframework.environment;

import com.luckyframework.common.Resources;
import com.luckyframework.configuration.ConfigurationUtils;

/**
 * ClassPath下面的资源文件
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/13 00:52
 */
public class ClassPathConfigurationPropertySource extends ConfigurationMapPropertySource{

    public ClassPathConfigurationPropertySource(String name) {
        super("ApplicationConfiguration[classpath: " + name + "]", ConfigurationUtils.loaderReader(name, Resources.getClassPathReader(name)));
    }



}
