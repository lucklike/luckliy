package com.luckyframework.httpclient.proxy;

import com.luckyframework.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static com.luckyframework.httpclient.proxy.CommonFunctions.read;
import static com.luckyframework.httpclient.proxy.CommonFunctions.resource;

/**
 * 版本获取工具
 */
public class Version {

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    public static final String LUCKY_VERSION = getLuckyHttpClientVersion();
    public static final String JAVA_VERSION = System.getProperty("java.version");
    public static final String LUCKY_USER_AGENT = StringUtils.format("Lucky-HttpClient/{} (Java/{})", LUCKY_VERSION, JAVA_VERSION);
    private static final String LUCKY_LOGO_FILE = "classpath:lucky-httpclient.logo";

    /**
     * 获取当前lucky-httpclient版本号
     *
     * @return 当前lucky-httpclient版本号
     */
    public static String getLuckyHttpClientVersion() {
        return Optional.ofNullable(Version.class.getPackage()).map(Package::getImplementationVersion).orElse("dev");
    }

    /**
     * 打印版本信息
     */
    public static void printVersion() {
        logger.info("Lucky-HttpClient-{}", LUCKY_VERSION);
    }

    /**
     * 打印Logo
     */
    public static void printLogo() {
        try {
            String logo = StringUtils.format(read(resource(LUCKY_LOGO_FILE)), LUCKY_VERSION);
            System.out.println(logo);
        } catch (IOException e) {
            // ignore
        }
    }
}
