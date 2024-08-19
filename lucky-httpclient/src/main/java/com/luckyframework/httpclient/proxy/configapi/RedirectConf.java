package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.interceptor.PriorityConstant;

public class RedirectConf {

    /**
     * 是否开启自动重定向
     */
    private Boolean enable;

    /**
     * 需要重定向的状态码，默认重定向状态码：301, 302, 303, 304, 307, 308
     */
    private Integer[] status;

    /**
     * 需要重定向的条件，此处支持SpEL表达式
     */
    private String condition;

    /**
     * 重定向地址表达式，此处支持SpEL表达式，默认值为：#{$respHeader$.Location}
     */
    private String location;

    /**
     * 最大重定向次数，默认值为：5
     */
    private Integer maxCount;

    /**
     * 重定向拦截器的优先级，默认{@value PriorityConstant#REDIRECT_PRIORITY}
     */
    private Integer priority;


    public Boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Integer[] getStatus() {
        return status;
    }

    public void setStatus(Integer[] status) {
        this.status = status;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
