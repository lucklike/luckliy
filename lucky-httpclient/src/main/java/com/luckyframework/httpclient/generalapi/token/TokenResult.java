package com.luckyframework.httpclient.generalapi.token;


/**
 * Token
 */
public interface TokenResult {

    /**
     * 当前 Token 是否已经过期
     *
     * @return Token 是否已经过期
     */
    boolean expires();

    /**
     * 后置处理逻辑，从接口获取Token之后如果需要进行后续处理，则可以通过此方法来进行
     */
    default void postProcess() {

    }
}
