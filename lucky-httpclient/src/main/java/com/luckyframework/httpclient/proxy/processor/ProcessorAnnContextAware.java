package com.luckyframework.httpclient.proxy.processor;

/**
 * 响应处理器注解上下文Aware
 *
 * @author fukang
 * @version 2.1.1
 * @date 2024/05/24 11:24
 */
public interface ProcessorAnnContextAware {


    void setProcessorAnnContext(ProcessorAnnContext processorAnnContext);
}
