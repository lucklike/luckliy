package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 版本获取工具
 */
public class Version {

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

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

    public static void printVersion() {
        logger.info("Lucky-HttpClient-{}", LUCKY_VERSION);
    }
}
