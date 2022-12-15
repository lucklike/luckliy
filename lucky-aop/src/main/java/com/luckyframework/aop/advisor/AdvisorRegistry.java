package com.luckyframework.aop.advisor;

import java.util.List;

/**
 * @author fk
 * @version 1.0
 * @date 2021/4/7 0007 11:44
 */
public interface AdvisorRegistry {

    /**
     * 注册一个Advisor
     * @param advisor Advisor
     */
    void registryAdvisor(Advisor advisor);

    /**
     * 获取所有的Advisor
     * @return 所有的Advisor
     */
    List<Advisor> getAdvisors();
}
