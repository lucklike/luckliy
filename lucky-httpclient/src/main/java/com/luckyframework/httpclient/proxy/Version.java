package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;

import java.util.Optional;

/**
 * 版本获取工具
 */
public class Version {

    public static final String LUCKY_VERSION = getLuckyHttpClientVersion();
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String LUCKY_USER_AGENT = StringUtils.format("Lucky-HttpClient/{} (Java/{})", LUCKY_VERSION, JAVA_VERSION);

    /**
     * 获取当前lucky-httpclient版本号
     *
     * @return 当前lucky-httpclient版本号
     */
    public static String getLuckyHttpClientVersion() {
        return Optional.ofNullable(Version.class.getPackage()).map(Package::getImplementationVersion).orElse("dev");
    }
}
