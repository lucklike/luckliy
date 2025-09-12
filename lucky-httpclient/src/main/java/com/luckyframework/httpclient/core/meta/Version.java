package com.luckyframework.httpclient.core.meta;

/**
 * Http版本
 */
public enum Version {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1"),
    HTTP_2("HTTP/2"),
    NON(" ");

    private final String versionStr;

    Version(String versionStr) {
        this.versionStr = versionStr;
    }

    public String getVersionStr() {
        return versionStr;
    }
}
