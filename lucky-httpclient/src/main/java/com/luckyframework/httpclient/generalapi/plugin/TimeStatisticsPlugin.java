package com.luckyframework.httpclient.generalapi.plugin;

import com.luckyframework.httpclient.proxy.creator.Scope;
import com.luckyframework.httpclient.proxy.plugin.ExecuteMeta;
import com.luckyframework.httpclient.proxy.plugin.ProxyDecorator;
import com.luckyframework.httpclient.proxy.plugin.ProxyPlugin;

import java.util.Date;

/**
 * 耗时统计插件
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/4/15 11:04
 */
public class TimeStatisticsPlugin implements ProxyPlugin {


    @Override
    public Object decorate(ProxyDecorator decorator) throws Throwable {
        TimeStatisticsInfo info = new TimeStatisticsInfo();
        ExecuteMeta meta = decorator.getMeta();
        info.setExecuteMeta(meta);
        try {
            info.setStart(new Date());
            Object proceed = decorator.proceed();
            info.setEnd(new Date());
            return proceed;
        } catch (Throwable e) {
            info.setEnd(new Date());
            info.setTh(e);
            throw e;
        } finally {
            TimeStatistics timeStatisticsAnn = meta.getMetaContext().getMergedAnnotationCheckParent(TimeStatistics.class);
            info.initTag(timeStatisticsAnn.warn(), timeStatisticsAnn.slow());
            TimeStatisticsHandle timeStatisticsHandle = meta.getMetaContext().generateObject(timeStatisticsAnn.handle(), Scope.SINGLETON);
            timeStatisticsHandle.handle(info);
        }
    }

    @Override
    public boolean match(ExecuteMeta meta) {
        TimeStatistics timeStatisticsAnn = meta.getMetaContext().getMergedAnnotationCheckParent(TimeStatistics.class);
        return timeStatisticsAnn != null && timeStatisticsAnn.enable();
    }
}
