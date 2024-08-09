package com.luckyframework.httpclient.core.executor;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.processor.ResponseProcessor;
import org.springframework.web.client.RestTemplate;

/**
 * 基于Spring的{@link RestTemplate}实现的HttpExecutor
 *
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/28 8:22 下午
 */
public class RestTemplateExecutor implements HttpExecutor {

    private final RestTemplate restTemplate;

    public RestTemplateExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void doExecute(Request request, ResponseProcessor processor) throws Exception {

    }


}
