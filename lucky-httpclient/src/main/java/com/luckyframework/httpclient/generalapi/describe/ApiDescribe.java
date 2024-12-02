package com.luckyframework.httpclient.generalapi.describe;

/**
 * API描述信息
 */
public class ApiDescribe {

    private static final ApiDescribe EMPTY = new ApiDescribe("", "", "", "", "", "", "", "", "", false);

    /**
     * 接口唯一ID
     */
    private final String id;

    /**
     * 接口名称
     */
    private final String name;

    /**
     * 接口描述信息
     */
    private final String desc;

    /**
     * 接口类型
     */
    private final String type;

    /**
     * 接口版本号
     */
    private final String version;

    /**
     * 接口作者
     */
    private final String author;

    /**
     * 创建时间
     */
    private final String createTime;

    /**
     * 修改时间
     */
    private final String updateTime;

    /**
     * 维护人员联系方式
     */
    private final String contactWay;

    /**
     * 是否为TokenApi
     */
    private final boolean isTokenApi;

    private ApiDescribe(
            String id,
            String name,
            String desc,
            String type,
            String version,
            String author,
            String createTime,
            String updateTime,
            String contactWay,
            boolean isTokenApi
    ) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.version = version;
        this.author = author;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.contactWay = contactWay;
        this.isTokenApi = isTokenApi;
    }

    public static ApiDescribe of(Describe describe) {
        if (describe == null) {
            return EMPTY;
        }
        return new ApiDescribe(
                describe.id(),
                describe.name(),
                describe.desc(),
                describe.type(),
                describe.version(),
                describe.author(),
                describe.createTime(),
                describe.updateTime(),
                describe.contactWay(),
                describe.isTokenApi()
        );
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

    public boolean isTokenApi() {
        return isTokenApi;
    }

    public String getDesc() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getCreateTime() {
        return createTime;
    }
}
