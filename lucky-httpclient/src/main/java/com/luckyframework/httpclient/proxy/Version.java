package com.luckyframework.httpclient.proxy;

import java.util.Optional;

/**
 * 版本获取工具
 */
public class Version {

    /**
     * 获取当前lucky-httpclient版本号
     *
     * @return 当前lucky-httpclient版本号
     */
    public static String getLuckyHttpClientVersion() {
        return Optional.ofNullable(Version.class.getPackage()).map(Package::getImplementationVersion).orElse("dev");
    }
}
