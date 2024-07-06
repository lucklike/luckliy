package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.retry.RetryDecider;

/**
 * 重试决策抽象类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 15:13
 */
public abstract class RetryDeciderContext<T> extends RetryContext implements RetryDecider<T> {

}
