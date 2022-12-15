package com.luckyframework.environment;

import com.luckyframework.common.Resources;
import com.luckyframework.configuration.ConfigurationUtils;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/13 01:15
 */
public class UserDirConfigurationPropertySource extends ConfigurationMapPropertySource{

    public UserDirConfigurationPropertySource(String name) {
        super("ApplicationConfiguration[userDir: " + name + "]", ConfigurationUtils.loaderReader(name, Resources.getWorkingDirectoryReader(name)));
    }
}
