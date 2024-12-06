package com.luckyframework.httpclient.generalapi.describe;

/**
 * API描述信息
 */
public class ApiDescribe {

    private static final ApiDescribe EMPTY = new ApiDescribe("", "", "", "", "", "", "", "", "", true);

    /**
     * 接口唯一ID
     */
    private String id;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 接口描述信息
     */
    private String desc;

    /**
     * 接口类型
     */
    private String type;

    /**
     * 接口版本号
     */
    private String version;

    /**
     * 接口作者
     */
    private String author;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 修改时间
     */
    private String updateTime;

    /**
     * 维护人员联系方式
     */
    private String contactWay;

    /**
     * 是否为TokenApi
     */
    private boolean needToken;

    private String method;

    private String clazz;

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
            boolean needToken
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
        this.needToken = needToken;
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
                describe.needToken()
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


    public String getDesc() {
        return desc;
    }

    public String getType() {
        return type;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public void setContactWay(String contactWay) {
        this.contactWay = contactWay;
    }

    public boolean isNeedToken() {
        return needToken;
    }

    public void setNeedToken(boolean needToken) {
        this.needToken = needToken;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}
