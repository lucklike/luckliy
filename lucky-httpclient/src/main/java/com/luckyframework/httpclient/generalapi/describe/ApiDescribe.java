package com.luckyframework.httpclient.generalapi.describe;

/**
 * API描述信息
 */
public class ApiDescribe {

    private static final ApiDescribe EMPTY = new ApiDescribe("", "", "", "", "", "");

    /**
     * 接口唯一ID
     */
    private final String id;

    /**
     * 接口名称
     */
    private final String name;

    /**
     * 接口版本号
     */
    private final String version;

    /**
     * 接口作者
     */
    private final String author;

    /**
     * 修改时间
     */
    private final String updateTime;

    /**
     * 维护人员联系方式
     */
    private final String contactWay;

    private ApiDescribe(String id, String name, String version, String author, String updateTime, String contactWay) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.updateTime = updateTime;
        this.contactWay = contactWay;
    }

    public static ApiDescribe of(Describe describe) {
        if (describe == null) {
            return EMPTY;
        }
        return new ApiDescribe(describe.id(), describe.name(), describe.version(), describe.author(), describe.updateTime(), describe.contactWay());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public String getContactWay() {
        return contactWay;
    }
}
