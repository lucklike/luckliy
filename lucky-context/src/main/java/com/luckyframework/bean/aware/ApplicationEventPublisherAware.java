package com.luckyframework.bean.aware;

import com.luckyframework.context.event.ApplicationEventPublisher;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/19 上午9:32
 */
public interface ApplicationEventPublisherAware extends Aware{

    void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher);
}
