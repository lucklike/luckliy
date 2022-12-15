package com.luckyframework.aop;

import com.luckyframework.aop.advisor.Advisor;

import java.util.Collection;

/**
 * Advisor批量产出工厂
 */
public interface AdvisorBatchProductionPlant {

    Collection<Advisor> getAdvisors();

}
